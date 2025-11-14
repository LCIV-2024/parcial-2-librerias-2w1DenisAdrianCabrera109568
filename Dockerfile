
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY src src


RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Comando para correr la app
# Usamos el path /data/ para el volumen H2 de Docker
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.datasource.url=jdbc:h2:file:/data/libreria_db"]