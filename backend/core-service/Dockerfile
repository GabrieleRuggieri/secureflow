# Multi-stage build — SecureFlow Core Service
# docker build -f backend/core-service/Dockerfile -t secureflow-core:local .
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY backend/core-service/pom.xml backend/core-service/pom.xml
COPY backend/core-service/src backend/core-service/src
COPY devops/flyway/sql devops/flyway/sql

WORKDIR /build/backend/core-service
RUN mvn -q -DskipTests package && \
    cp target/core-service-*.jar /build/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN apk add --no-cache curl && \
    addgroup -S secureflow && adduser -S secureflow -G secureflow
USER secureflow
COPY --from=build /build/app.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
