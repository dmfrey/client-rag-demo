# Offline deployment

Deploys the whole application (backend, frontend, SharePoint/filesystem batch job) to a Cloud
Foundry foundation with **no access to GHCR, npm or Maven Central**: the apps are pushed from
files you carry in, not pulled as images or built onsite.

This folder is self-contained - copy it onsite, drop the jars and the frontend bundle next to it,
fill in `vars.yml`, and run `./deploy.sh`.

| File | What it is |
|---|---|
| `manifest.yml` | The CF manifest. Everything that differs between sites is a `((variable))`. |
| `vars.yml.example` | The variables, with comments. Copy to `vars.yml` and edit. |
| `preflight.sh` | Checks vars, files, buildpacks, services and route domains before you push (read-only). |
| `prepare-frontend.sh` | Extracts the frontend bundle and points it at your backend route. |
| `deploy.sh` | `prepare-frontend.sh` + `preflight.sh` + `cf push`, in one command. |
| `lib.sh` | Helpers the scripts share. |

The scripts need only `bash`, `sed`, `grep`, `awk` and the `cf` CLI (logged in and targeting the
org/space you are deploying to).

## 1. Gather artifacts (on a connected machine)

| Artifact | Where to get it |
|---|---|
| `client-rag-demo-backend-<version>.jar` | GitHub Packages (Maven): `com.example:client-rag-demo-backend` |
| `client-rag-demo-sharepoint-batch-<version>.jar` | GitHub Packages (Maven): `com.example:client-rag-demo-sharepoint-batch` |
| `frontend-dist.tar.gz` | GitHub Release `frontend-latest` |
| this `offline/` folder | the repo |

Packages are SNAPSHOTs, so file names carry a timestamp, for example
`client-rag-demo-backend-0.0.1-20261015.171059-2.jar` - put the name you downloaded in `vars.yml`
(`backend-jar`, `batch-jar`); there is no need to rename anything.

## 2. Foundation prerequisites

- **Buildpacks:** a Java buildpack that bundles a **Java 25** JRE, and the staticfile buildpack.
  `cf buildpacks` shows their names (`java-buildpack` and `staticfile-buildpack` in `vars.yml`).
- **PostgreSQL** service instance, **with the pgvector extension available**.
- **Two single-model GenAI services**, one per model, so which model is used is decided by which
  service you create - not by a model name in a manifest:

  ```bash
  cf marketplace -e ai-models                                          # lists the plans
  cf create-service ai-models <chat-model-plan>      <chat-service>    # a CHAT model
  cf create-service ai-models <embedding-model-plan> <embed-service>   # a 768-dimension EMBEDDING model
  ```

  Each binding must expose `credentials.endpoint` with `config_url`, `api_key` and `api_base`
  (the Tanzu `ai-models` offering does). A service on an all-models plan is rejected at startup as
  ambiguous unless `APP_GENAI_CHAT_MODEL` / `APP_GENAI_EMBEDDING_MODEL` name the model.
- **A volume service** (block-storage / NFS-style) holding the documents, if `ingestion-source` is
  `filesystem`.

There are no service keys to create and no credentials to configure: both Java apps read Postgres
and their model endpoint(s) from their service bindings (`VCAP_SERVICES`) themselves.

> **Embedding model dimension.** The pgvector column is `vector(768)`, fixed by the database
> schema. At startup the backend (and the batch job, before it ingests anything) reads the column's
> dimension back from the database, asks the embedding model for a test vector and **refuses to
> start** if the two differ, naming both numbers. A different dimension would need a schema
> migration and a full re-index. The chat model can be changed freely (swap the service).

## 3. Fill in `vars.yml`

```bash
cp vars.yml.example vars.yml
$EDITOR vars.yml
```

| Variable | What to put | Also drives |
|---|---|---|
| `backend-route` | Backend hostname (no scheme), on a domain from `cf domains` | the frontend's API URL (`prepare-frontend.sh`) |
| `frontend-route` | Frontend hostname | the backend's CORS origin (`https://<frontend-route>`) |
| `backend-jar`, `batch-jar` | The downloaded jar file names | |
| `frontend-dir` | Where the frontend bundle is extracted (`./frontend-dist`) | |
| `java-buildpack`, `staticfile-buildpack` | Buildpack names from `cf buildpacks` | |
| `db-service` | PostgreSQL service instance name | the binding on both Java apps |
| `chat-service` | Chat GenAI service name | the binding **and** the backend's lookup of it |
| `embed-service` | Embedding GenAI service name | the binding **and** both apps' lookup of it |
| `volume-service` | Volume service name (filesystem source only) | the binding **and** the batch app's mount-path lookup |
| `ingestion-source` | `filesystem` or `graph` | |
| `chunk-size` | Document chunk size in tokens, shared by backend and batch (`128` suits an embedding model with a 512-token limit; lower it if documents fail to embed with a "too many tokens" error) | |
| `backend-memory`, `batch-memory` | JVM apps' memory (`1500M`) | |
| `startup-timeout` | Seconds CF waits for the backend to become healthy (`180`; a JVM start with database migrations is slow) | |

Values that must agree are driven by **one** variable each (the route and the CORS origin; a
service's name and the app's lookup of it), so they cannot drift apart.

The volume's mount path needs no setting: CF assigns it per binding, and the batch app reads it
from its own `VCAP_SERVICES`.

### SharePoint (Microsoft Graph) instead of a volume

Set `ingestion-source: graph`, then uncomment the five `APP_SHAREPOINT_*` lines in the batch app's
`env:` in `manifest.yml` and add the matching `sharepoint-*` variables to `vars.yml`. The client
secret then lives in `vars.yml` - keep it out of version control. (Not yet verified against a real
tenant.)

## 4. Deploy

```bash
./deploy.sh frontend-dist.tar.gz     # first time: extracts and prepares the frontend, checks, pushes
./deploy.sh                          # later: re-check and push
```

Or step by step: `./prepare-frontend.sh frontend-dist.tar.gz`, then `./preflight.sh`, then
`cf push -f manifest.yml --vars-file vars.yml`.

`preflight.sh` stops you with a clear `FAIL` line for: a variable with no value or a leftover
`example.com`; a missing jar; a frontend still carrying the placeholder; a buildpack, service or
route domain that does not exist on the foundation you are targeting. It only reads from CF.

The apps are pushed in manifest order, backend first. **Let the backend start before running the
batch job**: it runs the database migrations, and `sharepoint-batch` must never run against a
database the backend has not migrated at least once. Then trigger ingestion on demand or from an
external scheduler:

```bash
cf run-task client-rag-demo-sharepoint-batch
```

### Changing something later

Edit `vars.yml` and run `./deploy.sh`. To change `backend-route`, pass the tarball again
(`./deploy.sh frontend-dist.tar.gz`): an already-prepared bundle has lost its placeholder.

## Troubleshooting

- **Variable errors from `cf push`** ("Expected to find variables: ...") - a variable is missing
  from `vars.yml`; `preflight.sh` lists them first.
- **Staging fails or is OOM-killed** (exit 137) on some cells - retry, or raise staging memory.
- **Staging tries to download a JRE** - the buildpack does not bundle Java 25; use one that does,
  or point it at an internal mirror.
- **Both Java apps set `JBP_CONFIG_JAVA_CF_ENV: '{enabled: false}'`** - keep it. The buildpack
  would otherwise inject a second copy of `java-cfenv` next to the one bundled in each jar.
- **A startup failure that names a GenAI service** says what to fix:
  - "No bound service named '...'" - the service is not in the manifest's `services:` or is named
    differently (check `chat-service` / `embed-service`).
  - "grants N CHAT/EMBEDDING models" - the service is on an all-models plan; bind a single-model
    plan, or name the model with `APP_GENAI_CHAT_MODEL` / `APP_GENAI_EMBEDDING_MODEL`.
  - "returns N-dimension vectors but vector_store.embedding is vector(768)" - the embedding
    service grants a model of a different dimension.
- **`NoClassDefFoundError` for a Redis class** - nothing in these jars uses Redis (checked: no
  Redis library and no reference to `RedisConnectionFactory`). Look at the buildpack's
  Spring auto-reconfiguration (`JBP_CONFIG_SPRING_AUTO_RECONFIGURATION: '{enabled: false}'`) and at
  any Redis service bound to the app.
