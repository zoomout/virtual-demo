# Virtual Demo Project

This project demonstrates a Spring Boot application using Kotlin and Virtual Threads, including a comprehensive
performance testing suite.

## Modules

- **app**: The main Spring Boot application.
- **performance-test**: Gatling-based performance tests with managed infrastructure.

---

## Performance Testing

The `performance-test` module manages external dependencies (Postgres, Wiremock) proxied through **Toxiproxy** to
simulate network conditions.

### Manual Steps

If you prefer running steps manually:

### 1. Start Infrastructure

Start Docker containers and configure Toxiproxy:

```bash
./mvnw exec:exec@infra-up exec:exec@toxiproxy-setup -pl performance-test
```

### 2. Start Application

Run the app in the `perf` profile (connects to proxied services):

```bash
./mvnw clean compile -pl app
./mvnw spring-boot:run -pl app -Dspring-boot.run.profiles=perf
```

### 3. Run Gatling Tests

Execute the performance simulations:

```bash
./mvnw gatling:test -pl performance-test
```

### 4. Teardown

Stop and remove infrastructure:

```bash
./mvnw exec:exec@infra-down -pl performance-test
```

---

## Advanced Performance Testing

For detailed information on simulated infrastructure, Wiremock mappings, and how to use Toxiproxy to inject latency or
failures, see the [Performance Test README](performance-test/README.md).

### Quick Toxiproxy Example

Add 500ms latency to the Postgres connection:

```bash
curl -X POST http://localhost:8474/proxies/postgres/toxics \
     -H "Content-Type: application/json" \
     -d '{
           "name": "latency",
           "type": "latency",
           "attributes": { "latency": 500 }
         }'
```
