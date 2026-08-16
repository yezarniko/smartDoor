FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S smartdoor && adduser -S smartdoor -G smartdoor
COPY --from=build /app/target/smartdoor-backend-0.1.0.jar app.jar
USER smartdoor
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
