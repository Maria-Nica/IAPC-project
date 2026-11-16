# Multi-stage build: the JAR is built inside Docker from the sources,
# so no build artifacts need to be committed to git.

# ---- Stage 1: build ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Download dependencies first, so this layer is cached until pom.xml changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Build the application
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# ---- Stage 2: run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as a non-root user
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/target/compliance-service-*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
