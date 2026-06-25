# Combined: Next.js (public PORT) + Spring Boot (internal BACKEND_PORT)
FROM maven:3.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app
COPY apps/api/pom.xml .
COPY apps/api/src ./src
RUN mvn -q -DskipTests package

FROM node:20-bookworm-slim AS frontend-builder
WORKDIR /app/frontend
COPY apps/web/package.json apps/web/package-lock.json ./
RUN npm ci
COPY apps/web/ .
ENV API_URL=http://127.0.0.1:8081
ENV BACKEND_PORT=8081
ENV NODE_ENV=production
RUN npm run build

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN mkdir -p /app/backend

COPY --from=backend-builder /app/target/medicall-api-*.jar /app/backend/app.jar
COPY --from=frontend-builder /app/frontend /app/frontend
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

ENV SPRING_PROFILES_ACTIVE=railway
ENV NODE_ENV=production
ENV BACKEND_PORT=8081
ENV SERVER_PORT=8081
ENV API_URL=http://127.0.0.1:8081
ENV DEV_MODE=true

EXPOSE 3000

CMD ["/app/start.sh"]
