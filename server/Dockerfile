FROM gradle:8.14-jdk21 AS build


WORKDIR /app

COPY build.gradle .
COPY settings.gradle ./
COPY gradlew gradlew.bat 
COPY gradle/wrapper ./gradle/wrapper


COPY src/main/java ./src/main/java
COPY src/main/resources ./src/main/resources

RUN gradle bootJar --no-daemon



FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "app.jar" ]