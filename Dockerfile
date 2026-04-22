FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /workspace

COPY backend/pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
COPY --from=backend-build /workspace/target/backend-*.jar app.jar

USER spring:spring

# App hört auf Port 8080
EXPOSE 8080

# Startbefehl des Containers
ENTRYPOINT ["java", "-jar", "app.jar"]
