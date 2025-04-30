# ==================================================
# Stage 1: Build the application using Gradle
# ==================================================
FROM gradle:7.6.1-jdk17 AS builder
WORKDIR /app

# Copy only necessary build configuration files first
# This leverages Docker cache more effectively
COPY build.gradle settings.gradle ./
COPY gradlew ./
COPY gradle ./gradle

# Download dependencies (can be skipped if build command handles it)
# You can uncomment the next line if you want to explicitly download dependencies first
# RUN ./gradlew dependencies --info

# Copy the source code
COPY src ./src

# Make gradlew executable (just in case)
RUN chmod +x ./gradlew

# Build the executable JAR, skipping tests. Use --no-daemon in container builds.
RUN ./gradlew bootJar -x test --no-daemon

# ==================================================
# Stage 2: Create the final runtime image
# ==================================================
FROM openjdk:17-slim
WORKDIR /app

# Create a non-root user and group for security
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Copy the built JAR file from the builder stage
# Make sure the JAR filename matches what your build produces!
COPY --from=builder /app/build/libs/log-server-0.0.1-SNAPSHOT.jar app.jar

# Change ownership of the application JAR to the non-root user
RUN chown appuser:appgroup app.jar

# Switch to the non-root user
USER appuser

# Set the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]