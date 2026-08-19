# syntax=docker/dockerfile:1.7

# Maven e JDK existem apenas no estágio de compilação.
FROM maven:3.9.15-eclipse-temurin-26-alpine AS build
WORKDIR /workspace

# O POM separado torna mudanças de dependências explícitas no cache do build.
COPY pom.xml ./
COPY src ./src

# O cache BuildKit evita baixar novamente o repositório Maven sem armazená-lo
# na imagem final. Os testes ficam para o CI, como definido no fluxo do projeto.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean package -DskipTests

# Corretto 21.0.12 inclui as correções do CPU de julho/2026. Geramos um
# runtime modular para não carregar o JDK inteiro na imagem final.
FROM amazoncorretto:21.0.12-alpine3.24 AS jre-build
RUN apk add --no-cache binutils \
    && "$JAVA_HOME/bin/jlink" \
    --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.management,jdk.naming.dns,jdk.unsupported,jdk.zipfs \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=zip-6 \
    --output /opt/reelz-jre

# Alpine 3.24 remove os CVEs de SQLite presentes na antiga base Temurin 3.23.
FROM alpine:3.24 AS runtime
WORKDIR /app

# Aplica correções de segurança da distribuição antes de remover o root.
RUN apk upgrade --no-cache \
    && apk add --no-cache ca-certificates tzdata \
    && addgroup -S reelz \
    && adduser -S reelz -G reelz
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /opt/reelz-jre "$JAVA_HOME"
COPY --from=build --chown=reelz:reelz /workspace/target/*.jar /app/reelz.jar

USER reelz
EXPOSE 8080

# O Actuator já expõe esse endpoint publicamente para a orquestração.
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD wget -q -T 2 -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1

# MaxRAMPercentage faz a JVM respeitar melhor o limite de memória do contêiner.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/reelz.jar"]
