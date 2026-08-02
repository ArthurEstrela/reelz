# syntax=docker/dockerfile:1.7

# Maven e JDK existem apenas no estágio de compilação.
FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# O POM separado torna mudanças de dependências explícitas no cache do build.
COPY pom.xml ./
COPY src ./src

# O cache BuildKit evita baixar novamente o repositório Maven sem armazená-lo
# na imagem final. Os testes ficam para o CI, como definido no fluxo do projeto.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean package -DskipTests

# A imagem final contém somente o JRE e o artefato executável.
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Aplica correções de segurança da distribuição antes de remover o root.
RUN apk upgrade --no-cache \
    && addgroup -S reelz \
    && adduser -S reelz -G reelz
COPY --from=build --chown=reelz:reelz /workspace/target/*.jar /app/reelz.jar

USER reelz
EXPOSE 8080

# O Actuator já expõe esse endpoint publicamente para a orquestração.
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD wget -q -T 2 -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1

# MaxRAMPercentage faz a JVM respeitar melhor o limite de memória do contêiner.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/reelz.jar"]
