# Build stage
FROM gradle:8.5-jdk21 AS build

WORKDIR /app

COPY gradlew gradlew.bat settings.gradle* build.gradle* ./
COPY gradle gradle
COPY src src

RUN chmod +x gradlew

RUN ./gradlew clean bootJar --no-daemon

# Runtime stage
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
