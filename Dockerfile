# Build stage - Use Eclipse Temurin JDK 17 and install Maven
FROM eclipse-temurin:17-jdk-jammy as builder

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage - Use Eclipse Temurin JRE 17 (smaller image)
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port (Render will set PORT environment variable)
EXPOSE 8080

# Run the application with proper environment variable handling
# Use shell form to support environment variable expansion
CMD java \
    -Dserver.port=${PORT:-8080} \
    -Dspring.datasource.url=${DATABASE_URL} \
    -Dspring.datasource.username=${DATABASE_USERNAME} \
    -Dspring.datasource.password=${DATABASE_PASSWORD} \
    -Dapp.jwt.secret=${JWT_SECRET} \
    -jar app.jar
