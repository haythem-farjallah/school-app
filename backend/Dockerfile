# First stage: build the app with Maven
FROM maven:3.9-eclipse-temurin-17 AS build

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY env.properties .

RUN mvn clean package -DskipTests

# Second stage: run the built jar
FROM eclipse-temurin:17-jre-alpine

RUN apk update && apk add --no-cache curl

WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
