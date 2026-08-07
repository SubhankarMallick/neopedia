# Step 1: Build stage using Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy everything from the root context safely
COPY . .

# Build the application (assuming pom.xml is in a subfolder or root)
RUN mvn clean package -DskipTests

# Step 2: Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/neopedia-backend-[0-9]*.jar app.jar
EXPOSE 7070
CMD ["java", "-jar", "app.jar"]