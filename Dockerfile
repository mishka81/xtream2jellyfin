ARG VERSION=1.0.0

FROM maven:3.9-eclipse-temurin-21 AS builder

ARG VERSION

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

ARG VERSION

WORKDIR /app

RUN mkdir -p /app/config /app/cache /app/media

COPY --from=builder /build/target/xtream2jellyfin-${VERSION}-jar-with-dependencies.jar /app/xtream2jellyfin.jar

VOLUME ["/app/config", "/app/cache", "/app/media"]

CMD ["java", "-jar", "/app/xtream2jellyfin.jar"]
