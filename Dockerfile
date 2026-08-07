# Step 1: Build stage using Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy the entire project root context
COPY . .

# Build using the pom.xml located inside the Backend Code directory
RUN mvn -f Backend Code/pom.xml clean package -DskipTests

# Step 2: Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy the built jar from the Backend Code target directory
COPY --from=build /app/Backend Code/target/neopedia-backend-[0-9]*.jar app.jar

# Copy content and public directories from the root level
COPY --from=build /app/public ./public
COPY --from=build /app/content ./content

EXPOSE 7070
CMD ["java", "-jar", "app.jar"]