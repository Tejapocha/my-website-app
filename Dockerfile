# 1️⃣ Use official Maven + OpenJDK image for build
FROM maven:3.9.3-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper (if you use it) and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable (if using wrapper)
RUN chmod +x mvnw

# Copy all source code
COPY src ./src

# Build Spring Boot jar (skip tests)
RUN ./mvnw clean package -DskipTests

# 2️⃣ Create smaller runtime image
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy built jar from previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (default Spring Boot port)
EXPOSE 8080

# Run the app
ENTRYPOINT ["java","-jar","app.jar"]
