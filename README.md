# Hello, Spring Valkey

This project demonstrates a reactive Spring Boot application using Valkey (Redis-compatible server) for data storage, implementing a simple API for managing "bars".

## Technology Stack

- **Spring Boot**
- **Kotlin**
- **Valkey**
- **GLIDE** (Valkey Client Library)
- **Spring WebFlux**
- **Testcontainers**

## Prerequisites

- JDK 17 or later
- Docker

## Getting Started

1. Run the tests using a Valkey Testcontainer:
    ```bash
    ./gradlew test
    ```

2. Start the REST server backed by a Valkey Testcontainer:
    ```bash
    ./gradlew bootRun
    ```

## API Endpoints

### GET /bars
- **Description**: Retrieves all bars using Server-Sent Events (SSE)
- **Response**: Stream of bars as text/event-stream
- **Example**:

```bash
curl http://localhost:8080/bars
```

### POST /bars
- **Description**: Adds a new bar to the list
- **Request Body**: Plain text string
- **Response**: 200 OK on success
- **Example**:

```bash
curl -X POST -H "Content-Type: text/plain" -d "new bar" http://localhost:8080/bars
```
