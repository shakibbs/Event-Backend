# Use a supported OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Give execute permissions to mvnw
RUN chmod +x mvnw

# Download dependencies (will be cached if pom.xml and .mvn haven't changed)
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY . .

# Build the application
RUN ./mvnw clean package -DskipTests

# Copy the built jar to the working directory (adjust the jar name if needed)
RUN cp target/*.jar app.jar

# Expose the port (Render will set $PORT)
EXPOSE 10000

# Run the jar file
CMD ["java", "-jar", "app.jar"]
