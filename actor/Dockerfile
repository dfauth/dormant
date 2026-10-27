# Use a lightweight, production-ready JRE base image
FROM eclipse-temurin:25-jre-alpine

# Set the working directory inside the container
WORKDIR actor/target

# Copy the built JAR file from your target folder into the container
COPY actor/target/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
