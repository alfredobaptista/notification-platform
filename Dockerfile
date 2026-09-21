# Stage 1 - Build

FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copia primeiro apenas os ficheiros de build para
# aproveitar a cache das dependências Maven.
COPY pom.xml .

RUN mvn dependency:go-offline -B

# Copia o código-fonte
COPY src ./src

# Compila e empacota a aplicação
RUN mvn package -DskipTests


# Stage 2 - Runtime

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Utilizador não-root
RUN addgroup -S spring \
    && adduser -S spring -G spring

# Executa a aplicação sem privilégios de root
USER spring:spring

# Copia apenas o JAR produzido pelo stage de build
COPY --from=build /app/target/*.jar app.jar

# Porta HTTP da aplicação
EXPOSE 8080

# Inicia a aplicação
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]