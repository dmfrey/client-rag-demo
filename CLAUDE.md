# Client RAG Demo

Multi-module application targeting Tanzu Platform deployment:

- **`backend/`** — Spring Boot 4.1.1 application (this is the original single-module project; all Gradle/Java content lives here now)
- **`frontend/`** — React + Vite + TypeScript application

## Tech Stack

### Backend (`backend/`)

- **Java 25** (toolchain)
- **Spring Boot 4.1.1** — runs as a regular JVM application/container image for now; GraalVM native image is a deliberately deferred goal, not current behavior (see Build below)
- **Spring Data JDBC** + **Liquibase** (PostgreSQL)
- **Spring MVC** (webmvc)
- **Observability**: Micrometer tracing (Brave bridge), Prometheus, datasource-micrometer
- **Testcontainers**: PostgreSQL

### Frontend (`frontend/`)

- **React** + **TypeScript**, scaffolded with **Vite** (`react-ts` template)
- **npm** as package manager
- **React Router** for navigation, **TanStack Query** for server state, **Tailwind CSS v4** for styling — no separate component library
- **Vitest** + **React Testing Library** for tests (`npm test` in `frontend/`)

Dev server proxies `/api` to the backend (`vite.config.ts`) rather than the frontend hardcoding a backend origin — frontend code always calls relative paths (`/api/...`). This same shape works unchanged in any deployment that puts the built frontend and the backend behind one origin; the specific deployment topology (reverse proxy, backend serving the static build, etc.) is still undecided.

## Local Development

### Backend Prerequisites

This project uses **Podman** (not Docker). Setup is platform-specific:

**macOS** (Homebrew):

```bash
brew install podman podman-compose
podman machine init
podman machine start

# Route the standard docker.sock path to the Podman machine socket (once) -
# lets Testcontainers/Docker-Java find it with no DOCKER_HOST needed
podman-mac-helper install
```

**Linux**:

```bash
# Enable Podman socket (once)
systemctl --user enable --now podman.socket

# Set DOCKER_HOST (add to ~/.bashrc)
export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock

# Install podman-compose (once)
sudo apt install podman-compose
```

If `docker.sock` isn't routed to Podman (no `podman-mac-helper`, or on Linux without the socket enabled), Testcontainers needs `DOCKER_HOST` pointed at the Podman socket explicitly. On macOS, `podman machine inspect` reports the live path under `$TMPDIR`; a stable path that survives socket-path churn across machine restarts is the symlink Podman maintains at `~/.local/share/containers/podman/machine/podman.sock`.

### Running Backend Tests

```bash
./gradlew :backend:test
```

Rootless Podman can't run Ryuk (Testcontainers' privileged cleanup-reaper container), so it's disabled for every developer via `environment 'TESTCONTAINERS_RYUK_DISABLED', 'true'` on the `test` task in `backend/build.gradle`. Testcontainers 2.x reads this only from the environment — the older `ryuk.disabled` properties-file key (`testcontainers.properties`) has no effect and was removed from this project.

`TestRestTemplate` moved out of `spring-boot-test` as part of Boot 4.1's HTTP-client module split — it now lives in `org.springframework.boot.resttestclient.TestRestTemplate` (artifact `spring-boot-resttestclient`, added as a `testImplementation` in `backend/build.gradle`), and needs `@AutoConfigureTestRestTemplate` explicitly on the test class (it's no longer auto-wired just from `@SpringBootTest(webEnvironment = RANDOM_PORT)`). See `users/adapter/in/endpoint/AuthControllerIT` for the pattern.

`TestcontainersConfiguration`'s `OllamaContainer` eagerly starts and pulls `nomic-embed-text` (see the bean method) so ingestion tests can call a real embedding model rather than mocking the vector store boundary. If that pull fails with `x509: certificate signed by unknown authority`, it's a TLS-inspecting proxy on the network (this project has hit that with the Symantec WSS Agent) re-signing HTTPS to `registry.ollama.ai` with a CA the container doesn't trust — disable whatever's doing the inspection and retry; it's not a code problem. `execInContainer`'s result is checked for a non-zero exit code deliberately — it does not throw on pull failure, so a silent ignore there masked this exact failure the first time.

`@SpringBootTest` disables Micrometer metrics export by default (a `DisableMetricsExportContextCustomizer` sets `management.defaults.metrics.export.enabled=false`), which makes `PrometheusMetricsExportAutoConfiguration`'s `@ConditionalOnEnabledMetricsExport` never match — so `/actuator/prometheus` 401s in a test even though `management.endpoints.web.exposure.include` genuinely includes it and production works fine. Add `@AutoConfigureMetrics` (`org.springframework.boot.micrometer.metrics.test.autoconfigure`) to any test that needs to exercise the real Prometheus endpoint; see `configuration/ActuatorSecurityIT`.

### Test Coverage

`./gradlew :backend:test` also generates a JaCoCo report (`jacocoTestReport` runs as a `finalizedBy` of `test`) — HTML at `backend/build/reports/jacoco/test/html/index.html`, XML at `backend/build/reports/jacoco/test/jacocoTestReport.xml`. No coverage threshold is enforced (`jacocoTestCoverageVerification` isn't wired up) — this is reporting only, not a build gate. Coverage runs high (class/line coverage both in the 90s%) mostly as a side effect of this project's testing style: every feature has a real integration test exercising its full stack against Testcontainers Postgres/Ollama rather than unit tests with mocked ports, so a passing test suite already walks through nearly every adapter and domain service.

### Running the Backend

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew :backend:bootRun
```

The `local` profile (`application-local.yaml`) enables Spring Boot Docker Compose (`spring.docker.compose.enabled: true`, pointed at `backend/src/compose.yaml` via `spring.docker.compose.file` — bootRun's working directory is `backend/`, not `backend/src/`, which isn't one of Boot's default compose-file discovery locations) with `podman-compose`. Without the profile, Docker Compose stays disabled — this default matters: merely having `spring-boot-docker-compose` on the classpath (a `developmentOnly` dependency) makes Spring Boot try to start it unconditionally at every startup, and with no compose file at the working directory it's a **hard startup failure** (`IllegalStateException: No Docker Compose file found`), not a silent no-op. `application.yaml` explicitly sets `spring.docker.compose.enabled: false` as the base default for exactly this reason (required for CI/AOT builds, and for running against a manually-provided datasource).

For a one-off manual run against an already-running Postgres and a real (non-Testcontainers) Ollama instance — e.g. the GPU-backed one, see memory — skip the `local` profile and override directly:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/client_rag_demo \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=postgres \
SPRING_AI_OLLAMA_BASE_URL=http://<ollama-host>:11434 \
./gradlew :backend:bootRun
```

### Running the Frontend

```bash
cd frontend
npm install   # first time only
npm run dev
```

### Running Frontend Tests

```bash
cd frontend
npm test          # single run
npm run test:watch
```

`vite.config.ts` sets `mockReset: true` deliberately — without it, a `vi.fn()`'s call history from one test leaks into the next within the same file, which silently breaks any assertion of the form "not called yet" (found the hard way writing `DocumentsPage.test.tsx`'s confirm-before-upload tests). Also worth knowing: `userEvent.upload()` on a file input respects that input's `accept` attribute and silently drops non-matching files rather than delivering them to the change handler — if you need to test rejection of an unsupported file type, drive it through a real `drop` event instead (see `DocumentsPage.test.tsx`), since drag-and-drop has no such restriction and is the actual bug surface.

## Backend Architecture

The backend (`backend/`) follows **Hexagonal Architecture** (Ports and Adapters). Features are the primary unit of organisation — each feature is a self-contained module under the base package `com.example.clientragdemo`.

### Package Structure

```
com.example.clientragdemo
├── configuration/                        ← cross-cutting Spring configuration
├── shared/                               ← small types multiple features are allowed to depend on
│   └── exception/                        ← NotFoundException, ConflictException — see Error Handling below
└── <feature>/                            ← e.g. notes
    ├── adapter/
    │   ├── in/
    │   │   └── endpoint/                 ← REST controllers (other types: messaging, graphql, …)
    │   └── out/
    │       └── persistence/              ← DB adapters (other types: messaging, external APIs, …)
    ├── application/
    │   ├── domain/
    │   │   ├── model/                    ← domain model (Java Records; may evolve)
    │   │   └── service/                  ← one service class per use case
    │   └── port/
    │       ├── in/                       ← input port interfaces (UseCase + inner Command record)
    │       └── out/                      ← output port interfaces
    └── configuration/                    ← feature-scoped Spring configuration
```

`shared/` is a deliberate, narrow exception to "features are self-contained" — it exists only for types that would otherwise force cross-feature imports (see Error Handling). Don't grow it into a dumping ground; a feature-specific type belongs in that feature, not here.

### Conventions

**Input Ports** (`application/port/in/`):
- One interface per use case, named `<Verb><Feature>UseCase` (e.g., `CreateNoteUseCase`)
- Single method: `execute(Command command)`
- `Command` is an inner record on the interface itself

```java
public interface CreateNoteUseCase {
    Note execute(CreateNoteCommand command);

    record CreateNoteCommand(String title, String content) {}
}
```

The verb-prefixed `Command` name (e.g., `CreateNoteCommand`) keeps commands identifiable when they cross boundaries — important if the system evolves toward event-driven messaging.

**Output Ports** (`application/port/out/`):
- One interface per operation, verb-first, ending with `Port` (e.g., `LoadNotePort`, `SaveNotePort`)

**Domain Services** (`application/domain/service/`):
- One service class per use case, implementing the corresponding interface (e.g., `CreateNoteService implements CreateNoteUseCase`)

**Domain Model** (`application/domain/model/`):
- Java Records to start; may gain behaviour as requirements evolve
- Named after the real-world concept, singular, no suffix (e.g., `Note` not `NoteModel`)

**Input Adapters** (`adapter/in/`):
- Thin — delegate all work to input port interfaces; contain no business logic
- Types so far: `endpoint` (REST via Spring MVC), `security` (Spring Security SPI implementations, e.g. a `UserDetailsService` that's "driven" by the framework rather than by HTTP — see `users/adapter/in/security/`)

**Output Adapters** (`adapter/out/`):
- Implement output port interfaces; encapsulate the output technology
- Types so far: `persistence` (Spring Data JDBC), `vectorstore` (Spring AI's `VectorStore`/pgvector — chunking, embedding, filtered delete; see `documents/adapter/out/vectorstore/`), `async` (dispatches to a background executor rather than blocking the caller — the in-process stand-in for a message queue; see `documents/adapter/out/async/`), `ai` (Spring AI's `ChatClient` — streaming, RAG grounding via advisors, non-streaming completions; see `chat/adapter/out/ai/`), `chatmemory` (reads Spring AI's `ChatMemory` bean; see `chat/adapter/out/chatmemory/`)

**Feature Configuration** (`<feature>/configuration/`):
- Feature-scoped `@Configuration` only
- `@ComponentScan` scoped to the feature's root package — picks up `@Service`, `@Repository`, etc. within this feature only
- `@EnableJdbcRepositories` scoped to the feature's persistence package — limits Spring Data JDBC repository scanning to this feature
- For anything with more than one field, or that wants a typed default/validation, prefer a `@ConfigurationProperties` record over `@Value` (`@Value` is still fine for a single simple property like `SecurityConfig`'s CORS origins list). See `chat/configuration/ChatMemoryProperties` — bind it with `@EnableConfigurationProperties` on the feature's `@Configuration` class, not `@ConfigurationPropertiesScan`, to keep the feature's Spring wiring explicit and visible in one place rather than discovered by scanning.

**Root Configuration** (`com.example.clientragdemo.configuration/`):
- Cross-cutting concerns only (security, observability config, etc.)

**Error Handling**:
- One global `@RestControllerAdvice` (`configuration/GlobalExceptionHandler`) maps exceptions to HTTP status for every controller — no per-controller `@ExceptionHandler` methods.
- A feature's "not found" exception extends `shared.exception.NotFoundException` (→ 404); a "conflict" exception (e.g. a uniqueness violation) extends `shared.exception.ConflictException` (→ 409). This is *why* `shared/` is allowed to exist: the global handler only needs to know about these two base types, never about a specific feature's exception class, so a new feature never requires touching it.
- Plain `IllegalArgumentException` → 400 (input validation in a domain service) and Spring Security's `AuthenticationException` → 401 are handled generically there too.

### Adding a New Feature

1. Create the package tree under `com.example.clientragdemo.<feature>`
2. Define domain model records in `application/domain/model/`
3. Define input port interfaces (with inner `Command` records) in `application/port/in/`
4. Define output port interfaces in `application/port/out/`
5. Implement one service per use case in `application/domain/service/`
6. Implement input adapter(s) in `adapter/in/endpoint/` (thin — call the port)
7. Implement output adapter(s) in `adapter/out/persistence/` (implement the port)
8. Wire everything in `<feature>/configuration/`
9. Add Liquibase changeset(s) in `backend/src/main/resources/db/changelog/`

## Database Migrations

Liquibase changelogs live in `backend/src/main/resources/db/changelog/`. The master changelog is `db.changelog-master.yaml`. Add new changesets as separate files and include them from the master.

Several dependencies own tables that need a schema but (correctly) don't manage it themselves against a real Postgres — check before hand-writing DDL:
- If the owning module ships real per-vendor `.sql` files (e.g. `spring-ai-model-chat-memory-repository-jdbc`'s `schema-postgresql.sql`), reference the file directly from the changelog with a `sqlFile` change pointing at its classpath resource path inside the dependency jar (`db/changelog/chat/db.changelog-chat-001.yaml` is the example) — don't copy its contents in.
- If it doesn't (e.g. `spring-ai-pgvector-store`'s `PgVectorStore` builds its DDL as Java string templates, not a shipped file), there's nothing to reference; the DDL has to be reconstructed by hand and verified against the library's actual runtime behavior (`db/changelog/documents/db.changelog-documents-002.yaml` is the example — verified by decompiling `PgVectorStore.class`, then confirming a real insert/query/delete against it works end-to-end in `DocumentControllerIT`, not just by starting successfully).
- Either way, set that dependency's own `initialize-schema` property to disabled (`false` / `never`, whichever it uses) so it doesn't also try to create the table itself.

## Build

### Container Image (CI)

The CI workflow builds a container image via Cloud Native Buildpacks:

```bash
./gradlew :backend:bootBuildImage
```

This is a regular JVM-based image today, not a GraalVM native-image binary — `build.gradle` doesn't apply `org.graalvm.buildtools.native`, and no `BP_NATIVE_IMAGE` buildpack environment variable is set. True native image is an explicit future goal (this is a demo app; not worth the build-time/complexity cost until there's a real deployment target driving it), tracked here rather than silently left inconsistent with the tech stack description above.

Registry credentials are passed as Gradle properties (`-PregistryUrl`, `-PregistryUsername`, `-PregistryPassword`).

### GraalVM native image (deferred — anticipated issues)

Not started. From prior experience with this stack, expect at least these two to need explicit handling when this work begins (not yet investigated in *this* project — don't treat as confirmed findings):
- **Liquibase**: needs AOT/reflection hints for changelog parsing and JDBC driver classes to run correctly under `native-image`.
- **Micrometer observability** (tracing/Prometheus): instrumentation that relies on runtime bytecode generation or reflection typically needs native-image hints too.

### CI/CD


## Deployment

