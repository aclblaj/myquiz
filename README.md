# MyQuiz - Question Bank Management System

Import multiple choice and true/false questions from Excel files into PostgreSQL database and manage them through a web interface.

## Overview

MyQuiz is a microservices-based application for managing quiz questions, supporting Excel file imports, quiz generation, and Moodle XML export. The system features a Thymeleaf-based web interface with JWT authentication.

## Features

- 📊 **Excel Import**: Bulk import questions from Excel files (single or archive)
- 📝 **Quiz Management**: Create, edit, and organize quizzes by course
- 👤 **Author Tracking**: Track question authors and contributions
- 🔍 **Advanced Filtering**: Filter questions by course, author, quiz, type
- ⚠️ **Error Tracking**: Monitor and resolve import validation errors
- 🔐 **Authentication**: JWT-based user authentication
- 📄 **Moodle Export**: Export quizzes to Moodle XML format

## Technology Stack

- **Backend**: Java 21, Spring Boot 4.0.0, Maven 3.9.5
- **Frontend**: Thymeleaf, CSS, JavaScript
- **Database**: PostgreSQL 15
- **Containerization**: Docker, Docker Compose
- **Authentication**: JWT tokens

## Architecture

The solution follows a microservices architecture:

### Core Services:
- **myquiz-api**: Shared DTOs and API definitions (JAR)
- **myquiz-app**: Backend REST API (JAR, port 8082)
- **myquiz-thymeleaf**: Frontend web interface (JAR, port 8080)
- **myquiz-auth**: Authentication service (JAR, port 8090)
- **myquiz-iam**: User management service (JAR, port 8888)
- **postgres**: PostgreSQL database (container port 5432, exposed on 5433)

### Optional Services (Profiles):
- **adminer**: Database management UI (port 8083) - `dev` profile
- **ollama**: AI service for question generation (port 11434) - `ai` profile
- **nginx**: Reverse proxy (ports 80/443) - `production` profile

## Quick Start

### Prerequisites

- Java 21 (JAVA_HOME=C:\Software\Java\jdk-21)
- Maven 3.9.5 (MVN_HOME=C:\Software\Java\apache-maven-3.9.5)
- Docker and Docker Compose

### Option 1: Docker (Recommended)

```bash
# Build all modules
mvn clean install

# Build and start all services
docker-compose up -d

# Access the application
# Frontend: http://localhost:8080
# Backend API: http://localhost:8082/api
```

### Option 2: Local Development

1. **Setup Database**
```sql
CREATE DATABASE myquiz;
SET client_encoding = 'UTF8';
UPDATE pg_database SET datcollate='en_US.UTF-8', datctype='en_US.UTF-8' WHERE datname='postgres';
UPDATE pg_database SET encoding = pg_char_to_encoding('UTF8') WHERE datname = 'myquiz';
```

2. **Verify Database Encoding**
```sql
SELECT datname, pg_encoding_to_char(encoding) AS encoding 
FROM pg_database WHERE datname = 'myquiz';
```

3. **Configure Application**
Edit `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/myquiz
spring.datasource.username=myquiz_user
spring.datasource.password=myquiz_password
```

4. **Run Services**
```bash
# Start backend
cd myquiz-app
mvn spring-boot:run

# Start frontend (in another terminal)
cd myquiz-thymeleaf
mvn spring-boot:run
```

5. **Access Application**
- Frontend: http://localhost:8080
- Backend API: http://localhost:8082/api

## Excel File Format

The first sheet should contain multiple choice questions with this header:

| No | Title | Text | PR1 | Response 1 | PR2 | Response 2 | PR3 | Response 3 | PR4 | Response 4 |

The second sheet (optional) can contain true/false questions.

**Supported Formats**: .xlsx, .xls
**Archive Upload**: ZIP files containing multiple Excel files (one per author)

## Docker Commands

### Basic Commands

```powershell
# View logs
docker-compose logs -f myquiz-app

# Access database (note: container name is myquiz-postgres, exposed port is 5433)
docker exec -it myquiz-postgres psql -U myquiz_user -d myquiz

# Access database from host machine
psql -h localhost -p 5433 -U myquiz_user -d myquiz

# Access app container
docker exec -it myquiz-app bash

# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v

# Rebuild specific service
docker-compose build --no-cache myquiz-app
docker-compose up -d myquiz-app
```

### Docker Profiles

The project uses Docker Compose profiles for optional services:

```powershell
# Start with development tools (includes Adminer database UI)
docker-compose --profile dev up -d

# Start with AI services (includes Ollama for question generation)
docker-compose --profile ai up -d

# Start with production setup (includes Nginx reverse proxy)
docker-compose --profile production up -d

# Combine multiple profiles
docker-compose --profile dev --profile ai up -d
```

### Service Access

- **Frontend**: http://localhost:8080
- **Backend API**: http://localhost:8082/api
- **Adminer** (dev profile): http://localhost:8083
- **Database**: localhost:5433

### External Service Setup

#### Ollama (AI Integration) - Optional Profile

The application uses Ollama for AI-powered question generation and analysis. Ollama runs **on the host machine** (not in Docker) and the Docker services connect to it via `http://host.docker.internal:11434`.

**Prerequisites:**
- Install Ollama from https://ollama.ai

**Quick Setup (Windows):**
```powershell
# 1. Download and install Ollama
# 2. Pull the default model
ollama pull llama3

# 3. Verify installation
curl http://localhost:11434/api/tags

# 4. Start services with AI profile
docker compose --profile ai up -d

# 5. Verify integration
curl http://localhost:8082/api/ollama/status
```

**Available AI Features:**
- **Question Text Correction** - Corrects grammar while preserving meaning
- **Alternative Answer Generation** - Generates plausible incorrect answers
- **Question Improvement** - Suggests quality improvements
- **Health Check** - Verify Ollama connection status

**Environment Variables (Pre-configured):**
```yaml
OLLAMA_API_URL: http://host.docker.internal:11434
OLLAMA_DEFAULT_MODEL: llama3
OLLAMA_TIMEOUT_SECONDS: 120
```

**Model Options:**
- `llama3` (recommended) - Best balance of performance and quality
- `codellama` - Optimized for code and technical content
- `mistral` - Faster responses, good for simple tasks
- `tinyllama` - Smallest and fastest, lower quality

**Troubleshooting:**
- Cannot connect? → Verify `curl http://localhost:11434/api/tags` works
- Timeout issues? → Increase `OLLAMA_TIMEOUT_SECONDS` in docker-compose.yml
- Model not found? → Run `ollama pull <model-name>`
- See OLLAMA-SETUP.md in prompt/archive/ for detailed troubleshooting

## Database Initialization

The `data/` directory contains SQL scripts for database setup and maintenance.

**Documentation:** See `prompt/auth-sd.md` (Section 14) and `prompt/guidelines.md` (Section 16) for detailed information.

### 📁 Directory Structure

```
data/
├── init-database.sql              # 🚀 Initial database setup (run once)
├── data.json                      # 📄 JSON data file
├── test-data/                     # 🧪 Test and sample data
├── verification/                  # ✅ Verification scripts
└── archive/                       # 📚 Historical scripts and documentation
```

### 🚀 Initial Setup

#### **init-database.sql** (Run Once)

Complete initialization script for new deployments. Includes:
- Role and permission tables
- Default permissions (18 permissions)
- Default roles (ADMINISTRATOR, GUEST, TEACHER, CONTENT_MANAGER)
- Admin user creation (username: admin, password: admin)
- Role assignments
- Performance indexes

**When to Run:**
- ✅ First time database setup
- ✅ Clean database initialization
- ✅ After dropping and recreating database

**How to Run:**
```powershell
# Via Docker (note: external port is 5433, internal is 5432)
Get-Content data\init-database.sql | docker exec -i myquiz-postgres psql -U postgres -d myquiz

# Direct PostgreSQL (if running locally on default port)
psql -U postgres -d myquiz -f data/init-database.sql

# Direct PostgreSQL (if connecting to Docker container from host)
psql -h localhost -p 5433 -U postgres -d myquiz -f data/init-database.sql
```

**What It Does:**
1. Creates permission and role tables
2. Inserts 17 default permissions
3. Creates 4 default roles with appropriate permissions
4. Creates admin user (admin/admin)
5. Assigns ADMINISTRATOR role to admin user
6. Creates all necessary indexes

### 🔧 Maintenance

#### **maintenance-fix-sequences.sql**

Synchronizes PostgreSQL sequences with existing data. Fixes "duplicate key" errors.

**When to Run:**
- ✅ After importing data with explicit IDs
- ✅ After database restore from backup
- ✅ When encountering duplicate key violations
- ✅ After running test data scripts

**How to Run:**
```powershell
# Via Docker
Get-Content data\maintenance-fix-sequences.sql | docker exec -i myquiz-postgres psql -U postgres -d myquiz

# Direct PostgreSQL (if running locally)
psql -U postgres -d myquiz -f data/maintenance-fix-sequences.sql

# Direct PostgreSQL (if connecting to Docker from host)
psql -h localhost -p 5433 -U postgres -d myquiz -f data/maintenance-fix-sequences.sql
```

**Safety:** Safe to run multiple times, no data loss

### 📦 Database Dump

#### **myquiz clean dump sql**

Complete database dump with schema and sample data for quick restoration.

**When to Run:**
- ✅ Full database restore
- ✅ Setting up replica environments

### 🧪 Test Data

The `test-data/` directory contains sample data for development and testing:
- `insert-dummy-courses.sql` - Sample course data
- `insert-dummy-quizzes.sql` - Sample quiz data

**Note:** Run `maintenance-fix-sequences.sql` after inserting test data.

```powershell
# Via Docker
Get-Content data\test-data\insert-dummy-courses.sql | docker exec -i myquiz-postgres psql -U postgres -d myquiz
Get-Content data\test-data\insert-dummy-quizzes.sql | docker exec -i myquiz-postgres psql -U postgres -d myquiz
# Then fix sequences
Get-Content data\maintenance-fix-sequences.sql | docker exec -i myquiz-postgres psql -U postgres -d myquiz

# Direct PostgreSQL
psql -h localhost -p 5433 -U postgres -d myquiz -f data/test-data/insert-dummy-courses.sql
psql -h localhost -p 5433 -U postgres -d myquiz -f data/test-data/insert-dummy-quizzes.sql
psql -h localhost -p 5433 -U postgres -d myquiz -f data/maintenance-fix-sequences.sql
```

### ✅ Verification

The `verification/` directory contains scripts to verify system setup:
- `verify-role-permission-system.sql` - Verify role/permission configuration
- `verify-extended-statistics-permission.sql` - Verify statistics permission

```powershell
# Via Docker
Get-Content data\verification\verify-role-permission-system.sql | docker exec -i myquiz-postgres psql -U postgres -d myquiz

# Direct PostgreSQL
psql -h localhost -p 5433 -U postgres -d myquiz -f data/verification/verify-role-permission-system.sql
```

### 📚 Archive

The `archive/` directory contains:
- **Historical SQL scripts** - Superseded by consolidated `init-database.sql`
- **Historical documentation** - Consolidated into `prompt/` directory

See `data/archive/README.md` for details on archived files.

### 🎯 Quick Start Workflow

#### New Deployment:
```powershell
# 1. Initialize database
psql -h localhost -p 5433 -U postgres -d myquiz -f data/init-database.sql

# 2. (Optional) Add test data
psql -h localhost -p 5433 -U postgres -d myquiz -f data/test-data/insert-dummy-courses.sql
psql -h localhost -p 5433 -U postgres -d myquiz -f data/test-data/insert-dummy-quizzes.sql

# 3. Fix sequences after test data
psql -h localhost -p 5433 -U postgres -d myquiz -f data/maintenance-fix-sequences.sql

# 4. Verify setup
psql -h localhost -p 5433 -U postgres -d myquiz -f data/verification/verify-role-permission-system.sql
```

#### Default Login:
- **Username:** admin
- **Password:** admin
- **Access:** http://localhost:8080

### 🔍 Common Issues

**Issue:** "duplicate key value violates unique constraint"  
→ **Solution:** Run `maintenance-fix-sequences.sql`

**Issue:** Admin user can't login  
→ **Solution:** Verify admin user exists and has ADMINISTRATOR role
```sql
SELECT u.username, r.name FROM users u
JOIN user_roles ur ON u.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.role_id
WHERE u.username = 'admin';
```

**Issue:** Menu items not showing  
→ **Solution:** Check JWT contains permissions, verify role assignments

### 📝 Database Change Log

**Version 3.0 - January 27, 2026**
- ✅ Consolidated SQL scripts into `init-database.sql`
- ✅ Simplified structure: init, maintenance, test-data, verification, archive
- ✅ Removed redundant scripts (now in archive)
- ✅ Removed indexes already managed by JPA @Index annotations
- ✅ Created `maintenance-fix-sequences.sql` for ongoing maintenance

**Version 2.1 - January 27, 2026**
- ✅ Consolidated detailed documentation into prompt directory
- ✅ Moved detailed guides to `data/archive/`

**Version 2.0 - January 15, 2026**
- ✅ Added comprehensive role and permission management system

**Version 1.0 - Previous**
- Initial database schema and scripts

## API Documentation

When running, access Swagger UI:
- http://localhost:8082/swagger-ui.html

## Key Features Implementation

### Excel Import & Question Management
- **Duplication Detection**: Automatically detects duplicate questions during import based on title and answer sets
- **Question Error Tracking**: All import errors are captured and linked to questions for easy resolution
- **Cascade Deletion**: Deleting quizzes cascades to authors, questions, and related data
- **Pagination & Filtering**: All list views support filtering by course, author, quiz, and custom pagination
- **Quiz Statistics**: View detailed statistics for each quiz including question counts and author contributions

### Authentication & Authorization
- **JWT Tokens**: Token-based authentication with secure session management
- **Role-Based Access Control (RBAC)**: Users have roles (ADMINISTRATOR, TEACHER, CONTENT_MANAGER, GUEST) with specific permissions
- **Cookie-Based Sessions**: Stateless authentication with secure session tracking (no JSESSIONID in URLs)

### Data Management
- **Moodle XML Export**: Export quizzes to Moodle-compatible XML format
- **Author Tracking**: Track which authors contributed which questions
- **Course Organization**: Organize all content by course for easy management

## Important Implementation Notes

### Service Architecture Highlights
1. **QuestionDuplicationService**: Handles all duplicate detection and management, integrated into upload workflow
2. **QuestionErrorService**: Centralized error tracking for validation failures during import
3. **UploadService**: Orchestrates file upload, parsing, and error handling
4. **OllamaService**: AI-powered question generation and analysis (optional, profile: `ai`)
5. **DataCleanupService**: Maintenance and data integrity operations

### Best Practices Reference
For comprehensive coding standards, architecture guidelines, best practices, and development procedures, see **`guidelines.md`** in the root directory. This covers:
- Database, JPA, and transaction management
- Authentication & session handling
- UI/UX formatting standards  
- Code quality and refactoring guidelines
- Deployment and maintenance procedures

## Project Structure

```
myquiz/
├── myquiz-api/          # Shared DTOs and interfaces
├── myquiz-app/          # Backend REST API
├── myquiz-thymeleaf/    # Frontend web interface
├── myquiz-auth/         # Authentication service
├── myquiz-iam/          # User management service
├── data/                # Database initialization scripts
├── nginx/               # Nginx configuration (optional)
├── docker-compose.yml   # Docker orchestration
└── pom.xml              # Parent Maven configuration
```

## Contributing

1. Review `guidelines.md` for coding standards
2. Review relevant `*-sd.md` files for feature specifications
3. Follow the established architecture patterns
4. Use DTOs for all data transfer
5. Implement proper error handling and logging
6. Test thoroughly before committing

## Documentation

### Active Documentation (v1.0)
The MyQuiz project uses consolidated, authoritative documentation organized by scope:

- **`README.md`** (this file) - Project overview, features, quick start, and Docker setup
- **`guidelines.md`** - Comprehensive development guidelines, best practices, architecture standards (2500+ lines)
- **`prompt/*-sd.md`** (13 files) - Feature-specific software design documents
  - `core-sd.md` - Ecosystem & menu architecture
  - `auth-sd.md` - Authentication & authorization design
  - `upload-sd.md` - File upload workflows
  - `question-sd.md` - Question management  
  - `question-bank-sd.md` - Question bank operations
  - `duplicate-sd.md` - Duplicate detection design
  - And 7 more feature-specific docs...

### How to Use Documentation
1. **New to MyQuiz?** → Start with README.md (this file) for overview and quick start
2. **Implementing a feature?** → Check relevant `prompt/*-sd.md` file for architecture and flows
3. **Coding guidelines needed?** → See `guidelines.md` for best practices (DB, transactions, UI, code quality, deployment)
4. **Database operations?** → See `guidelines.md` Section 18 for database management and maintenance

### Database Documentation
- **`data/init-database.sql`** - Initial database setup script (run once per deployment)
- **`data/maintenance-fix-sequences.sql`** - Synchronize sequences after manual data imports
- **`guidelines.md` Section 18** - Complete database management guide

### Archived Documentation
Historical implementation reports, fix summaries, and delivery notes are preserved in `prompt/archive/` for reference.  
Current system design and implementation details are maintained in the files above.

Note: Always refer to the active documentation in root/prompt directories for current information.

## License

[Add license information]

## Support

For issues or questions, please create an issue in the repository.

