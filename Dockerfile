# Use the official OpenJDK image as the base image
FROM openjdk:21-slim

# Set the working directory inside the container
WORKDIR /app

# Set the application's JAR file path
ARG JAR_FILE=target/*.jar

# Copy the application JAR file into the container
COPY ${JAR_FILE} /app/app.jar

# Expose port 8080
EXPOSE 8080


# Run the Spring Boot application
ENTRYPOINT ["java","-jar","/app/app.jar"]
