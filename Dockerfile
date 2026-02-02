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
RUN echo "=== DIAGNOSTIC: Current directory ===" && \
    ls -la && \
    echo "=== DIAGNOSTIC: Source files present ===" && \
    find src -name "*.java" | wc -l && \
    echo "=== DIAGNOSTIC: Application class location ===" && \
    find src -name "EventManagementSystemApplication.java" && \
    echo "=== DIAGNOSTIC: Starting Maven build ===" && \
    mvn -v && \
    echo "=== DIAGNOSTIC: Running mvn clean package ===" && \
    mvn clean package -DskipTests 2>&1 | tail -50 && \
    echo "=== DIAGNOSTIC: Checking target directory ===" && \
    ls -lh target/ | grep -E "^-|^d" && \
    echo "=== DIAGNOSTIC: JAR file details ===" && \
    ls -lh target/*.jar && \
    echo "=== DIAGNOSTIC: Total classes in JAR ===" && \
    jar tf target/*.jar | grep "\.class$" | wc -l && \
    echo "=== DIAGNOSTIC: ALL classes in JAR ===" && \
    jar tf target/*.jar | grep "\.class$" && \
    echo "=== DIAGNOSTIC: Checking BOOT-INF structure ===" && \
    jar tf target/*.jar | grep -E "^BOOT-INF" | head -20

# Runtime stage - use smaller JRE image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port (Dynamic port from environment)
EXPOSE ${PORT:-10000}

# Run the jar file - extract and run the class directly if needed
CMD ["java", "-Dserver.port=${PORT:-8083}", "-jar", "app.jar"]
