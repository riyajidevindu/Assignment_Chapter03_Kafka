# syntax=docker/dockerfile:1
FROM cgr.dev/chainguard/maven:latest AS builder
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM cgr.dev/chainguard/jre:latest
WORKDIR /app
COPY --from=builder /workspace/target/consumer-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
