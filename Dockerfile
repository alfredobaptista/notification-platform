FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Utilizador não-root
RUN addgroup -S spring \
    && adduser -S spring -G spring

USER spring:spring

# Copia apenas o JAR da aplicação
COPY --from=build /app/target/*.jar app.jar

# Porta da API
EXPOSE 8080

# Inicia a aplicação
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]