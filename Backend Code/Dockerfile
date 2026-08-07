# Step 1: Build stage using Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy the backend code folder into the container workspace
COPY "Backend Code/" ./

# Build the application
RUN mvn clean package -DskipTests

# Step 2: Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 7070
CMD ["java", "-jar", "app.jar"]