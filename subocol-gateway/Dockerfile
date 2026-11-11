# Etapa de build: compila el artefacto con Maven.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml ./pom.xml
COPY src ./src
RUN mvn -B -DskipTests package

# Etapa de runtime: JRE minima + curl para el healthcheck del compose.
FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
