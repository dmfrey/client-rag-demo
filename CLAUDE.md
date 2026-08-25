# Client RAG Demo

Multi-module application targeting Tanzu Platform deployment:

- **`backend/`** — Spring Boot 4.1.1 application (this is the original single-module project; all Gradle/Java content lives here now)
- **`frontend/`** — React + Vite + TypeScript application
- **`ingestion-core/`** — plain Java library (no Spring Boot app of its own) holding the hexagonal document-ingestion slice (domain model, ports, JDBC persistence, Tika/chunk/pgvector indexing) shared between `backend` (web upload) and `sharepoint-batch` (SharePoint polling) — see Shared Ingestion Library below
- **`sharepoint-batch/`** — standalone Spring Boot + Spring Batch service that polls a Microsoft SharePoint document library via Microsoft Graph and (re-)ingests changed files through `ingestion-core`, on its own deployment/schedule separate from `backend` — see SharePoint Batch Service below

## Tech Stack

### Backend (`backend/`)

- **Java 25** (toolchain)
- **Spring Boot 4.1.1** — ships as a GraalVM native-image container image, built via Cloud Native Buildpacks and published to GHCR on every push to `main` (see Build below)
- **Spring Data JDBC** + **Liquibase** (PostgreSQL)
- **Spring MVC** (webmvc)
- **Spring AI** — OpenAI-compatible client (`spring-ai-starter-model-openai`, not Ollama's native client) against whichever OpenAI-compatible endpoint an environment has: Ollama's own `/v1` routes locally and in tests (Ollama exposes these alongside its native API — see Running the Backend and Testcontainers below), Tanzu Platform's `genai-service` marketplace proxy or another OpenAI-compatible endpoint in production (see Deployment below); **pgvector** vector store; JDBC-backed chat memory
- **Observability**: Micrometer tracing (Brave bridge), Prometheus, datasource-micrometer
- **Testcontainers**: PostgreSQL, Ollama

### Frontend (`frontend/`)

- **React** + **TypeScript**, scaffolded with **Vite** (`react-ts` template)
- **npm** as package manager
- **React Router** for navigation, **TanStack Query** for server state, **Tailwind CSS v4** for styling — no separate component library
- **Vitest** + **React Testing Library** for tests (`npm test` in `frontend/`)

Dev server proxies `/api` to the backend (`vite.config.ts`) rather than the frontend hardcoding a backend origin — frontend code always calls relative paths (`/api/...`) unless `VITE_API_BASE_URL` is set at build time (see `src/vite-env.d.ts`, `src/api/client.ts`), in which case every request is prefixed with it. Cloud Foundry (see Deployment below) is the deployment topology that needs this: frontend and backend are separate CF routes/origins there, not one shared origin, so the frontend's build has to know the backend's absolute URL ahead of time.

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

For a one-off manual run against an already-running Postgres and a real (non-Testcontainers) Ollama instance — e.g. the GPU-backed one, see memory — skip the `local` profile and override directly. The app talks to Ollama via its OpenAI-compatible `/v1` routes, not Ollama's native API (see Tech Stack above), so the URL needs that suffix, and `SPRING_AI_OPENAI_API_KEY` needs an explicit empty value (a deliberate no-auth signal, not the same as leaving it unset — see `OpenAiAutoConfigurationUtil` in `spring-ai-openai`):

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/client_rag_demo \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=postgres \
SPRING_AI_OPENAI_BASE_URL=http://<ollama-host>:11434/v1 \
SPRING_AI_OPENAI_API_KEY= \
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

## Shared Ingestion Library (`ingestion-core/`)

Holds everything both `backend` and `sharepoint-batch` need to turn bytes into an indexed,
status-tracked document: `Document`/`ContentType`/`DocumentStatus` domain model, the
`Save`/`LoadById`/`LoadByFilename`/`LoadBySourceAndExternalId`/`LoadAll`/`Delete`
`DocumentPort`s, `IngestDocumentUseCase`/`DeleteDocumentUseCase`, `DocumentPersistenceAdapter`
(Spring Data JDBC), and `DocumentIndexingAdapter` (Tika parse → `TokenTextSplitter` chunk →
`VectorStore.add`). Same hexagonal conventions as every `backend` feature — one difference:
`IngestionCoreConfiguration` (`@ComponentScan` + `@EnableJdbcRepositories` +
`@EnableConfigurationProperties`) has to be explicitly `@Import`ed by each consuming app's own
config, since this module has no `@SpringBootApplication` of its own to auto-scan it.

- **Plain Java library, not a Spring Boot app** — no `org.springframework.boot` plugin, no Boot
  starters as main dependencies (`ingestion-core/build.gradle` uses `java-library` +
  `io.spring.dependency-management` importing the same `spring-boot-dependencies`/`spring-ai-bom`
  versions `backend` uses, so both stay in lockstep). Only the non-starter `spring-ai-model`/
  `spring-ai-vector-store` API jars are `api` dependencies — each consuming app supplies the real
  `VectorStore`/`EmbeddingModel` beans from its own starter dependencies, so this module never
  drags Boot autoconfiguration or a pgvector driver into whatever depends on it.
- **`Document.source`/`externalId`/`sourceVersion`** (added by `documents-003`, alongside the
  original `filename`-only tracking from the web-upload-only era) distinguish `UPLOAD` rows
  (identified by filename, `externalId`/`sourceVersion` null) from `SHAREPOINT` rows (identified
  by the Graph driveItem id via `LoadDocumentBySourceAndExternalIdPort` — filename can collide
  across SharePoint folders, a driveItem id can't; `sourceVersion` holds the driveItem's eTag for
  change detection). The unique constraint moved from `filename` alone to `(source, filename)`.
- **Schema ownership**: `backend` remains the *sole* Liquibase runner against `documents`/
  `vector_store` (`ingestion-core`'s changelog is packaged into its jar and referenced by
  `backend`'s own `db.changelog-master.yaml` via classpath, same cross-jar `sqlFile`/`include`
  trick already used for `spring-ai-model-chat-memory-repository-jdbc`'s vendor schema).
  `sharepoint-batch` never runs these changesets itself — only its own (see below) — to avoid two
  independent Liquibase instances racing over one changelog-lock/history table. Operational
  consequence: **`sharepoint-batch` must not be pointed at a database `backend` hasn't migrated
  at least once.**
- **Testing**: no HTTP layer to test through (that's `backend`'s `DocumentController`), so
  `ingestion-core`'s own IT (`IngestDocumentServiceIT`) exercises `IngestDocumentUseCase`/
  `DeleteDocumentUseCase` directly against real Testcontainers Postgres+pgvector+Ollama, via a
  test-only synthetic `@SpringBootApplication` (`IngestionCoreTestApplication`) that `@Import`s
  `IngestionCoreConfiguration` — the standard shape for testing a library module that deliberately
  isn't a Boot app itself. A test-scope-only gotcha found here: Ollama's OpenAI-compatible
  endpoint 404s ("model not found, try pulling it first") unless
  `spring.ai.openai.embedding.model` explicitly matches whatever
  `TestcontainersConfiguration` actually pulled (`nomic-embed-text`) — leaving it unset doesn't
  fall back to that model, it falls back to Spring AI's own OpenAI default
  (`text-embedding-ada-002`), which was never pulled. Set in both `ingestion-core`'s and
  `sharepoint-batch`'s test `application.yaml`.
- **`IngestionCoreConfiguration` supplies its own `JdbcAggregateOperations`
  (`IngestionJdbcConfiguration extends AbstractJdbcConfiguration`), rather than relying on the
  consuming app's own `JdbcRepositoriesAutoConfiguration`** — found necessary after the exact same
  failure (`Cannot resolve reference to bean 'JdbcAggregateOperations'` on
  `DocumentJdbcRepository`) recurred through three *different* mechanisms across three different
  consuming-app contexts: CF's `java_buildpack` injecting `java-cfenv` (see SharePoint Batch
  Service below), `sharepoint-batch`'s own now-removed `@EnableBatchProcessing` (which bypasses
  Boot's `BatchAutoConfiguration` entirely), and `spring-cloud-task-batch`'s own autoconfiguration
  racing `@EnableJdbcRepositories`'s eager repository-FactoryBean creation. Boot's
  `JdbcRepositoriesAutoConfiguration` is deferred until *after* regular `@Configuration` classes
  are processed, but `@EnableJdbcRepositories` (used by `IngestionCoreConfiguration` directly, not
  via autoconfiguration) creates its repository FactoryBean eagerly in that earlier pass — a
  chicken-and-egg ordering race that happened to resolve correctly often enough to look solid,
  until some new autoconfiguration class in a *particular* consuming app shifted bean-creation
  order enough to expose it. A library depended on by multiple apps with different
  autoconfiguration profiles shouldn't rely on getting this right by accident in every one of
  them. `IngestionJdbcConfiguration` overrides just `jdbcDialect()` with
  `@ConditionalOnMissingBean(JdbcDialect.class)` so it still defers to `backend`'s own explicit
  `JdbcDialectConfig` (added for a *different*, AOT-specific reason — see GraalVM native image
  below) rather than double-defining that one bean, which otherwise fails outright with
  `BeanDefinitionOverrideException` under AOT's stricter bean-registration pass (caught by
  `:backend:processTestAot`, not assumed).
- **`documents-004` makes `uploaded_by` nullable** — it was `NOT NULL` from the original
  web-upload-only design (`documents-001`), before `SOURCE_SHAREPOINT`/other automated-ingestion
  rows existed; those legitimately have no human uploader. Caught by `IngestDocumentServiceIT`
  failing with a real `DataIntegrityViolationException` against a real Postgres, not assumed.

## SharePoint Batch Service (`sharepoint-batch/`)

Polls a SharePoint document library on a schedule and (re-)ingests changed files through
`ingestion-core`. One correction worth keeping in mind: there's no GraphQL API for SharePoint —
Microsoft's modern surface is **Microsoft Graph**, a REST+OData API
(`com.microsoft.graph:microsoft-graph`, the official Java SDK); "the SharePoint GraphQL client"
in casual conversation means this.

- **Spring Batch job shape**: `sharePointSyncJob` = `resolveDeltaLinkStep` (tasklet: loads the
  delta link persisted from the previous poll, or builds the base delta URL on a first-ever run)
  → `syncStep` (chunk-oriented: `ChangedItemReader` pages Graph's delta query via
  `ListChangedItemsPort`, `ChangedItemProcessor` finds-or-creates the `Document` row and downloads
  content via `DownloadItemContentPort`, `ChangedItemWriter` calls `IngestDocumentUseCase`/
  `DeleteDocumentUseCase`) → `persistDeltaLinkStep` (tasklet: writes the new delta link, only
  reached after `syncStep` fully succeeds). The delta link travels step-to-step via the **job's**
  `ExecutionContext` (a step's own `ExecutionContext` isn't visible to other steps) —
  `ChangedItemReader` reads its start link via `@StepScope`'s `#{jobExecutionContext[...]}` SpEL
  binding and writes the final one back via `ItemStream#update`, promoted from step to job level
  by an `ExecutionContextPromotionListener` registered on `syncStep`.
- **Deployment model: a Spring Cloud Task, not a long-running service** — `SharepointBatchApplication`
  is `@EnableTask` + `spring-cloud-starter-task` (pulling in `spring-cloud-task-batch`), not
  `@EnableScheduling`. `TaskJobLauncherApplicationRunner` (from `spring-cloud-task-batch`) launches
  `sharePointSyncJob` once at startup and the JVM exits when it's done — there's deliberately no
  web starter on the classpath (Boot infers `WebApplicationType.NONE`), since a persistent process
  with nothing to keep it alive would just be dead weight. `sharePointSyncJob`'s
  `.incrementer(new RunIdIncrementer())` gives each fresh JVM invocation a unique `run.id`
  automatically, since there's no `@Scheduled` loop supplying one — every invocation is a brand
  new process with no CLI-supplied `JobParameters` of its own. On Cloud Foundry this deploys as a
  **CF Task** (`cf run-task client-rag-demo-sharepoint-batch`, see `manifest.yml` — pushed with
  `instances: 0`, never started as a running app) triggered by an *external* scheduler (Scheduler
  for VMware Tanzu, or plain cron calling `cf run-task`); this app no longer schedules itself
  internally. Needs its own schema on top of Spring Batch's — `TASK_EXECUTION`/
  `TASK_EXECUTION_PARAMS`/`TASK_TASK_BATCH`/etc. (`db.changelog-sharepoint-002.yaml`, same
  vendor-`sqlFile` convention as Spring Batch's own schema, `spring.cloud.task.initialize-enabled: false`
  so Liquibase stays the sole owner) — `TASK_TASK_BATCH` is what correlates a `TASK_EXECUTION` row
  with the job's own `BATCH_JOB_EXECUTION` row. Tests override `spring.batch.job.enabled: false`
  (the opposite of production) so the job doesn't auto-fire at context startup before each test's
  own `@BeforeEach`/`JobLauncherTestUtils.launchJob(...)` gets to run.
- **Graph/SharePoint specifics stay entirely inside this app** (`sharepoint/adapter/out/graph/`,
  `sharepoint/configuration/`) — never in `ingestion-core`, which stays source-agnostic. Auth is
  app-only client-credentials (`com.azure:azure-identity`'s `ClientSecretCredential`, scope
  `https://graph.microsoft.com/.default`) against an Azure AD app registration with application
  permission `Sites.Selected` scoped to one site.
- **A second, filesystem-backed ingestion source exists alongside Graph** — `app.ingestion.source`
  (`graph`, default, or `filesystem`) picks between `GraphChangedItemsAdapter`/
  `GraphItemDownloadAdapter` and `FilesystemChangedItemsAdapter`/`FilesystemItemDownloadAdapter`
  (`sharepoint/adapter/out/filesystem/`), each pair `@ConditionalOnProperty`-gated so only one is
  ever wired in — including the `GraphServiceClient` bean itself, so a filesystem-only deployment
  never needs real Graph credentials. This is a genuine alternative production input (the client
  themselves floated an NFS-style mount as a possible SharePoint substitute), not just test
  scaffolding, tested end-to-end against a real Cloud Foundry `block-storage` volume service and a
  real document corpus. A plain filesystem has no delta-link/eTag concept, so
  `FilesystemChangedItemsAdapter` always does one full recursive scan per invocation (size+mtime
  stands in for `eTag`) and **does not detect deletions** — a real gap before this becomes more
  than a one-off test. The container mount path a bound volume service gets is only knowable
  *after* binding (`cf env <app>`'s `VCAP_SERVICES.block-storage[0].volume_mounts[0].container_dir`
  — CF assigns a GUID-based path, there's no way to request one via bind parameters for this
  broker), so `APP_FILESYSTEM_SOURCE_PATH` has to be set manually per fresh bind; see the
  `manifest.yml` comment.
- **Deployment: plain JVM image, not GraalVM native** — deliberately, unlike `backend`. This app
  would inherit the Tika/POI/XMLBeans native-image reflection fights `backend` already documents
  above, plus need new hints for the Graph SDK, for no benefit: a short-lived task's startup
  latency doesn't matter the way a request-serving backend's does.
- **`java_buildpack` breaks `@EnableJdbcRepositories` outright unless explicitly told not to
  "help"** — unlike `backend`'s Docker/native-image path, `java_buildpack` unconditionally adds
  `java-cfenv-boot` (on-disk component name `java_cf_env`, confirmed via `cf run-task` shelling
  out to `find` — `cf ssh` was blocked in the environment this was debugged from, `cf run-task`
  wasn't, and turned out to be the more reliable diagnostic tool anyway since it doesn't need an
  interactively-reachable running instance) to the classpath whenever it detects bound services.
  Its `CloudProfileApplicationListener` activates a `cloud` Spring profile and rebinds the
  datasource from `VCAP_SERVICES`, which broke `ingestion-core`'s `@EnableJdbcRepositories` wiring
  outright on the very first real deploy (`JdbcAggregateOperations` bean not found — see
  `IngestionJdbcConfiguration` above, which is the durable fix; disabling this buildpack component
  is still worth doing regardless, since the datasource rebind itself is redundant here). The fix
  for *this* symptom is the buildpack's own documented per-component opt-out,
  `JBP_CONFIG_JAVA_CF_ENV: '{enabled: false}'` — note this is *not* the same as the older,
  differently-named `JBP_CONFIG_SPRING_AUTO_RECONFIGURATION` (a legacy, unrelated framework
  component with zero effect on this buildpack version despite the superficially similar purpose —
  tried first, confirmed to do nothing here). This app already configures its datasource
  explicitly via `SPRING_DATASOURCE_*` env vars, so the buildpack's own auto-reconfiguration was
  always redundant.
- **`spring-batch-core` 6.0.5 (pulled in transitively by `spring-boot-starter-batch` under Boot
  4.1.1) reorganized nearly every core class's package** from the Spring Batch 5.x layout most
  documentation/examples still show — confirmed by inspecting the actual jars, not assumed:
  `Job`/`Step` moved to `org.springframework.batch.core.job`/`.step`; `JobParameters(Builder)` to
  `org.springframework.batch.core.job.parameters`; `JobExplorer` to
  `org.springframework.batch.core.repository.explore`; `ItemReader`/`Writer`/`Processor`/`Stream`/
  `ExecutionContext`/`Chunk` all moved out of `org.springframework.batch.item` entirely into
  `org.springframework.batch.infrastructure.item` (a separate `spring-batch-infrastructure` jar);
  `RepeatStatus` similarly into `org.springframework.batch.infrastructure.repeat`. Also new in
  6.0: a unified `JobOperator` (extends `JobLauncher`) is the modern way to launch/inspect jobs,
  replacing separate `JobLauncher`+`JobExplorer` calls — though at this exact 6.0.5 release
  `JobLauncher`, `JobOperator`, and `JobOperator.getRunningExecutions` are *themselves* already
  flagged `@Deprecated(forRemoval=true)`, with no non-deprecated alternative shipped yet (confirmed
  via `javap`, not a false positive) — this is 6.0's launch API still mid-churn, not something
  this codebase got wrong; revisit on the next Spring Batch upgrade. `spring-batch-test`'s own
  package (`org.springframework.batch.test`) is unchanged.
- **Testing** (no way to test against a real SharePoint tenant — everything below leans on fakes):
  unit tests for `ChangedItemProcessor` against Mockito-mocked ports
  (`ChangedItemProcessorTest`); a full-job IT (`SharePointSyncJobIT`, `@SpringBatchTest` +
  `JobLauncherTestUtils`, real Testcontainers Postgres+pgvector+Ollama for the shared-schema
  writes) with the Graph side replaced by `FakeListChangedItemsPort`/`FakeDownloadItemContentPort`
  (`@Primary` test beans — the real Graph adapters are still component-scanned into the test
  context alongside them, so `@Primary` is what wins the ambiguity, not an exclusion filter),
  covering a first-ever full-enumeration run and a single-item-skip run; `SyncStateJdbcAdapterIT`
  for the delta-link persistence round trip. **Not covered**: a restart-after-failure scenario and
  a mixed incremental add+modify+delete run — real gaps to close before this handles production
  traffic, not implemented here due to time. One gotcha worth knowing before adding more
  `@SpringBatchTest` test methods: its `StepScopeTestExecutionListener` reflectively scans the
  test class's own declared methods for *any* method whose return type is `StepExecution` —
  matched by return type alone, not by name — and invokes whichever one it finds with zero
  arguments to set up step-scope test context. A local helper method that happens to return
  `StepExecution` (regardless of what it's called or how many parameters it takes) gets swept up
  and breaks every test in the class with `Could not create step execution from method: <name>`;
  confirmed by decompiling the listener, not guessed. `StepExecutionLookup` is a separate
  (non-nested) class for exactly this reason — renaming the method doesn't help, since the match
  isn't name-based.
- **Unverified against a real tenant**: `GraphChangedItemsAdapter`/`GraphItemDownloadAdapter`
  compile cleanly against the real `microsoft-graph` SDK jar (confirming the method chains/type
  names are real), but there's no way to confirm the actual request/response shape is correct
  without a real SharePoint site. The first thing to do once the client supplies tenant/site/drive
  credentials is a real end-to-end poll against a small test document library, before trusting
  this against production data.

## Database Migrations

Liquibase changelogs live in `backend/src/main/resources/db/changelog/`. The master changelog is `db.changelog-master.yaml`. Add new changesets as separate files and include them from the master.

Several dependencies own tables that need a schema but (correctly) don't manage it themselves against a real Postgres — check before hand-writing DDL:
- If the owning module ships real per-vendor `.sql` files (e.g. `spring-ai-model-chat-memory-repository-jdbc`'s `schema-postgresql.sql`), reference the file directly from the changelog with a `sqlFile` change pointing at its classpath resource path inside the dependency jar (`db/changelog/chat/db.changelog-chat-001.yaml` is the example) — don't copy its contents in.
- If it doesn't (e.g. `spring-ai-pgvector-store`'s `PgVectorStore` builds its DDL as Java string templates, not a shipped file), there's nothing to reference; the DDL has to be reconstructed by hand and verified against the library's actual runtime behavior (`db/changelog/documents/db.changelog-documents-002.yaml` is the example — verified by decompiling `PgVectorStore.class`, then confirming a real insert/query/delete against it works end-to-end in `DocumentControllerIT`, not just by starting successfully).
- Either way, set that dependency's own `initialize-schema` property to disabled (`false` / `never`, whichever it uses) so it doesn't also try to create the table itself.

## Build

### Container Image (CI)

`.github/workflows/native-image.yaml` runs the backend test suite, then (on push to `main`, or manual dispatch) builds a GraalVM native-image container via Cloud Native Buildpacks and publishes it to `ghcr.io/<owner>/<repo>:latest`, authenticating with the workflow's own `GITHUB_TOKEN` (needs `packages: write`, already set in the workflow) — no separate registry secret needed:

```bash
./gradlew :backend:bootBuildImage --imageName=ghcr.io/<owner>/<repo>:latest --publishImage
```

Locally, `./gradlew :backend:bootBuildImage` (imageName defaults to `ghcr.io/dmfrey/client-rag-demo:${version}` per `backend/build.gradle`) builds the same image into the local Docker/Podman daemon without publishing. `BP_NATIVE_IMAGE=true` (set as the task's `environment`, not a shell env var) is what tells the Paketo builder to produce a native image instead of a regular JVM layer — the actual `native-image` compilation runs inside Paketo's Linux builder container, so neither a local machine nor the CI runner needs GraalVM installed themselves, just Docker/Podman.

If your local Docker CLI config (`~/.docker/config.json`) has `credsStore`/`currentContext` pointing at Docker Desktop (common if it was ever installed, even though this project uses Podman) - `bootBuildImage` will fail trying to resolve credentials or find a socket that doesn't exist under Podman. Don't work around this by editing `bootBuildImage`'s `docker {}` block in `build.gradle` (that's shared config, including for CI, which has neither problem) - fix or ignore the local Docker CLI config instead, e.g. `docker context use default` or removing the stale `credsStore` entry.

### GraalVM native image

Working, as of the fixes below - all discovered by actually running `processAot`, `nativeCompile`, and a real native executable against Postgres/Ollama, not anticipated in advance:

- **`processAot` needs a live, reachable database purely because of Spring Data JDBC's dialect auto-detection** (`DataJdbcRepositoriesAutoConfiguration$SpringBootJdbcConfiguration#jdbcDialect`, `@ConditionalOnMissingBean`) - AOT's eager bean-factory introspection instantiates `NamedParameterJdbcOperations` → `dataSource` to query the dialect from a real connection, and does so *before* `@ConfigurationProperties` binding has run for that early pass, so `spring.datasource.*` (env var or `-D`, doesn't matter) is seen as unset and dataSource creation fails with "Failed to determine a suitable driver class" even with a real reachable database and correct properties. Fixed by declaring an explicit `JdbcDialect` bean (`configuration/JdbcDialectConfig`, `JdbcPostgresDialect.INSTANCE` - this app is Postgres-only) so Boot's auto-detecting bean never gets created at all; also removes a real, if small, per-startup DB round trip in production.
- **Liquibase's changelog parser reflectively invokes each Change/config class's setters (to populate it) and getters (to recompute checksums, on *every* startup, not just the first)** - GraalVM strips unregistered members, so an uncovered type throws `MissingReflectionRegistrationError` at whatever point that type's parsing/checksum path is first hit, which can be startup N, not necessarily startup 1. `configuration/LiquibaseRuntimeHints` registers exactly the change types this project's changelogs use (`createTable`, `createIndex`, `sql`, `sqlFile`) plus their shared superclasses. Add a type there if a new changelog change type is introduced and this error resurfaces.
- **`org.eclipse.angus:angus-activation`** (pulled in transitively via `jaxb-core`, used by Tika/POI for XML/OOXML parsing, nothing to do with email) **registers a native-image `Feature` that unconditionally references `jakarta.mail.Part`** - without that class on the classpath, native-image generation itself fails with `NoClassDefFoundError` before the app even starts. Fixed with a `runtimeOnly` dependency on just `jakarta.mail:jakarta.mail-api` (API only, no implementation, no functionality used) to satisfy the class lookup.
- **PDFBox 3.x's `PDDocument` static initializer touches `java.awt.image.ColorModel`/`Raster`** (AWT/Java2D, apparently regardless of whether the PDF or the extraction path involves images at all) **- on macOS specifically, this crashed a directly-invoked (`nativeCompile`) native executable at runtime with `NoSuchFieldError: ColorModel.nBits`**, a JNI field-registration gap in GraalVM's macOS AWT native-image support. Did not reproduce building the same app the way it's actually shipped - via Cloud Native Buildpacks (Linux, see above) - so treated as a macOS-native-image-only gap, not something this project's production path hits.
- **PDFBox ships none of its own native-image metadata at all** (no `META-INF/native-image` directory in its jar, confirmed by inspecting it directly) **- every classpath resource it reads at runtime is silently missing from the native image unless registered**: the first real PDF uploaded to the actual deployed backend failed ingestion with `IOException: resource '/org/apache/pdfbox/resources/afm/ZapfDingbats.afm' not found` - a font-metrics file for one of the 14 standard PDF fonts, needed depending on what a given PDF actually uses (fonts/CMaps/ICC profiles/glyphs vary per document, so this is exactly the kind of gap no fixed set of local test PDFs would reliably catch either). `configuration/PdfBoxRuntimeHints` registers all of `org/apache/pdfbox/resources/**` rather than one file at a time as different PDFs exercise different resources.
- **`openai-java-core`'s own bundled `META-INF/native-image/reflect-config.json` is itself incomplete** - about a third of its ~12,600 entries (confirmed by inspecting the shipped JSON) grant only reflection *query* access, not *invoke* access, to `com.openai.models.*` request/response classes Jackson deserializes reflectively. Invisible to any local/CI testing, which only ever exercises Ollama's `/v1` endpoint: the first real embeddings call against Tanzu Platform's genai proxy in production crashed with `MissingReflectionRegistrationError` invoking `CreateEmbeddingResponse$Usage.putAdditionalProperty` - a Jackson "any-setter" fallback for JSON fields the generated model doesn't declare, which that proxy's responses have and Ollama's don't. `configuration/OpenAiRuntimeHints` scans `com.openai.models` via the classpath and grants full invoke/field access to everything found, rather than fixing one class at a time as each new response shape gets exercised in production.
- Micrometer/Prometheus needed no extra hints - `micrometer-registry-prometheus` is `runtimeOnly` and worked without further changes.

### CI/CD

**Executable jars (GitHub Packages)**: alongside its native-image container, each of `native-image.yaml` and `sharepoint-batch.yaml` has its own `publish-jar` job (depends on `test`, runs in parallel with the image-publish job) that publishes that app's `bootJar` to this repo's GitHub Packages Maven registry (`com.example:client-rag-demo-backend` / `client-rag-demo-sharepoint-batch`, versioned off the existing `0.0.1-SNAPSHOT`) — a plain-JVM artifact for anyone running these outside the native-image/CF path, using the workflow's own `GITHUB_TOKEN` (no new secret). Both `build.gradle` files' `publishing {}` blocks need `./gradlew :<module>:publish --no-configuration-cache` specifically — `maven-publish`'s `GenerateMavenPom` task can't serialize into this project's configuration cache (`Configuration`/`Project`/`DependencyHandler` aren't cache-safe types, confirmed locally: the task itself succeeds and produces a correct POM either way, only the cache *write* fails and turns that into a false `BUILD FAILED`).

**Automated CF deploy (`deploy-cf.yaml`)**: `cf push`es all three `manifest.yml` apps (backend, frontend, sharepoint-batch) on every push to `main` (also `workflow_dispatch` for an ad-hoc redeploy, e.g. after a config-only `manifest.yml` change that doesn't touch any build workflow's path filters). The actual safety gate isn't the trigger — it's a **required-reviewer protection rule on a `production` GitHub Environment** (created via `gh api --method PUT repos/<owner>/<repo>/environments/production` with a `required_reviewers` rule, since this isn't expressible in the workflow YAML itself), which pauses the job immediately after it's created until a human clicks Approve in the Actions UI. By the time you approve, you've had a chance to confirm the two build workflows finished green.

CF login uses a service-account username/password (`CF_USERNAME`/`CF_PASSWORD` repo secrets) — UAA `client_credentials` wasn't available for this org/space. The four values `vars.yml` normally supplies locally (Postgres URL/username/password, genai API key) are **not** duplicated as separate GitHub secrets — the job is already authenticated to CF by that point, so it fetches them live via `cf service-key client-rag-demo-db client-rag-demo-db-key` / `cf service-key client-rag-demo-ai client-rag-demo-ai-key` (creating the keys first if they don't already exist) and writes them into a runtime-only `vars.yml`, keeping CF as the single source of truth. `VCAP_SERVICES`/`cf env` was considered and rejected for this: confirmed by directly inspecting a running app's `cf env` output that `postgres` bindings expose full credentials there, but `ai-models` bindings only expose a CredHub pointer (`{"credhub-ref": "/c/..."}`) — the real API key is only resolvable via an actual service-key creation, not the app's own binding view. Values pulled this way aren't in the `secrets.*` context, so nothing masks them automatically — each is registered with `echo "::add-mask::$VALUE"` before being written anywhere, the same log redaction `secrets.*` gets for free.

Two gotchas hit getting the first real run to succeed, neither a workflow bug:
- **A workflow run queued before a secret exists never sees that secret, even after later waiting through the approval gate and executing steps well after the secret was created.** The first real deploy attempt showed `CF_USERNAME`/`CF_PASSWORD` as blank in the log despite `gh secret list`/the GitHub API confirming both existed at the repo level — root-caused by timing: the run had been queued (and started waiting on approval) *before* the secrets were added, several minutes earlier. A fresh run created after the secrets existed picked them up correctly. If a deploy run mysteriously can't see a secret you just added, re-trigger rather than re-approving the stale one.
- **A real Cloud Controller-side 500** (`Puma caught this error: undefined method 'escape' for true (NoMethodError)`, deep in `cloud_controller_ng`'s `security_context_configurer.rb#user_from_token`) hit the very next attempt, after login/credential-fetch both succeeded — the same category of transient CF platform flakiness already documented elsewhere in this file (staging OOM, Log Cache 404s), resolved by simply retrying rather than treating it as a real failure.

## Deployment

`manifest.yml` (repo root) deploys both apps to Cloud Foundry with one `cf push --vars-file vars.yml`. See the comment block at the top of the file for the exact order of operations, since the frontend has to be built with the backend's route baked in *before* `cf push` runs (it's a static SPA — see Tech Stack above). Route domains and model names are filled in directly in the manifest (`apps.tas-ndc.kuhn-labs.com`, confirmed via `cf domains`); the four real secrets (Postgres URL/username/password, the genai proxy's API key) are `((var))` references resolved from `vars.yml` — copy `vars.yml.example` to `vars.yml` and fill it in from the two services' keys (`cf create-service-key` / `cf service-key` — see the manifest's own comments for the exact commands). `vars.yml` is gitignored; never put real credentials directly in `manifest.yml`, it's committed.

The two backing services (`client-rag-demo-db` — Postgres, `client-rag-demo-ai` — the `ai-models`/`genai-service` marketplace proxy) already exist in this foundation. Binding them in the manifest does *not* auto-populate Spring properties from `VCAP_SERVICES` — confirmed by reading `spring-cloud-bindings`' actual source rather than assuming: it only reads bindings materialized as files under `$SERVICE_BINDING_ROOT`/`$CNB_BINDINGS` (the Kubernetes/Cloud-Native-Buildpacks convention), never `VCAP_SERVICES` itself, so it wouldn't help here even if this app depended on it. A service key is the only way to get real values. The AI service's `nomic-ai/nomic-embed-text-v2-moe` embedding model was confirmed via a real embeddings API call to output 768 dimensions — matching `vector_store`'s schema exactly, no migration needed.

The backend app deploys via `docker: image:` pointing at the image `.github/workflows/native-image.yaml` publishes to GHCR (see Build above) — Cloud Foundry pulls the already-built native-image container rather than building from source. The frontend app deploys its `dist/` build via the `staticfile_buildpack`, with `frontend/public/Staticfile` (copied into `dist/` by Vite) enabling pushstate so React Router's client-side routes survive a direct load/refresh.

`application-cloud.yaml` (activated via `SPRING_PROFILES_ACTIVE: cloud` in the manifest) sets the session cookie's `SameSite=None; Secure` and `server.forward-headers-strategy: framework` — both required only because the frontend and backend are separate CF routes rather than one shared origin (cross-site cookies need `SameSite=None`, which browsers reject without `Secure`; CF's Gorouter terminates TLS in front of the app, so the app needs forwarded-header handling to know the original request was HTTPS).

