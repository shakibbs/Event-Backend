# Build stage - Use Eclipse Temurin JDK 17 and install Maven
FROM eclipse-temurin:17-jdk-jammy as builder

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy everything needed for the build
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests && \
    echo "JAR created at:" && \
    ls -lh target/*.jar && \
    echo "Checking main class in JAR:" && \
    jar tf target/*.jar | grep EventManagementSystemApplication.class

# Runtime stage - use smaller JRE image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port (Dynamic port from environment)
EXPOSE ${PORT:-10000}

# Run the jar file - extract and run the class directly if needed
CMD ["java", "-Dserver.port=${PORT:-8083}", "-jar", "app.jar"]
