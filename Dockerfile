# Multi-stage build for PayFlow Lite API

# Build stage
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/payflow-api-0.0.1-SNAPSHOT.jar ./payflow-api.jar

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=UTC

# Create a non-root user
RUN groupadd -r payflow && useradd -r -g payflow payflow
USER payflow

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "payflow-api.jar"]
