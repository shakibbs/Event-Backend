# Build stage - Use Eclipse Temurin JDK 17 and install Maven
FROM eclipse-temurin:17-jdk-jammy as builder

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

FROM eclipse-temurin:17-jdk-jammy as builder
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Show what we have
RUN echo "=== Checking src structure ===" && \
    ls -la src/ && \
    echo "" && \
    echo "=== Checking src/main structure ===" && \
    ls -la src/main/ && \
    echo "" && \
    echo "=== Checking src/main/java structure ===" && \
    ls -la src/main/java/ && \
    echo "" && \
    echo "=== Checking for EventManagementSystemApplication.java ===" && \
    find src -name "EventManagementSystemApplication.java" && \
    echo "Total Java files: $(find src -type f -name "*.java" | wc -l)"

# Build
RUN echo "=== Running Maven build ===" && \
    mvn clean package -DskipTests || (echo "Maven build failed!"; exit 1)

# Show results
RUN echo "=== JAR Analysis ===" && \
    echo "Total .class files in JAR: $(jar tf target/*.jar | grep "\.class$" | wc -l)" && \
    echo "Classes in BOOT-INF/classes/: $(jar tf target/*.jar | grep "^BOOT-INF/classes/.*\.class$" | wc -l)" && \
    echo "Application classes (com.event_management): $(jar tf target/*.jar | grep "BOOT-INF/classes/com/event_management" | wc -l)"

# Runtime stage - use smaller JRE image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port (Dynamic port from environment)
EXPOSE ${PORT:-10000}

# Run the jar file - extract and run the class directly if needed
CMD ["java", "-Dserver.port=${PORT:-8083}", "-jar", "app.jar"]
