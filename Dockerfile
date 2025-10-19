# Use official OpenJDK image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy everything
COPY . .

# Build the app
RUN ./mvnw clean package -DskipTests

# Run the app (adjust JAR name below)
CMD ["java", "-jar", "target/website-0.0.1-SNAPSHOT.jar"]
