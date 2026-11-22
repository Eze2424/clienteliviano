FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline -DskipTests || true

COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /app/uploads/recepciones

EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
