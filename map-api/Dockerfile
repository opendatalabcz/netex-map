FROM gradle:9-jdk21 AS build
WORKDIR /app

COPY gradle gradle
COPY gradlew gradle.properties build.gradle.kts settings.gradle.kts ./
RUN ./gradlew build -x test --no-daemon || true

COPY . .
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
