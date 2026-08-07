# Step 1: Build stage using Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy everything from the root context safely
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Step 2: Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Use the exact filename to avoid wildcard matching disasters
COPY --from=build /app/target/neopedia-backend-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 7070
CMD ["java", "-jar", "app.jar"]