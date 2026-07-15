# Docker Deployment Software Design
## 1. Overview
This document defines the current containerized deployment model for MyQuiz based on the root `docker-compose.yml` and per-module `Dockerfile` files.
## 2. Functional Scope
The Docker layer is operational infrastructure rather than end-user functionality. Its role is to run the ecosystem consistently for development and deployment-style environments.
## 3. Architecture
### 3.1 Current Compose Services
The active compose file defines these services:
- `postgres`
- `myquiz-app`
- `myquiz-thymeleaf`
- `myquiz-auth`
- `myquiz-iam`
- `myquiz-api`
- `adminer`
- `nginx`
### 3.2 Main Runtime Relationships
- `myquiz-thymeleaf` calls `myquiz-app` and `myquiz-auth`
- `myquiz-app` depends on `postgres` and `myquiz-auth`
- `myquiz-auth` depends on `myquiz-iam`
- `myquiz-iam` depends on `postgres`
- `nginx` is only enabled in the production profile
### 3.3 Compose Profiles
- `dev`
- `production`
## 4. Container Responsibilities
### 4.1 `postgres`
- persistent relational storage
- initialization scripts from `data/`
- host port mapping `5433:5432`
### 4.2 `myquiz-app`
- core REST API on `8082`
- business data access
- upload, duplicate, data-management, and AI-related backend logic
### 4.3 `myquiz-thymeleaf`
- server-rendered UI on `8080`
- depends on backend availability
### 4.4 `myquiz-auth`
- authentication service on `8090`
- gateway to IAM
### 4.5 `myquiz-iam`
- identity and authorization storage/service on `8888`
### 4.6 `adminer`
- dev-only database inspection UI
### 4.7 `nginx`
- optional reverse proxy for production-style traffic routing
## 5. Configuration Model
### 5.1 Important Environment Variables
Current compose usage includes environment variables for:
- datasource URL, username, password
- JWT secret shared across services
- backend/frontend/auth base URLs
- server ports
- Ollama connection settings
- Spring profile selection (`docker`)
### 5.2 Ollama Host Integration
Ollama is not started as a compose service.
Current integration uses:
- `OLLAMA_API_URL=http://host.docker.internal:11434`
## 6. Flows
### 6.1 Startup Flow
1. `postgres` becomes healthy.
2. `myquiz-iam` starts against postgres.
3. `myquiz-auth` waits for IAM.
4. `myquiz-app` waits for postgres and auth.
5. `myquiz-thymeleaf` starts after the app is available.
### 6.2 Data Initialization Flow
Database initialization is mounted from `data/init-database.sql`.
### 6.3 Health and Restart Behavior
Major services define healthchecks and are configured with `restart: unless-stopped` where appropriate.
## 7. Security and Operational Boundaries
- secrets are currently environment-driven in compose for local/dev usage
- service-to-service hostnames use the compose network
- IAM is not exposed as a UI-facing dependency from Thymeleaf
- Nginx configuration is externalized under `nginx/`
## 8. Responsibilities
### 8.1 Compose File
- wire service dependencies
- inject runtime configuration
- mount volumes and init scripts
- define profiles and healthchecks
### 8.2 Module Dockerfiles
- package and run each Spring Boot module appropriately
- keep runtime environments aligned with Java 21-based execution
## 9. Key Decisions
- keep a single compose file for the main local/deployment-style topology
- keep Ollama external to containers
- use service healthchecks for dependency ordering
- mount initialization SQL as read-only scripts into postgres startup
## 10. Implementation Notes
- Compose file:
  - `docker-compose.yml`
- Reverse proxy config:
  - `nginx/nginx.conf`
- Module Dockerfiles:
  - `myquiz-api/Dockerfile`
  - `myquiz-app/Dockerfile`
  - `myquiz-thymeleaf/Dockerfile`
  - `myquiz-auth/Dockerfile`
  - `myquiz-iam/Dockerfile`
Related docs:
- `prompt/core-sd.md`
- `prompt/ollama-sd.md`
