# ── Étape 1 : compilation avec Maven ──
FROM maven:3.9-eclipse-temurin-19 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ── Étape 2 : image finale légère, juste le JRE + le jar compilé ──
FROM eclipse-temurin:19-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-Xmx400m", "-Xss512k", "-jar", "app.jar"]