# Multi-stage build — SecureFlow Gateway Service
# docker build -f backend/gateway-service/Dockerfile -t secureflow-gateway:local .
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY backend/gateway-service/pom.xml backend/gateway-service/pom.xml
COPY backend/gateway-service/src backend/gateway-service/src

WORKDIR /build/backend/gateway-service
RUN mvn -q -DskipTests package && \
    cp target/gateway-service-*.jar /build/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN apk add --no-cache curl && \
    addgroup -S secureflow && adduser -S secureflow -G secureflow
USER secureflow
COPY --from=build /build/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
