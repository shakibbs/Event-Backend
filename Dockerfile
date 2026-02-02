# Build stage - Use Eclipse Temurin JDK 17 and install Maven
FROM eclipse-temurin:17-jdk-jammy as builder

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy everything needed for the build
COPY pom.xml .
COPY src ./src

# List contents to verify copy worked
RUN echo "=== Listing current directory ===" && ls -la && echo "=== Listing src directory ===" && find src -type f | head -20

# Build the application with verbose output
RUN echo "=== Starting Maven build ===" && \
    mvn clean package -DskipTests -e 2>&1 | tail -150 && \
    echo "=== Build completed, checking for JAR ===" && \
    ls -lh target/*.jar || echo "ERROR: No JAR file created!" && \
    echo "=== Checking JAR contents ===" && \
    jar tf target/*.jar | grep -c "\.class$" || echo "ERROR: No classes found in JAR!"

# Runtime stage - use smaller JRE image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port (Dynamic port from environment)
EXPOSE ${PORT:-10000}

# Run the jar file - extract and run the class directly if needed
CMD ["java", "-Dserver.port=${PORT:-8083}", "-jar", "app.jar"]
