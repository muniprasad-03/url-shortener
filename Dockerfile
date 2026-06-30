# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the package
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-root user for security
RUN groupadd -g 1000 appgroup && useradd -u 1000 -g appgroup -m -s /bin/bash appuser
USER appuser

# Copy the built jar from the build stage
COPY --from=build --chown=appuser:appgroup /app/target/url-shortener-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Configure JVM flags for container environments
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
