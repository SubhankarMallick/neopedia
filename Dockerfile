# Step 1: Build stage using Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy the shaded jar from the build stage
COPY --from=build /app/target/neopedia-backend-[0-9]*.jar app.jar

# Copy the public and Content directories so Javalin can actually find them!
COPY --from=build /app/public ./public
COPY --from=build /app/Content ./Content

EXPOSE 7070
CMD ["java", "-jar", "app.jar"]