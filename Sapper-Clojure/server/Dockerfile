# Stage 1: Сборка uberjar
FROM clojure:temurin-11-lein-alpine AS builder

WORKDIR /app
COPY . /app

RUN lein uberjar

# Stage 2: Минимальный образ для запуска
FROM openjdk:11.0.11-jre-slim

WORKDIR /app

# Копируем собранный JAR-файл
COPY --from=builder /app/target/uberjar/sapper.jar /app/sapper.jar

# Порт, на котором работает сервер
EXPOSE 8080

CMD ["java", "-jar", "/app/sapper.jar"]

