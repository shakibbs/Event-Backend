# Build stage - Use Eclipse Temurin JDK 17 and install Maven
FROM eclipse-temurin:17-jdk-jammy as builder

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy entire source first
COPY . .

# Build the application - explicitly set main class for Spring Boot
RUN mvn clean package -DskipTests -Dspring-boot.repackage.main-class=com.event_management_system.EventManagementSystemApplication

# Runtime stage - use smaller JRE image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port (Dynamic port from environment)
EXPOSE ${PORT:-10000}

# Run the jar file with dynamic PORT from environment
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8083} -jar app.jar"]
