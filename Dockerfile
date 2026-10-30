# syntax=docker/dockerfile:1

FROM maven:3.9.12-eclipse-temurin-21-alpine@sha256:8b2f036477a5bc9fbeb16cfb7301c484d7fff727b1c4907301ac665526bd7a8e AS build
WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine@sha256:82cf3d9ccbe1ed46252008d51b2f2329e6b9e8a2f4394c4bebefc3ea7f4b1372
# Correções de segurança do sistema base e usuário sem privilégios.
RUN apk upgrade --no-cache \
    && addgroup -S -g 10001 orders \
    && adduser -S -u 10001 -G orders -h /app orders
WORKDIR /app

COPY --from=build --chown=orders:orders /workspace/target/order-service-*.jar /app/app.jar

USER orders
EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["sh","-c","exec java -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -jar /app/app.jar"]
