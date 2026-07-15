# Ollama Integration Software Design
## 1. Overview
This document defines the current AI integration with Ollama in MyQuiz.
Ollama is treated as an external dependency and is accessed by application services over HTTP, typically through a host-installed Ollama instance.
## 2. Functional Scope
### 2.1 Main Features
- backend prompt-based content generation through `/api/ollama/generate`
- backend question-improvement entry point through `/api/ollama/improve-questions`
- service-health visibility through backend and Thymeleaf-facing status endpoints
- UI-side compatibility status checks for frontend JavaScript or diagnostic display
### 2.2 Main Routes
| Operation | Layer | Route |
|---|---|---|
| Generate AI response | backend | `POST /api/ollama/generate` |
| Improve questions | backend | `POST /api/ollama/improve-questions` |
| Backend status | backend | `GET /api/ollama/status` |
| Frontend status | thymeleaf | `GET /api/ollama/status` |
| Frontend health alias | thymeleaf | `GET /api/ollama/health` |
## 3. Architecture
### 3.1 Components
- `OllamaController` in `myquiz-app`
- `OllamaService` in `myquiz-app`
- `ThyOllamaController` in `myquiz-thymeleaf`
### 3.2 Deployment Model
Current Docker-based deployment does not run an Ollama container.
The app is configured to connect to an external Ollama instance, typically through `http://host.docker.internal:11434`.
## 4. Data Model and Contracts
The main shared API contracts are:
- `OllamaRequestDto`
- `OllamaResponseDto`
Current backend implementation also accepts simple JSON map payloads for prompt-oriented generation endpoints.
## 5. Flows
### 5.1 Generate Response
1. Client calls `POST /api/ollama/generate`.
2. Backend validates prompt presence.
3. `OllamaService` calls external Ollama.
4. Controller returns generated text, model, and timestamp.
### 5.2 Improve Questions
1. Client sends question IDs to `POST /api/ollama/improve-questions`.
2. Backend currently returns an acknowledgement-style response.
3. This endpoint is the extension point for richer AI-assisted question improvement workflows.
### 5.3 Status Checks
- backend status returns service metadata and available-model information
- Thymeleaf status performs a direct HTTP check to the configured Ollama tags endpoint and returns a UI-oriented operational/unavailable payload
## 6. Security and Operational Constraints
- Ollama is an infrastructure dependency, not an authenticated user-facing domain service
- application error handling should not assume the AI service is always available
- timeouts must remain bounded so AI failures do not block normal application behavior excessively
## 7. UI, API, and Service Responsibilities
### 7.1 Backend Controller
- validate prompt-oriented inputs
- expose AI endpoints to the rest of the ecosystem
- translate service failures into stable HTTP responses
### 7.2 Backend Service
- own external Ollama communication
- isolate HTTP request/response handling from domain controllers
### 7.3 Thymeleaf Controller
- expose frontend-friendly status/health endpoints
- avoid duplicating backend generation logic
## 8. Validation and Error Handling
- missing prompt input is a bad request
- unavailable Ollama should map to `503`-class behavior for status/health routes
- unexpected backend AI failures should map to `500` with structured error metadata
## 9. Key Decisions
- keep Ollama external to compose-managed services
- keep generation logic in backend service layer, not in Thymeleaf
- provide separate UI-friendly health endpoints in Thymeleaf for frontend diagnostics
## 10. Implementation Notes
- Backend controller:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/controller/OllamaController.java`
- Backend service:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/services/OllamaService.java`
- Thymeleaf controller:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyOllamaController.java`
Related docs:
- `prompt/docker-sd.md`
- `prompt/question-sd.md`
