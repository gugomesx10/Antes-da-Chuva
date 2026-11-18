# API Antes da Chuva (Spring Boot, Java 21).
# Build a partir da raiz do repositório:
#   docker build -f dockerfiles/app/Dockerfile -t antes-da-chuva-api .
# Em produção o jar roda no App Service Java 21; esta imagem serve para execução local e portabilidade.

# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sh ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN sh ./mvnw -B -q package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/target/*-SNAPSHOT.jar app.jar
USER app

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
