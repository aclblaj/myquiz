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

- **myquiz-api**: Shared DTOs and API definitions (JAR)
- **myquiz-app**: Backend REST API (WAR, port 8082)
- **myquiz-thymeleaf**: Frontend web interface (WAR, port 8080)
- **myquiz-auth**: Authentication service (JAR, port 8090) - exclusively integrates with myquiz-iam
- **myquiz-iam**: User management service (JAR, port 8888) - used only by myquiz-auth
- **postgres**: PostgreSQL database (port 5433)

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
UPDATE pg_database SET encoding = pg_char_to_encoding('UTF8') WHERE datname = 'myQuiz';
```

2. **Verify Database Encoding**
```sql
SELECT datname, pg_encoding_to_char(encoding) AS encoding 
FROM pg_database WHERE datname = 'myQuiz';
```

3. **Configure Application**
Edit `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/myQuiz
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

```bash
# View logs
docker-compose logs -f myquiz-app

# Access database
docker exec -it myquiz-postgres psql -U myquiz_user -d myquiz

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

## Database Initialization

Place initialization scripts in `data/` folder:
- `init-admin-user.sql` - Create admin user
- `insert-dummy-courses.sql` - Sample courses
- `insert-dummy-quizzes.sql` - Sample quizzes

Scripts execute automatically on first container start.

## API Documentation

When running, access Swagger UI:
- http://localhost:8082/swagger-ui.html

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

## License

[Add license information]

## Support

For issues or questions, please create an issue in the repository.

---


