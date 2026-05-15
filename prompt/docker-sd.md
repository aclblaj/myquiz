# Docker Software Design

## 0. Author Operations

There are no direct author-facing operations in Docker. Authors interact only through the application UI; see author-related SD documents for details on author workflows.

## 1. Overview

This document describes the Docker setup and containerization strategy for the MyQuiz microservices ecosystem.

## 2. Solution Modules and Containers

### 2.1 myquiz-api
- **Type:** JAR (not standalone, no main class)
- **Purpose:** OpenAPI REST API definitions and DTOs
- **Build:** Maven package
- **Dependencies:** None
- **Used By:** All other modules

### 2.2 myquiz-app
- **Type:** WAR (standalone with main class)
- **Purpose:** Spring Boot backend implementing REST API
- **Port:** 8082
- **Base URL:** http://localhost:8082/api
- **Database:** PostgreSQL (myquiz-db)
- **Dependencies:** myquiz-db, myquiz-api
- **Features:** Quizzes, questions, authors, courses, user errors, pagination, filtering

### 2.3 myquiz-thymeleaf
- **Type:** WAR (standalone with main class)
- **Purpose:** Thymeleaf GUI consuming REST API
- **Port:** 8080
- **Base URL:** http://localhost:8080
- **Backend:** myquiz-app
- **Dependencies:** myquiz-app
- **Features:** All user interface templates and interactions

### 2.4 myquiz-auth
- **Type:** JAR (standalone with main class)
- **Purpose:** Authentication service
- **Port:** 8090
- **Base URL:** http://localhost:8090/api/auth
- **Dependencies:** myquiz-iam (exclusive access)
- **Features:** Login, registration, token issuance, password validation
- **Access Pattern:** Acts as the sole gateway to myquiz-iam

### 2.5 myquiz-iam
- **Type:** JAR (standalone with main class)
- **Purpose:** User management service
- **Port:** 8888
- **Base URL:** http://localhost:8888/api/users
- **Database:** PostgreSQL (postgres)
- **Dependencies:** postgres
- **Used By:** myquiz-auth (exclusively)
- **Access Control:** No other service may access myquiz-iam directly
- **Features:** Users, roles, permissions, policies

### 2.6 postgres
- **Type:** PostgreSQL 17 Alpine container
- **Port:** 5433 (host) → 5432 (container)
- **Database:** myquiz
- **User:** myquiz_user
- **Password:** myquiz_password
- **Volume:** postgres_data (persistent storage)
- **Used By:** myquiz-app (business data), myquiz-iam (user data)

### 2.7 nginx (Optional)
- **Type:** Nginx reverse proxy
- **Port:** 80
- **Purpose:** Route requests to appropriate services
- **Configuration:** nginx.conf

## 3. Environment Variables

### 3.1 Database Configuration
```
POSTGRES_USER=myquiz_user
POSTGRES_PASSWORD=myquiz_password
POSTGRES_DB=myquiz
SPRING_DATASOURCE_URL=jdbc:postgresql://myquiz-db:5432/myquiz
SPRING_DATASOURCE_USERNAME=myquiz_user
SPRING_DATASOURCE_PASSWORD=myquiz_password
```

### 3.2 Service URLs
```
MYQUIZ_API_BASE_URL=http://myquiz-app:8082/api
AUTH_API_URL=http://myquiz-auth:8090/api/auth
MYQUIZ_IAM_URL=http://myquiz-iam:8888/api
```

### 3.3 Application Ports
```
APP_PORT=8082         # myquiz-app
FRONTEND_PORT=8080    # myquiz-thymeleaf
AUTH_PORT=8090        # myquiz-auth
IAM_PORT=8888         # myquiz-iam
DB_PORT=5433          # postgres (host mapping)
```

## 4. Docker Compose Configuration

### 4.1 docker-compose.yml
```yaml
version: '3.8'

services:
  myquiz-db:
    image: postgres:15
    container_name: myquiz-postgres
    environment:
      POSTGRES_USER: myquiz_user
      POSTGRES_PASSWORD: myquiz_password
      POSTGRES_DB: myquiz
    ports:
      - "5432:5432"
    volumes:
      - db_data:/var/lib/postgresql/data
      - ./data:/docker-entrypoint-initdb.d
    networks:
      - myquiz-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U myquiz_user -d myquiz"]
      interval: 10s
      timeout: 5s
      retries: 5

  myquiz-app:
    build: ./myquiz-app
    container_name: myquiz-app
    image: myquiz-app:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://myquiz-db:5432/myquiz
      SPRING_DATASOURCE_USERNAME: myquiz_user
      SPRING_DATASOURCE_PASSWORD: myquiz_password
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
    ports:
      - "8082:8082"
    depends_on:
      myquiz-db:
        condition: service_healthy
    networks:
      - myquiz-network

  myquiz-iam:
    build: ./myquiz-iam
    container_name: myquiz-iam
    image: myquiz-iam:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://myquiz-db:5432/myquiz
      SPRING_DATASOURCE_USERNAME: myquiz_user
      SPRING_DATASOURCE_PASSWORD: myquiz_password
    ports:
      - "8084:8084"
    depends_on:
      myquiz-db:
        condition: service_healthy
    networks:
      - myquiz-network

  myquiz-auth:
    build: ./myquiz-auth
    container_name: myquiz-auth
    image: myquiz-auth:latest
    environment:
      IAM_URL: http://myquiz-iam:8084/api/users
    ports:
      - "8083:8083"
    depends_on:
      - myquiz-iam
    networks:
      - myquiz-network

  myquiz-thymeleaf:
    build: ./myquiz-thymeleaf
    container_name: myquiz-thymeleaf
    image: myquiz-thymeleaf:latest
    environment:
      API_URL: http://myquiz-app:8082/api
      AUTH_URL: http://myquiz-auth:8083/api/auth
    ports:
      - "8080:8080"
    depends_on:
      - myquiz-app
      - myquiz-auth
    networks:
      - myquiz-network

volumes:
  db_data:
    driver: local

networks:
  myquiz-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.18.0.0/16
```

## 5. Dockerfile Specifications

### 5.1 myquiz-app/Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/myquiz-app.war app.war
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.war"]
```

### 5.2 myquiz-thymeleaf/Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/myquiz-thymeleaf.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
```

### 5.3 myquiz-auth/Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/myquiz-auth.war app.war
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.war"]
```

### 5.4 myquiz-iam/Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/myquiz-iam.war app.war
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.war"]
```

## 6. Build and Run Instructions

### 6.1 Local Prerequisites
- Docker and Docker Compose installed
- Java 21 (JAVA_HOME=C:\Software\Java\jdk-21)
- Maven 3.9.5 (MVN_HOME=C:\Software\Java\apache-maven-3.9.5)

### 6.2 Build All Modules
```powershell
# Build from root directory
mvn clean install
```

### 6.3 Build Docker Images
```powershell
# Build all images
docker-compose build

# Build specific image
docker-compose build myquiz-app
```

### 6.4 Start All Services
```powershell
# Start all containers in detached mode
docker-compose up -d

# Start with logs
docker-compose up

# Start specific service
docker-compose up -d myquiz-app
```

### 6.5 Stop All Services
```powershell
# Stop all containers
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### 6.6 View Logs
```powershell
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f myquiz-app
```

## 7. Database Initialization

### 7.1 Init Scripts Location
```
data/
  init-admin-user.sql
```

### 7.2 Automatic Initialization
Scripts in `/data` folder are automatically executed when PostgreSQL container starts for the first time.

**Execution Order:**
1. Database creation
2. All .sql files in alphabetical order

### 7.3 Manual Database Access
```powershell
# Connect to PostgreSQL in Docker
docker exec -it myquiz-postgres psql -U myquiz_user -d myquiz

# Set encoding
SET client_encoding = 'UTF8';

# Check encoding
SELECT datname, pg_encoding_to_char(encoding) AS encoding 
FROM pg_database 
WHERE datname = 'myquiz';
```

### 7.4 Database Sequence Fix
After importing data, run sequence fix:
```sql
\i /docker-entrypoint-initdb.d/fix-all-sequences.sql
```

## 8. Container Management

### 8.1 Container Status
```powershell
# List running containers
docker-compose ps

# Check container health
docker ps --filter "name=myquiz"
```

### 8.2 Restart Services
```powershell
# Restart all
docker-compose restart

# Restart specific service
docker-compose restart myquiz-app
```

### 8.3 Shell Access
```powershell
# App container
docker exec -it myquiz-app bash

# Thymeleaf container
docker exec -it myquiz-thymeleaf bash

# Database container
docker exec -it myquiz-postgres bash
```

## 9. Networking

### 9.1 Network Configuration
- **Network Name:** myquiz-network
- **Driver:** bridge
- **Subnet:** 172.18.0.0/16

### 9.2 Service Discovery
Services can communicate using container names as hostnames:
- `myquiz-db:5432`
- `myquiz-app:8082`
- `myquiz-auth:8083`
- `myquiz-iam:8084`
- `myquiz-thymeleaf:8080`

### 9.3 External Access
From host machine:
- Frontend: http://localhost:8080
- Backend API: http://localhost:8082/api
- Auth API: http://localhost:8083/api/auth
- IAM API: http://localhost:8084/api/users
- Database: localhost:5432

## 10. Volume Management

### 10.1 Persistent Volumes
- **db_data:** PostgreSQL data files

### 10.2 Volume Commands
```powershell
# List volumes
docker volume ls

# Inspect volume
docker volume inspect myquiz_db_data

# Remove volume (WARNING: deletes all data)
docker volume rm myquiz_db_data
```

### 10.3 Backup Database
```powershell
# Backup
docker exec myquiz-postgres pg_dump -U myquiz_user myquiz > backup.sql

# Restore
docker exec -i myquiz-postgres psql -U myquiz_user myquiz < backup.sql
```

## 11. Troubleshooting

### 11.1 Container Won't Start
```powershell
# Check logs
docker-compose logs myquiz-app

# Check if port is in use
netstat -ano | findstr :8082

# Rebuild image
docker-compose build --no-cache myquiz-app
```

### 11.2 Database Connection Issues
```powershell
# Verify database is running
docker-compose ps myquiz-db

# Check database logs
docker-compose logs myquiz-db

# Test connection
docker exec myquiz-postgres pg_isready -U myquiz_user
```

### 11.3 Service Communication Issues
```powershell
# Check network
docker network inspect myquiz_myquiz-network

# Test connectivity
docker exec myquiz-app ping myquiz-db
```

### 11.4 Clean Restart
```powershell
# Stop all, remove containers and volumes, rebuild
docker-compose down -v
mvn clean install
docker-compose build --no-cache
docker-compose up -d
```

## 12. Production Considerations

### 12.1 Security Enhancements
- Use secrets management for passwords
- Enable SSL/TLS for all services
- Restrict network access with firewall rules
- Use non-root users in containers
- Scan images for vulnerabilities

### 12.2 Performance Optimization
- Set JVM heap size: `-Xmx2g -Xms1g`
- Use connection pooling for database
- Enable database query caching
- Configure Nginx for load balancing
- Use production-grade database backup

### 12.3 Monitoring and Logging
- Centralized logging (ELK stack)
- Health check endpoints
- Metrics collection (Prometheus)
- Alerting (Grafana)
- Container resource limits

### 12.4 High Availability
- Multiple container replicas
- Load balancer (Nginx/HAProxy)
- Database replication
- Automated failover
- Rolling updates

## 13. Related Documentation

- See `auth-sd.md` for authentication and session management
- See `core-sd.md` for microservices architecture
- See `guidelines.md` for development best practices
- See `README.md` for quick start guide

---

**Status:** ✅ Production Ready

Docker setup is fully configured and tested for development and production environments.

