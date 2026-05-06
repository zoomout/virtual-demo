# Version 1
You are an expert Kotlin and Spring Boot developer. I need you to generate a complete Spring Boot 4.0.6 project that demonstrates a highly concurrent hybrid architecture.

The goal is to use traditional blocking Spring Web MVC and Spring Data JPA, but power them entirely with Java 24 Virtual Threads. Inside the service layer, we will use Kotlin Coroutines on a custom Virtual Thread dispatcher to make parallel 3rd party API calls.

Please provide the implementation for the following files, adhering strictly to the constraints below.

### Tech Stack & Constraints
* **Build Tool:** Maven (`pom.xml`)
* **Language:** Kotlin 2.2.21
* **Framework:** Spring Boot 4.0.6
* **Java Version:** Java 24
* **Web Framework:** Spring Web MVC (DO NOT use Spring WebFlux).
* **Database:** PostgreSQL with Spring Data JPA and Hibernate (DO NOT use R2DBC).
* **Concurrency:** Java 24 Virtual Threads must be enabled via `application.yml` (`spring.threads.virtual.enabled=true`).
* **Coroutines:** Use `kotlinx-coroutines-core`. Create a custom `CoroutineDispatcher` backed by `Executors.newVirtualThreadPerTaskExecutor()`.
* **HTTP Client:** Use Spring's modern `RestClient` for 3rd party API calls (not WebClient or RestTemplate).

### Domain Context
We are building a simple Order Processing API.
1. A request comes in to process an Order ID.
2. We fetch the `Order` entity from the database using JPA.
3. We make two parallel HTTP calls to a Payment API and an Inventory API.
4. We await both results, update the Order status, save it via JPA, and return the response.

### Required Files to Generate

1.  `pom.xml`: Include all necessary dependencies for Spring Boot 4.0.6 (Web, Data JPA, Postgres, Kotlin Coroutines, Kotlin Jackson/Reflect). You MUST properly configure the `kotlin-maven-plugin` with `<jvmTarget>24</jvmTarget>` and the `spring-boot-maven-plugin`.
2.  `application.yml`: Enable virtual threads and configure a mock Postgres connection and JPA settings.
3.  `VirtualThreadConfig.kt`: Define the custom Virtual Thread CoroutineDispatcher as a Spring Bean or top-level variable.
4.  `Order.kt`: A JPA Entity with an `id`, `paymentId`, `itemId`, and `status`. Use Jakarta EE 11 annotations.
5.  `OrderJpaRepository.kt`: Standard Spring Data JPA repository.
6.  `ExternalClients.kt`: A component using `RestClient` to mock blocking calls to a Payment API and Inventory API.
7.  `OrderService.kt`: The hybrid service. It must be a standard blocking function (NOT `suspend`), annotated with `@Transactional`. It should use `runBlocking` with the custom Virtual Thread dispatcher to launch the external client calls concurrently using `async`.
8.  `OrderController.kt`: A standard `@RestController` mapping a POST request to the service layer.

Please output the code for each file clearly.