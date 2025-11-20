# Kafka Orders Pipeline

A complete end-to-end Kafka data pipeline built with Spring Boot microservices, Confluent Platform (Kafka + Schema Registry), Avro serialization, retry/DLQ handling, and Docker Compose orchestration.

## Architecture

```
+-------------+      +-----------+      +-----------------+
| curl / REST | ---> | Producer  | ---> |   Kafka Topic   |
+-------------+      |  Service  |      |    orders       |
                     +-----------+      +-----------------+
                                              |
                                              v
                                   +-----------------+
                                   | Consumer        |
                                   | Service         |
                                   +-----------------+
                                              |
                            +-----------------+-----------------+
                            | Running average + DLQ (orders_dlq)|
                            +-----------------+-----------------+
```

*Confluent Zookeeper, Kafka broker, and Schema Registry back the producer/consumer services. Avro schemas are compiled via the Maven Avro plugin.*

## Project Layout

```
root
├── docker-compose.yml
├── README.md
├── producer-service
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/{java,resources,avro}
└── consumer-service
    ├── pom.xml
    ├── Dockerfile
    └── src/main/{java,resources,avro}
```

Both services share the `com.example.avro.Order` schema, generated from `src/main/avro/order.avsc` during the Maven build.

## Prerequisites

- Docker Desktop 4.x (with Docker Compose v2)
- Java 17+ (only needed if you want to run the services locally without Docker)
- Maven 3.9+ (for local builds/tests)

## Build & Run

```bash
docker-compose up --build
```

Compose will:
- start Zookeeper (2181), Kafka (9092), Schema Registry (8081)
- build & run the producer (8082) and consumer (8083) Spring Boot services
- wire Kafka + Schema Registry endpoints into both services via env vars

### Testing the Producer REST API

Send one order:

```bash
curl -X POST http://localhost:8082/produce
```

Send a batch of orders (e.g., 50):

```bash
curl -X POST "http://localhost:8082/produce/bulk?count=50"
```

### Observing the Pipeline

- Follow producer logs: `docker compose logs -f producer-service`
- Follow consumer logs: `docker compose logs -f consumer-service`
- Kafka broker logs: `docker compose logs -f kafka`

Consumer logs show each order plus the running average:

```
consumer-service  | Received order 123 for Laptop priced at 799.12
consumer-service  | Running average after 3 orders: 612.45
```

### Retry & DLQ Flow

- The consumer randomly fails ~20% of messages via `RuntimeException("Simulated failure")`.
- `@RetryableTopic` performs 3 attempts with 1-second backoffs.
- If all retries fail, the message is published to `orders_dlq` and logged by the `@DltHandler`:

```
consumer-service  | Order 123 routed to DLQ orders_dlq after retries
```

DLQ topics are auto-created (Kafka auto-create+retry topic support). You can inspect them with standard Kafka tooling if desired.

## Local Development Notes

- Avro classes are generated automatically during `mvn compile` or `mvn package` via the Avro Maven plugin.
- To run a single service locally, override the bootstrap servers and schema registry URL via env vars or `application.yml`.
- Topic names can be changed by exporting `ORDERS_TOPIC` (defaults to `orders`). Docker Compose already sets it, but you can supply a different value when running locally.
- Example local run for the producer:

```bash
cd producer-service
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DKAFKA_BOOTSTRAP_SERVERS=localhost:9092 -DSCHEMA_REGISTRY_URL=http://localhost:8081"
```

## Troubleshooting

- **Containers fail to start**: Ensure Docker Desktop is running and ports 2181/8081/8082/8083/9092 are free.
- **Avro class not found**: Run `mvn clean package` inside each service to trigger the Avro code generation.
- **Schema Registry errors**: Verify `docker compose logs schema-registry` and ensure Kafka is healthy; the registry waits for Kafka.
- **DLQ not receiving data**: Reduce the random failure threshold or temporarily throw an exception for every message to validate the flow.
- **Slow startup on Windows**: WSL2-based Docker backends may take longer; wait for health checks to pass before sending traffic.

## Cleanup

Stop and remove all containers and networks:

```bash
docker compose down -v
```
