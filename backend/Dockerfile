# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# 8080 is the local-dev default; on Render the app actually binds to
# whichever port Render injects via the PORT env var (see application.yml).
# EXPOSE is documentation only and doesn't restrict which port is used.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
