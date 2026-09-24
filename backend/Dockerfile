# syntax=docker/dockerfile:1

# ---- Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/uploads \
    && chown app:app /app/uploads
WORKDIR /app
COPY --from=build /workspace/target/school-management-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
