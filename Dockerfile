# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/order-service-*.jar /app/app.jar

EXPOSE 8080

ENV PORT=8080

ENTRYPOINT ["sh","-c","exec java -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -jar /app/app.jar"]
