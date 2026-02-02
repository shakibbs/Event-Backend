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
RUN echo "=== Source structure ===" && \
    find src -type f -name "*.java" | head -5 && \
    echo "Total Java files: $(find src -type f -name "*.java" | wc -l)"

# Build
RUN mvn clean package -DskipTests || exit 1

# Show results
RUN echo "=== JAR Contents ===" && \
    jar tf target/*.jar | grep "EventManagementSystemApplication" || \
    (echo "ERROR: Main class NOT found in JAR!" && \
     echo "Listing first 20 application classes:" && \
     jar tf target/*.jar | grep "\.class$" | head -20 && exit 1)

# Runtime stage - use smaller JRE image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port (Dynamic port from environment)
EXPOSE ${PORT:-10000}

# Run the jar file - extract and run the class directly if needed
CMD ["java", "-Dserver.port=${PORT:-8083}", "-jar", "app.jar"]
