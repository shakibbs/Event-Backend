# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY .mvn .mvn
COPY mvnw pom.xml ./

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
