# Client RAG Demo

A retrieval-augmented generation (RAG) chat application. Authenticated users build a shared
knowledge base of documents, then chat with an LLM whose answers are grounded in that content and
cite the source documents. Documents arrive by manual upload in the web UI, or automatically from
a SharePoint document library (or a mounted filesystem) via a scheduled batch job.

It is built for deployment on **Tanzu Platform / Cloud Foundry**, against an OpenAI-compatible
model endpoint (Tanzu's `genai-service` proxy in production, Ollama locally) and PostgreSQL with
pgvector.

## Components

| Module | What it is |
|---|---|
| `backend/` | Spring Boot 4 REST API (Java 25). Deployed as a plain executable jar; reads its Postgres and model-proxy credentials from the bound Cloud Foundry services (`java-cfenv`). |
| `frontend/` | React + Vite + TypeScript single-page app, served as static files. |
| `ingestion-core/` | Plain Java library: the document-ingestion slice shared by `backend` and `sharepoint-batch`. |
| `sharepoint-batch/` | Spring Batch / Spring Cloud Task job that ingests from SharePoint or a filesystem. Runs once and exits. |

### Backend

- **Accounts**: local username/password registration and login (session cookie, no external IdP,
  no roles). Profile update and password change.
- **Consent banner**: a configurable banner each session must acknowledge before any other API
  call succeeds (enforced server-side, HTTP 428 otherwise).
- **Documents**: upload PDF, DOCX and TXT (up to 50 MB). Re-uploading a filename replaces the
  existing document. Ingestion is asynchronous: parse (Apache Tika) → chunk → embed → store in
  pgvector, with status `PROCESSING` / `READY` / `FAILED`. Delete removes the chunks too. One
  shared knowledge base for all users.
- **Chat**: per-user named sessions with auto-generated titles, rename, archive/unarchive and
  delete. Answers **stream** token-by-token (SSE), are grounded on retrieved document chunks, and
  include **citations** that are persisted with the message. Conversation memory is stored in
  Postgres (window size `app.chat.memory.max-messages`, default 20).
- **Operations**: Actuator health and Prometheus metrics; Micrometer tracing.

### Frontend

Login/registration, consent-banner gate, a documents page (drag-and-drop multi-upload, status
badges, replace-confirmation, delete), a chat page with streamed answers and source citations, a
sidebar of sessions (including archived), and a settings page. All API calls go to relative
`/api/...` paths unless `VITE_API_BASE_URL` is set at build time (see deployment).

### ingestion-core

Hexagonal domain/ports/adapters for turning bytes into an indexed, status-tracked document, so web
upload and batch ingestion behave identically. Documents carry a `source` (`UPLOAD` / `SHAREPOINT`),
an `externalId` and a `sourceVersion` (eTag) for change detection. `backend` is the sole owner of
the database schema (Liquibase).

### sharepoint-batch

A Cloud Foundry **task**, not a long-running app. Each run `resolveDeltaLink → sync → persistDeltaLink`:
only changed or deleted items are (re-)ingested.

- **`graph` source** (default): Microsoft Graph delta queries against one SharePoint document
  library, app-only auth (Azure AD app with `Sites.Selected`). *Not yet verified against a real
  tenant.*
- **`filesystem` source**: full recursive scan of a mounted directory (e.g. CF `block-storage`
  volume). Does not detect deletions.
- Triggered externally (`cf run-task`, Scheduler for VMware Tanzu, or cron).

## Architecture

```
 Browser ──► frontend (static) ──► backend ──► PostgreSQL + pgvector
                                      │            ▲
                                      ▼            │
                         OpenAI-compatible LLM     │
                         (chat + embeddings)       │
                                      ▲            │
 SharePoint / filesystem ──► sharepoint-batch ─────┘
```

Backend and batch share the same Postgres and embedding model, so their chunk size settings must
match.

## Local development

See `CLAUDE.md` for full detail. In short (Podman required for Testcontainers/Compose):

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew :backend:bootRun   # starts Postgres/Ollama via compose
cd frontend && npm install && npm run dev                  # proxies /api to the backend
./gradlew test                                              # all JVM modules
cd frontend && npm test
```

## Standard deployment (online)

`manifest.yml` deploys all three apps to Cloud Foundry: the backend as an executable jar
(`java_buildpack_offline`; `./gradlew :backend:bootJar`), the batch service from a GHCR container image
(built by CI), the frontend via `staticfile_buildpack`. The backend needs no credentials in the
manifest - it reads Postgres and the model proxy from its service bindings. The batch service
still takes its credentials from `vars.yml` (copy `vars.yml.example`; never commit it). `deploy-cf.yaml` automates this behind a
required-reviewer approval.

## Deploying in an offline environment

For a foundation/network with no access to GHCR, npm or Maven Central. Uses
**`manifest-offline.yml`**, which deploys the apps from files rather than images.

### 1. Gather artifacts (on a connected machine)

| Artifact | Where to get it |
|---|---|
| `client-rag-demo-backend-<version>.jar` | GitHub Packages (Maven): `com.example:client-rag-demo-backend` |
| `client-rag-demo-sharepoint-batch-<version>.jar` | GitHub Packages (Maven): `com.example:client-rag-demo-sharepoint-batch` |
| `frontend-dist.tar.gz` | GitHub Release `frontend-latest` (asset built by `frontend-package.yaml`) |
| `manifest-offline.yml`, `vars.yml.example` | this repo |

Packages are SNAPSHOTs, so file names carry a timestamp, e.g.
`client-rag-demo-backend-0.0.1-20260825.162146-1.jar`. Either rename the files or edit the `path:`
lines in the manifest to match what you downloaded.

### 2. Foundation prerequisites

- A Java buildpack that bundles a **Java 25** JRE (offline variant), and `staticfile_buildpack`.
  The manifests name them `java_buildpack_offline` and `staticfile_buildpack`; run `cf buildpacks`
  and change the `buildpacks:` entries in `manifest-offline.yml` if yours are named differently.
- A PostgreSQL service instance `client-rag-demo-db` (**with the pgvector extension available**),
  plus `rag-docs-volume` if using the filesystem source.
- **Two single-model GenAI services**, one per model, so which model is used is decided by which
  service you create - not by a model name in a manifest:

  ```bash
  cf create-service ai-models <chat-model-plan>      client-rag-demo-chat    # a CHAT model
  cf create-service ai-models <embedding-model-plan> client-rag-demo-embed   # a 768-dimension EMBEDDING model
  ```

  (On the original foundation these are the `tanzu-gpt-oss-120b` and `tanzu-nomic-embed-text-v2`
  plans; list yours with `cf marketplace -e ai-models`.) Each binding must expose
  `credentials.endpoint` with `config_url`, `api_key` and `api_base` - the Tanzu `ai-models`
  offering does. Each service should grant exactly one model of its kind; a service on an
  all-models plan is rejected at startup as ambiguous unless you name the model with
  `APP_GENAI_CHAT_MODEL` / `APP_GENAI_EMBEDDING_MODEL`.
- **Backend:** bind the three services (the manifest does this) - that is all. The backend finds
  Postgres and both model endpoints itself from `VCAP_SERVICES` (`java-cfenv`), with no service
  keys and nothing in `vars.yml`.
- **sharepoint-batch** (if you deploy it): still takes credentials from `vars.yml`. It only embeds
  documents, so it is bound to the embedding service; create service keys for the database and for
  `client-rag-demo-embed` and fill in `db-jdbc-url`, `db-username`, `db-password`, `ai-base-url`
  (the embedding key's `credentials.endpoint.openai_api_base`) and `ai-api-key`. Each GenAI service
  has its own endpoint URL, so the batch service's base URL is the embedding service's.

### 3. Values you must change

Everything in `manifest-offline.yml` that is specific to the original foundation:

| Setting | Where | Change to |
|---|---|---|
| Backend route `client-rag-demo-backend.apps.tas-ndc.kuhn-labs.com` | backend `routes` | your backend FQDN |
| Frontend route `client-rag-demo-frontend.apps.tas-ndc.kuhn-labs.com` | frontend `routes` | your frontend FQDN |
| `APP_CORS_ALLOWED_ORIGINS` | backend env | `https://<frontend FQDN>` exactly (scheme + host, no trailing slash) |
| `SPRING_AI_OPENAI_EMBEDDING_MODEL` | **batch env only** | the one model your embedding service grants (currently `nomic-ai/nomic-embed-text-v2-moe`). The backend has no model names - it uses whatever its two services grant |
| `ai-base-url` / `ai-api-key` | `vars.yml` (batch only) | the embedding service key's `openai_api_base` / `api_key` |
| `APP_DOCUMENTS_INGESTION_CHUNK_SIZE` | backend and batch env | `128` is tuned for a 512-token embedding limit; adjust to your model, keep both apps equal |
| Service names `client-rag-demo-db`, `client-rag-demo-chat`, `client-rag-demo-embed`, `rag-docs-volume` | `services:` | your service instance names. If the chat/embedding services are named differently, also set `APP_GENAI_CHAT_SERVICE` / `APP_GENAI_EMBEDDING_SERVICE` on the backend |
| `APP_INGESTION_SOURCE` | batch env | `filesystem` (current) or `graph` |
| `APP_FILESYSTEM_SOURCE_PATH` | batch env | the volume mount path (see `cf env`; changes on each fresh bind) |
| Graph settings (`APP_SHAREPOINT_TENANT_ID`, `_CLIENT_ID`, `_CLIENT_SECRET`, `_SITE_ID`, `_DRIVE_ID`) | batch env | only if `APP_INGESTION_SOURCE=graph`; not in the manifest by default — add them |
| jar `path:` values | backend, batch | the actual downloaded file names |
| `memory` | backend (1500M) | tune for the JVM; staging or running a JVM app can need more memory than expected |

> **Embedding model dimension.** The pgvector column is `vector(768)`, fixed by the Liquibase
> schema; nothing in a manifest states it. At startup the backend reads the column's dimension
> back from the database, asks the embedding model for a test vector and **refuses to start** if
> the two differ, naming both numbers - so a wrong embedding service shows up immediately, not on
> the first upload. Using a model with a different dimension requires a schema migration and a
> full reindex. The chat model can be changed freely (swap the service).

### 4. Prepare the frontend

The frontend bundle is built with a placeholder backend URL. Replace it with your backend route
before pushing (no Node/npm required):

```bash
mkdir -p frontend-dist && tar -xzf frontend-dist.tar.gz -C frontend-dist
grep -rl __RUNTIME_API_BASE_URL__ frontend-dist | xargs sed -i \
  's|__RUNTIME_API_BASE_URL__|https://<your backend FQDN>|g'      # macOS: sed -i ''
```

Because the frontend and backend are separate origins, the backend runs with the `cloud` profile
(`SameSite=None; Secure` cookies, forwarded headers) — this is already set in the manifest, and
requires both routes to be HTTPS.

### 5. Deploy

```
.
├── manifest-offline.yml
├── vars.yml
├── client-rag-demo-backend-<version>.jar
├── client-rag-demo-sharepoint-batch-<version>.jar
└── frontend-dist/
```

```bash
cf push -f manifest-offline.yml --vars-file vars.yml
```

Push the backend **first and let it start**: it runs the Liquibase migrations, and
`sharepoint-batch` must never run against a database the backend has not migrated at least once.
Then trigger ingestion on demand or from a scheduler:

```bash
cf run-task client-rag-demo-sharepoint-batch
```

### Offline caveats

- A JVM backend start (with Liquibase migrations) is slow; the manifest allows 180s for the health check - raise `timeout` if your foundation is slower.
- `java_buildpack` staging has previously been OOM-killed (exit 137) on some cells; retry or
  raise staging memory if it recurs.
- Both Java apps set `JBP_CONFIG_JAVA_CF_ENV: '{enabled: false}'`. For the backend this stops
  `java_buildpack` injecting a second copy of `java-cfenv` next to the one bundled in the jar; for
  sharepoint-batch it avoids the buildpack's own datasource rebinding. Keep it.
- Backend startup failures that name a GenAI service and say what to fix:
  - "No bound service named '...'" - the service isn't in the manifest's `services:` or is named
    differently; set `APP_GENAI_CHAT_SERVICE` / `APP_GENAI_EMBEDDING_SERVICE`.
  - "grants N CHAT/EMBEDDING models" - the service is on an all-models plan; bind a single-model
    plan, or name the model with `APP_GENAI_CHAT_MODEL` / `APP_GENAI_EMBEDDING_MODEL`.
  - "returns N-dimension vectors but vector_store.embedding is vector(768)" - the embedding service
    grants a model of a different dimension (see above).

## CI/CD

| Workflow | Purpose |
|---|---|
| `backend.yaml` | Test backend; publish its executable jar (GitHub Packages). |
| `sharepoint-batch.yaml` | Test the batch service; publish its native-image container (GHCR) and executable jar (GitHub Packages). |
| `frontend-package.yaml` | Publish the offline frontend bundle as the `frontend-latest` release asset. |
| `deploy-cf.yaml` | Build the backend jar, then `cf push` all apps, gated by a `production` environment approval. |

## Further reading

- `CLAUDE.md` — architecture, conventions, GraalVM and Cloud Foundry gotchas.
- `docs/requirements.md` — the v1 requirements and data model.
