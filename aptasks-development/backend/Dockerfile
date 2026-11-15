# syntax=docker/dockerfile:1

# ---------- Estágio 1: build do JAR ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Wrapper e scripts de build primeiro: essa camada só muda se as dependências mudarem
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew

# Código-fonte por último, para não invalidar o cache das dependências
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test

# ---------- Estágio 2: runtime enxuto ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /app/build/libs/aptasks-backend-*.jar app.jar

USER app

ENV TZ=America/Sao_Paulo \
    PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=America/Sao_Paulo"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
