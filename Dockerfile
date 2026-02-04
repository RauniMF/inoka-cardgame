# ==================================
# Stage 1: Build Angular Frontend
# ==================================
FROM node:20-alpine AS frontend-build

WORKDIR /frontend

# Copy package files and install dependencies (including dev dependencies for build)
COPY inoka-front/package*.json ./
RUN npm ci

# Copy frontend source and build
COPY inoka-front/ ./
RUN npm run build -- --configuration production

# Output: dist/inoka-front/ contains compiled Angular app

# ==================================
# Stage 2: Build Spring Boot Backend
# ==================================
FROM eclipse-temurin:21-jdk AS backend-build

WORKDIR /backend

# Copy the entire inoka-app directory
COPY inoka-app/ ./

# Fix Windows line endings (CRLF -> LF) and make gradlew executable
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy built frontend into Spring Boot static resources
# Angular builds to dist/inoka-front/browser, we want the contents in static/
COPY --from=frontend-build /frontend/dist/inoka-front/browser ./src/main/resources/static

# Build the Spring Boot JAR
RUN ./gradlew bootJar --no-daemon

# Output: build/libs/inoka_app-*.jar

# ==================================
# Stage 3: Runtime Image
# ==================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR from backend-build stage
COPY --from=backend-build /backend/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8080

# Set JVM options for container environment
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]