# Client RAG Demo

Multi-module application targeting Tanzu Platform deployment:

- **`backend/`** — Spring Boot 4.1.0 application (this is the original single-module project; all Gradle/Java content lives here now)
- **`frontend/`** — React + Vite + TypeScript application

## Tech Stack

### Backend (`backend/`)

- **Java 25** (toolchain)
- **Spring Boot 4.1.0** with GraalVM native image (`org.graalvm.buildtools.native`)
- **Spring Data JDBC** + **Liquibase** (PostgreSQL)
- **Spring MVC** (webmvc)
- **Observability**: Micrometer tracing (Brave bridge), Prometheus, datasource-micrometer
- **Testcontainers**: PostgreSQL

### Frontend (`frontend/`)

- **React** + **TypeScript**, scaffolded with **Vite** (`react-ts` template)
- **npm** as package manager
- No test runner or component library added yet — bare Vite scaffold

How the frontend and backend integrate at runtime (dev proxy, static-asset serving, separate deployment, etc.) is not decided yet.

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

### Running the Backend

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew :backend:bootRun
```

The `local` profile (`application-local.yaml`) enables Spring Boot Docker Compose with `podman-compose`. Without it, Docker Compose is disabled (required for CI/AOT). Service definitions live in `backend/src/compose.yaml`.

### Running the Frontend

```bash
cd frontend
npm install   # first time only
npm run dev
```

## Backend Architecture

The backend (`backend/`) follows **Hexagonal Architecture** (Ports and Adapters). Features are the primary unit of organisation — each feature is a self-contained module under the base package `com.example.clientragdemo`.

### Package Structure

```
com.example.clientragdemo
├── configuration/                        ← cross-cutting Spring configuration
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
- Current type: `endpoint` (REST via Spring MVC)

**Output Adapters** (`adapter/out/`):
- Implement output port interfaces; encapsulate the output technology
- Current type: `persistence` (Spring Data JDBC)

**Feature Configuration** (`<feature>/configuration/`):
- Feature-scoped `@Configuration` only
- `@ComponentScan` scoped to the feature's root package — picks up `@Service`, `@Repository`, etc. within this feature only
- `@EnableJdbcRepositories` scoped to the feature's persistence package — limits Spring Data JDBC repository scanning to this feature

**Root Configuration** (`com.example.clientragdemo.configuration/`):
- Cross-cutting concerns only (security, observability config, etc.)

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

## Build

### Native Image (CI)

The CI workflow builds a native container image via Cloud Native Buildpacks:

```bash
./gradlew :backend:bootBuildImage
```

Registry credentials are passed as Gradle properties (`-PregistryUrl`, `-PregistryUsername`, `-PregistryPassword`).

### GraalVM reflection gaps (known issue class)


### CI/CD


## Deployment

