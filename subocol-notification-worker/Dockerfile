# Etapa de build: compila el artefacto con Maven Wrapper incluido en el proyecto.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY subocol-notification-worker/pom.xml ./pom.xml
COPY subocol-notification-worker/src ./src
RUN mvn -B -DskipTests package

# Etapa de runtime: JRE minima. El worker no expone HTTP (no hay servidor web),
# asi que su healthcheck comprueba la vida del proceso principal.
FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
