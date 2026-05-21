# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies to cache them in the Docker layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Define port default environment variable
ENV PORT=9999
EXPOSE 9999

# Run the jar, mapping the port dynamically
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
