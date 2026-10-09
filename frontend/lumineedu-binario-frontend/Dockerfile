FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S lumineedu \
    && adduser -S -G lumineedu lumineedu

COPY --from=builder \
  /workspace/target/lumineedu-binario-frontend-0.0.1-SNAPSHOT.jar \
  /app/app.jar

RUN chown lumineedu:lumineedu /app/app.jar

USER lumineedu

ENV SERVER_PORT=8081

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
