# Ollama AI Integration Software Design

## 1. Overview

MyQuiz integrates with Ollama AI services for intelligent question generation, validation, and text correction. Ollama runs on the host machine and is accessed by Docker containers via `host.docker.internal:11434`.

## 2. Setup and Configuration

### 2.1 Host Machine Setup

Ollama runs **outside of Docker** on the host machine:

1. **Install Ollama**: Download from https://ollama.ai
2. **Pull Models**: Run `ollama pull llama3` (or your preferred model)
3. **Verify Service**: Ollama typically runs automatically on port 11434
4. **Test Connection**: `curl http://localhost:11434/api/tags`

### 2.2 Docker Configuration

The Docker services are pre-configured to use the host's Ollama instance:

- **Connection URL**: `http://host.docker.internal:11434`
- **Default Model**: `llama3`
- **Timeout**: 120 seconds (myquiz-app), 60 seconds (default)

**Services Using Ollama:**
- `myquiz-app` (port 8082) - Backend AI operations
- `myquiz-thymeleaf` (port 8080) - Frontend AI features

**Environment Variables:**
```yaml
OLLAMA_API_URL: http://host.docker.internal:11434
OLLAMA_DEFAULT_MODEL: llama3
OLLAMA_TIMEOUT_SECONDS: 120
```

### 2.3 Why Not in Docker?

Ollama is run on the host machine instead of as a Docker service because:
1. **Resource Access**: Better GPU access and performance
2. **Model Persistence**: Models are large and persist across container restarts
3. **Flexibility**: Can be used by multiple projects/containers
4. **Development**: Easier to test and update models

## 3. Implemented Features

### 3.1 Question Text Correction

**Endpoint**: `POST /api/ollama/correct-question`

Corrects grammar and spelling in question text while preserving meaning.

**Implementation:**
- `OllamaService.correctQuestionText()` - Backend service method
- Uses Ollama API `/api/generate` endpoint
- Supports multiple languages (English, Romanian)

### 3.2 Alternative Answer Generation

**Endpoint**: `POST /api/ollama/generate-alternatives`

Generates alternative wrong answers for multiple choice questions.

**Implementation:**
- `OllamaService.generateAlternativeAnswers()` - Backend service method
- Generates plausible but incorrect answers
- Maintains difficulty level

### 3.3 Question Improvement

**Endpoint**: `POST /api/ollama/improve-question`

Suggests improvements to question clarity and quality.

**Implementation:**
- `OllamaService.improveQuestion()` - Backend service method
- Analyzes question structure
- Provides specific improvement suggestions

### 3.4 Health Check

**Endpoints**: 
- `GET /api/ollama/status` (myquiz-app)
- `GET /api/ollama/status` (myquiz-thymeleaf)

Checks if Ollama service is available and responsive.

**Implementation:**
- Tests connection to Ollama API
- Returns service status and availability

## 4. Architecture

### 4.1 Service Layer

**OllamaService** (`myquiz-app/src/main/java/.../services/OllamaService.java`)
- HTTP client for Ollama API communication
- Request/response DTO mapping
- Error handling and timeout management
- Connection testing

### 4.2 Controller Layer

**OllamaController** (`myquiz-app/src/main/java/.../controller/OllamaController.java`)
- REST endpoints for AI operations
- Request validation
- Response formatting
- Swagger documentation

**ThyOllamaApiController** (`myquiz-thymeleaf/src/main/java/.../controller/ThyOllamaApiController.java`)
- Frontend API endpoints
- Health checks for UI

### 4.3 Configuration

**Application Properties:**
```properties
# Default values (can be overridden by environment variables)
ollama.api.url=http://localhost:11434
ollama.default.model=llama3
ollama.timeout.seconds=60
```

**Docker Environment:**
```yaml
OLLAMA_API_URL: http://host.docker.internal:11434
OLLAMA_DEFAULT_MODEL: llama3
OLLAMA_TIMEOUT_SECONDS: 120
```

## 5. Usage Examples

### 5.1 Check Ollama Status

```bash
curl http://localhost:8082/api/ollama/status
```

### 5.2 Correct Question Text

```bash
curl -X POST http://localhost:8082/api/ollama/correct-question \
  -H "Content-Type: application/json" \
  -d '{"text": "What is capitol of France?", "language": "en"}'
```

### 5.3 Generate Alternative Answers

```bash
curl -X POST http://localhost:8082/api/ollama/generate-alternatives \
  -H "Content-Type: application/json" \
  -d '{"questionId": 1, "count": 3}'
```

## 6. Troubleshooting

### 6.1 Connection Issues

**Symptom**: "Cannot connect to Ollama API"

**Solutions:**
1. Verify Ollama is running: `curl http://localhost:11434/api/tags`
2. Check Windows Firewall isn't blocking port 11434
3. Ensure Docker Desktop is running and has network access
4. Test from inside container: `docker exec -it myquiz-app curl http://host.docker.internal:11434/api/tags`

### 6.2 Timeout Errors

**Symptom**: Requests timeout before completing

**Solutions:**
1. Increase `OLLAMA_TIMEOUT_SECONDS` in docker-compose.yml
2. Use a smaller/faster model
3. Check host machine resources (CPU, RAM)

### 6.3 Model Not Found

**Symptom**: "Model not found" error

**Solutions:**
1. Pull the model: `ollama pull llama3`
2. List available models: `ollama list`
3. Update `OLLAMA_DEFAULT_MODEL` to match an installed model

## 7. Future Enhancements

## 7. Future Enhancements

### 7.1 Question Generation
- AI-assisted question creation from topics
- Multiple choice option generation
- Difficulty level suggestion

### 7.2 Advanced Question Validation
- Duplicate detection using semantic analysis
- Quality assessment scoring
- Automated difficulty rating

### 7.3 Quiz Optimization
- Difficulty balancing across quizzes
- Topic coverage analysis
- Learning objective alignment
- Adaptive question selection

## 8. Author Operations

### Create / Update
- Authors do not directly configure or manage OLLAMA integration; any author-facing AI correction features are exposed via the UI and backed by this service.

### View / List
- From the author perspective, AI-generated suggestions and corrections appear within question editing flows; OLLAMA endpoints remain backend-only.

### Delete / Archive
- Authors may discard or override AI suggestions, but do not delete OLLAMA resources.

### Permissions & Roles
- Only authenticated users with appropriate roles can trigger AI-assisted operations; authorization is enforced at the application level, not in OLLAMA itself.

## 9. Implementation Status

**Status:** ✅ Partially Implemented

**Completed:**
- ✅ Ollama service integration (host.docker.internal)
- ✅ Question text correction
- ✅ Alternative answer generation
- ✅ Question improvement suggestions
- ✅ Health check endpoints

**Planned:**
- 🚧 Advanced semantic analysis
- 🚧 Automated question generation from topics
- 🚧 Quiz difficulty optimization
- 🚧 Learning objective alignment

---

**Last Updated:** April 7, 2026
