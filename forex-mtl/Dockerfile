# Start with a base Scala image
FROM hseeberger/scala-sbt:11.0.11_1.5.5_2.13.6 as builder

# Set the working directory
WORKDIR /app

# Copy SBT build files
COPY build.sbt ./
COPY project/ ./project

# Copy the application code
COPY src/ ./src

# Run tests
RUN sbt clean test

# Build the application
RUN sbt clean assembly

# Create a minimal Docker image for running the service
FROM openjdk:11-jre-slim
WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/target/scala-2.13/forex-assembly-1.0.1.jar ./forex-proxy.jar

# Set the entrypoint
ENTRYPOINT ["java", "-Dconfig.file=/app/docker-application.conf", "-jar", "./forex-proxy.jar"]