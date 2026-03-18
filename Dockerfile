FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app/backend

COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=backend-builder /app/backend/target/*.jar app.jar

EXPOSE 8001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
