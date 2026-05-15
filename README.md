# MyQuiz - Question Bank Management System

Import multiple choice and true/false questions from Excel files into PostgreSQL database and manage them through a web interface.

## Overview

MyQuiz is a microservices-based application for managing banks with questions, supporting Excel file imports, quiz generation, and Moodle XML export. The system features a Thymeleaf-based web interface with JWT authentication.

## Features

- 📊 **Excel Import**: Bulk import questions from Excel files (single or archive)
- 📝 **Question banks Management**: Create, edit, and organize question banks by course
- 👤 **Author Tracking**: Track question authors and contributions
- 🔍 **Advanced Filtering**: Filter questions by course, author, quiz, type
- ⚠️ **Error Tracking**: Monitor and resolve import validation errors
- 🔐 **Authentication**: JWT-based user authentication
- 📄 **Moodle Export**: Export question banks to Moodle XML format

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
- **ai mode**: Enables AI integration settings (uses Ollama on host at port 11434) - `ai` profile
- **nginx**: Reverse proxy (ports 80/443) - `production` profile

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 15 (required for manual run)
- Docker and Docker Compose (required for Docker run)

> Note: Commands below are written for Windows PowerShell.

### Run Manually

1. **Initialize local database**

```powershell
psql -U postgres -c "CREATE DATABASE myquiz;"
psql -h localhost -p 5432 -U postgres -d myquiz -f data/init-database.sql
```

2. **Build all modules from project root**

```powershell
mvn clean install
```

3. **Run services in separate terminals**

```powershell
cd myquiz-auth
mvn spring-boot:run
```

```powershell
cd myquiz-iam
mvn spring-boot:run
```

```powershell
cd myquiz-app
mvn spring-boot:run
```

```powershell
cd myquiz-thymeleaf
mvn spring-boot:run
```

4. **Open the application**
- Frontend: http://localhost:8080
- Backend API: http://localhost:8082/api
- Swagger UI: http://localhost:8082/swagger-ui.html

Default login (from initialized DB):
- Username: `admin`
- Password: `admin`

For environment variable mappings and recommended startup order, see `guidelines.md` section `19.1`.

### Run with Docker

1. **Build project artifacts**

```powershell
mvn clean install
```

2. **Start containers (dev profile includes all core services)**

```powershell
docker-compose --profile dev up -d --build
```

3. **Open the application**
- Frontend: http://localhost:8080
- Backend API: http://localhost:8082/api
- Swagger UI: http://localhost:8082/swagger-ui.html
- Database (host): localhost:5433

4. **Stop services**

```powershell
docker-compose down
```

To remove volumes and re-run first-start DB initialization scripts:

```powershell
docker-compose down -v
```

### Optional Docker Profiles

```powershell
docker-compose --profile dev up -d
docker-compose --profile dev --profile ai up -d
docker-compose --profile production up -d
```

- `dev`: includes core app services and Adminer (http://localhost:8083)
- `ai`: enables AI integration settings (requires Ollama running on host)
- `production`: includes Nginx reverse proxy

## Excel File Format

The first sheet should contain multiple choice questions with this header:

| No | Title | Text | PR1 | Response 1 | PR2 | Response 2 | PR3 | Response 3 | PR4 | Response 4 |

The second sheet (optional) can contain true/false questions.

**Supported Formats**: .xlsx, .xls
**Archive Upload**: ZIP files containing multiple Excel files (one per author)

## Operations and Troubleshooting

Operational runbooks were moved from this README to `guidelines.md` to keep onboarding short.

- Manual runbook: `guidelines.md` section `19.1`
- Docker runbook: `guidelines.md` section `19.2`
- SQL scripts and verification flow: `guidelines.md` section `19.3`
- Ollama (optional) setup notes: `guidelines.md` section `19.4`
- Common operational issues: `guidelines.md` section `19.5`

## API Documentation

When running, access Swagger UI:
- http://localhost:8082/swagger-ui.html

## Implementation Notes

Detailed implementation behavior and architecture notes are documented in:

- `guidelines.md` (coding, architecture, deployment, and operations)
- `prompt/*-sd.md` (feature-level software design documents)

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
4. **Database or operations tasks?** → See `guidelines.md` Section 19 for runbooks and script usage

### Database Documentation
- **`data/init-database.sql`** - Initial database setup script (run once per deployment)
- **`data/verification/verify-role-permission-system.sql`** - Verify role/permission setup
- **`guidelines.md` Section 19** - Operational runbooks and script usage

### Archived Documentation
Historical implementation reports, fix summaries, and delivery notes are preserved in `prompt/archive/` for reference.  
Current system design and implementation details are maintained in the files above.

Note: Always refer to the active documentation in root/prompt directories for current information.

## License

[Add license information]

## Support

For issues or questions, please create an issue in the repository.

