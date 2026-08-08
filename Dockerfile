# Step 1: Build stage using Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy the entire project root context
COPY . .

# Build the backend from its lowercase project directory.
RUN mvn -f backend/pom.xml clean package -DskipTests

# Step 2: Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/backend/target/neopedia-backend-1.0.0-SNAPSHOT.jar app.jar

# Copy runtime content and generated-page directory.
COPY --from=build /app/backend/public ./backend/public
COPY --from=build /app/content ./content

EXPOSE 7070
CMD ["java", "-jar", "app.jar"]
