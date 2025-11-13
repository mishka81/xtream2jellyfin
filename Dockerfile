FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /app/config /app/cache /app/media

COPY --from=builder /build/target/xtream2jellyfin-1.0.0-jar-with-dependencies.jar /app/xtream2jellyfin.jar

ENV EXTRACT_ONLY=false
ENV RUN_ONCE=false
ENV WRITE_METADATA_JSON=false
ENV FILE_MANAGER_TYPE=simple

VOLUME ["/app/config", "/app/cache", "/app/media"]

CMD ["java", "-jar", "/app/xtream2jellyfin.jar"]
