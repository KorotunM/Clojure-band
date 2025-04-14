# Stage 1: Leiningen для сборки jar
FROM clojure:openjdk-11-lein AS builder

WORKDIR /app
COPY . /app

RUN lein uberjar

# Stage 2: Минимальный образ для запуска
FROM openjdk:11-jre-slim

WORKDIR /app

COPY --from=builder /app/target/sapper-0.1.0-SNAPSHOT-standalone.jar /app/sapper.jar

# Порт на котором работает сервер
EXPOSE 8080

CMD ["java", "-jar", "/app/sapper.jar"]
