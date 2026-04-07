# Knowledge Platform

Play Framework APIs for the Sunbird Knowledge Platform. Each service exposes REST endpoints for managing content, taxonomy, search, and assessments, backed by JanusGraph, YugabyteDB, Elasticsearch, and Redis.

---

## Table of Contents

1. [Modules](#modules)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
   - [Start all services](#start-all-services)
   - [Initialize YugabyteDB keyspaces](#initialize-yugabytedb-keyspaces)
   - [Redis (optional)](#redis-optional)
   - [Verify services](#verify-services)
4. [Building the Project](#building-the-project)
5. [Running a Service Locally](#running-a-service-locally)
   - [Option A — Run an individual service](#option-a--run-an-individual-service)
   - [Option B — Run Content, Taxonomy, and Assessment together](#option-b--run-content-taxonomy-and-assessment-together)
6. [Cloud Storage Configuration](#cloud-storage-configuration)
7. [CI/CD — GitHub Actions](#cicd--github-actions)

---

## Modules

| Module | Description |
|--------|-------------|
| `platform-core` | Shared libraries: graph engine, schema validators, actors, cloud storage |
| `ontology-engine` | Graph operations for content, taxonomy, and assessment nodes |
| `content-api/content-service` | Content and collection CRUD, hierarchy, publishing triggers |
| `taxonomy-api/taxonomy-service` | Frameworks, categories, terms, channels, licenses |
| `search-api/search-service` | Composite search across the knowledge graph via Elasticsearch |
| `assessment-api/assessment-service` | QuestionSets and assessment items |

---

## Prerequisites

Make sure these are installed before you begin:

- **Java 11** — verify with `java -version`
- **Maven 3.9+** — verify with `mvn -version`
- **Docker** — verify with `docker --version`

---

## Local Development Setup

All services are defined in `docker/docker-compose.yml`.

### Start all services

```shell
cd docker
docker compose up -d
```

This starts YugabyteDB, JanusGraph, Elasticsearch, and Kafka. JanusGraph automatically initializes the graph schema on startup via `docker/janusgraph/scripts/schema_init.groovy`.

Verify JanusGraph schema was initialized:
```shell
docker logs janusgraph | grep "SCHEMA INITIALIZATION"
# Expected: --- SCHEMA INITIALIZATION COMPLETE ---
```

### Initialize YugabyteDB keyspaces

Once YugabyteDB is up, run the CQL migration script to create the required keyspaces and tables. This downloads the migration files from [sunbird-spark-installer](https://github.com/Sunbird-Spark/sunbird-spark-installer/tree/develop/scripts/sunbird-yugabyte-migrations/sunbird-knowlg) and executes them against the local YugabyteDB container.

```shell
./init-yugabyte.sh              # env=dev, branch=develop
./init-yugabyte.sh sb           # env=sb, branch=develop
./init-yugabyte.sh dev main     # env=dev, branch=main
```

This only needs to be run once (or after `docker compose down -v` which deletes volumes).

### Redis (optional)

Redis is disabled by default. All service `application.conf` files ship with `redis.enable = false`, so the services read directly from the graph database. To enable Redis caching:

1. Start Redis:
   ```shell
   docker compose --profile redis up -d
   ```

2. Set `redis.enable = true` in the `application.conf` of the service you are running.

### Verify services

| Service | URL |
|---------|-----|
| YugabyteDB YCQL | localhost:9042 |
| JanusGraph (Gremlin) | ws://localhost:8182/gremlin |
| Elasticsearch | localhost:9200 |
| Kafka | localhost:9092 |

### Stop / Reset

```shell
cd docker
docker compose down            # stop containers, keep data
docker compose down -v         # stop containers and delete volumes
```

---

## Building the Project

From the repository root:

```shell
# Default build (Azure)
mvn clean install -DskipTests

# Build for specific Cloud Provider
mvn clean install -DskipTests -Paws   # For AWS S3
mvn clean install -DskipTests -Pgcp   # For Google Cloud Storage
mvn clean install -DskipTests -Poci   # For Oracle Cloud Infrastructure
```

A successful build ends with `BUILD SUCCESS`.

---

## Running a Service Locally

You can either run services individually or run Content, Taxonomy, and Assessment together via `knowlg-service`.

### Option A — Run an individual service

| Service | Module Path | Default Port |
|---------|-------------|--------------|
| **Content Service** | `content-api/content-service` | 9000 |
| **Search Service** | `search-api/search-service` | 9000 |
| **Taxonomy Service** | `taxonomy-api/taxonomy-service` | 9000 |
| **Assessment Service** | `assessment-api/assessment-service` | 9000 |

1. Make sure all containers from [Local Development Setup](#local-development-setup) are running.

2. Set the [cloud storage environment variables](#cloud-storage-configuration).

3. Run the service. Example for Taxonomy Service:

   **Linux:**
   ```shell
   cd taxonomy-api/taxonomy-service
   mvn play2:run
   ```

   **macOS:**
   ```shell
   cd taxonomy-api/taxonomy-service
   mvn play2:dist
   cd target
   tar xvzf taxonomy-service-1.0-SNAPSHOT-dist.zip
   cd taxonomy-service-1.0-SNAPSHOT
   ./start
   ```

4. Health check:
   ```shell
   curl http://localhost:9000/health
   ```

### Option B — Run Content, Taxonomy, and Assessment together

The `knowlg-service` module bundles Content, Taxonomy, and Assessment into a single Play application.

1. Make sure all containers from [Local Development Setup](#local-development-setup) are running.

2. Set the [cloud storage environment variables](#cloud-storage-configuration).

3. Build and run:

   **Linux:**
   ```shell
   cd knowlg-service
   mvn play2:run
   ```

   **macOS:**
   ```shell
   cd knowlg-service
   mvn play2:dist
   cd target
   tar xvzf knowlg-service-1.0-SNAPSHOT-dist.zip
   cd knowlg-service-1.0-SNAPSHOT
   ./start
   ```

4. Health check:
   ```shell
   curl http://localhost:9000/health
   ```

---

## Cloud Storage Configuration

Set these environment variables before running any service locally:

```shell
export cloud_storage_type=            # azure | aws | gcloud
export cloud_storage_auth_type=ACCESS_KEY

# Azure (default)
export cloud_storage_key=             # account name
export cloud_storage_secret=          # account key
export cloud_storage_container=       # container name

# AWS
export cloud_storage_key=             # access key ID
export cloud_storage_secret=          # secret access key
export cloud_storage_region=          # e.g. ap-south-1
export cloud_storage_container=       # S3 bucket name

# GCP
export cloud_storage_key=             # service account client email
export cloud_storage_secret=          # path to JSON key file
export cloud_storage_container=       # GCS bucket name
```

---

## CI/CD — GitHub Actions

The project uses **GitHub Actions** for CI/CD. Workflows are defined in `.github/workflows/` and triggered on tag push.

### Required variables (Settings > Secrets and variables > Actions)

| Variable | Description |
|----------|-------------|
| `REGISTRY_PROVIDER` | Registry type: `gcp`, `dockerhub`, `azure`, `aws`, or `ghcr` |
| `REGISTRY_URL` | Container registry URL |
| `CLOUD_STORE_GROUP_ID` | Cloud storage SDK group ID |
| `ARTIFACT_ID` | Cloud storage SDK artifact ID |
| `VERSION` | Cloud storage SDK version |

### Registry credentials

**GitHub Container Registry (GHCR)** — default, no setup needed. Uses the built-in `GITHUB_TOKEN`.

**DockerHub**

| Secret | Example |
|--------|---------|
| `REGISTRY_USERNAME` | `myusername` |
| `REGISTRY_PASSWORD` | DockerHub password or access token |
| `REGISTRY_NAME` | `docker.io` |

**Azure Container Registry**

| Secret | Example |
|--------|---------|
| `REGISTRY_USERNAME` | ACR username |
| `REGISTRY_PASSWORD` | ACR password |
| `REGISTRY_NAME` | `myregistry.azurecr.io` |

**GCP Artifact Registry**

| Secret | Example |
|--------|---------|
| `GCP_SERVICE_ACCOUNT_KEY` | Base64-encoded service account JSON key |
| `REGISTRY_NAME` | `asia-south1-docker.pkg.dev` |

**Amazon ECR**

| Secret | Example |
|--------|---------|
| `AWS_ACCESS_KEY_ID` | AWS access key ID |
| `AWS_SECRET_ACCESS_KEY` | AWS secret access key |
| `AWS_REGION` | `us-east-1` |
