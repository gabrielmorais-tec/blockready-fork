# Estágio 1: build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Estágio 2: execução
FROM eclipse-temurin:25-jre
RUN useradd --system --create-home blockready \
    && mkdir -p /app/logs && chown -R blockready /app
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER blockready
ENV LOG_DIR=/app/logs
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
