# Shared multi-stage Dockerfile for every microservice (Maven multi-module).
# The module to build is passed from docker-compose.yml:  args: MODULE: retrieval-pre
# The JAR is built inside Docker from the sources, so no build artifacts are committed to git.

# ---- Stage 1: build ----
FROM eclipse-temurin:21-jdk-alpine AS build
ARG MODULE
WORKDIR /build

COPY . .
RUN chmod +x mvnw && ./mvnw -B -q -pl ${MODULE} -am package -DskipTests

# ---- Stage 2: run ----
FROM eclipse-temurin:21-jre-alpine
ARG MODULE
WORKDIR /app

# Run as a non-root user
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/${MODULE}/target/${MODULE}-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
