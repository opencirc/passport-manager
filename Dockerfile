# Build

FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

# Use the official OpenJDK image as the base image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the application JAR file into the container
COPY --from=builder /app/target/*.jar /app/app.jar

# Expose port 8080
EXPOSE 8080


# Run the Spring Boot application
ENTRYPOINT ["java","-jar","/app/app.jar"]
