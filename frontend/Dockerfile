# syntax=docker/dockerfile:1.7

# As dependências atuais de build/teste declaram Node >= 22.
FROM node:22-alpine AS build
WORKDIR /app

# npm ci garante uma instalação reprodutível a partir do package-lock.json.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vazio por padrão: o browser usa a mesma origem e o Nginx encaminha /api.
ARG VITE_API_URL=""
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

FROM nginx:1.29-alpine AS runtime

# Incorpora os patches de segurança publicados após a imagem-base.
RUN apk upgrade --no-cache

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/reelz-security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -T 2 -O /dev/null http://127.0.0.1/ || exit 1
CMD ["nginx", "-g", "daemon off;"]
