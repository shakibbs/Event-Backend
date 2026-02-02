# Use Maven image with Java 17
FROM maven:3.9-eclipse-temurin-17-jammy as builder

# Set the working directory
WORKDIR /app

# Copy pom.xml
COPY pom.xml .

# Download dependencies (will be cached if pom.xml hasn't changed)
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage - use smaller JRE image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port (Dynamic port from environment)
EXPOSE ${PORT:-10000}

# Run the jar file with dynamic PORT from environment
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8083} -jar app.jar"]
