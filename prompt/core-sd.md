# Core Software Design - Ecosystem & Menu

## 1. Overview

This document describes the core architecture of the MyQuiz microservices ecosystem, including the menu system, module integration, and overall design patterns.

## 2. Microservices Ecosystem

### 2.1 Architecture Overview

The MyQuiz solution follows a microservices architecture with clear separation of concerns:

```
┌─────────────────┐
│  myquiz-thymeleaf│  ← Frontend (GUI)
│   Port: 8080     │
└────────┬─────────┘
         │ REST API
         ├─────────────────────┬─────────────────────┐
         ▼                     ▼                     ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   myquiz-app    │  │  myquiz-auth    │  │   myquiz-iam    │
│   Port: 8082    │  │  Port: 8083     │  │   Port: 8084    │
│   (Backend)     │  │  (Auth)         │  │   (User Mgmt)   │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                     │                     │
         └─────────────────────┴─────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   myquiz-db     │
                    │   PostgreSQL    │
                    │   Port: 5432    │
                    └─────────────────┘

         ┌─────────────────┐
         │   myquiz-api    │  ← Shared DTOs
         │   (JAR Library) │
         └─────────────────┘
```

### 2.2 Module Responsibilities

#### myquiz-api (JAR)
- **Type:** Library (not standalone)
- **Purpose:** Shared DTOs and API definitions
- **Contains:**
  - DTOs for all entities (Author, Question, Quiz, Course, etc.)
  - Filter DTOs (AuthorFilterDto, QuestionFilterDto, etc.) — all extend `BasePaginationDto`
  - `BasePaginationDto` — common base class with `page`, `pageSize`, `totalPages`, `totalElements` fields
  - OpenAPI interface definitions
  - No business logic
- **Used By:** All other modules
- **Dependencies:** None

#### myquiz-app (WAR)
- **Type:** Spring Boot backend service
- **Purpose:** Core business logic and REST API
- **Contains:**
  - REST Controllers (AuthorController, QuestionController, etc.)
  - Services (AuthorService, QuestionService, etc.)
  - Repositories (JPA)
  - Specifications (for filtering)
  - Entity models
- **Exposes:** REST API at /api/*
- **No GUI:** No templates, HTML, CSS, or client-side logic
- **Dependencies:** myquiz-api, myquiz-db

#### myquiz-thymeleaf (WAR)
- **Type:** Spring Boot frontend service
- **Purpose:** User interface and GUI logic
- **Contains:**
  - Thymeleaf controllers (ThyAuthorController, ThyQuestionController, etc.)
  - HTML templates
  - CSS styles (styles.css)
  - JavaScript (if any)
  - SessionService
- **Consumes:** REST API from myquiz-app
- **Dependencies:** myquiz-api, myquiz-app

#### myquiz-auth (JAR)
- **Type:** Spring Boot authentication service
- **Purpose:** User authentication and token management
- **Contains:**
  - Login/logout endpoints
  - JWT token generation
  - Password validation
- **Integrates:** Exclusively integrates with myquiz-iam for all user operations
- **No GUI:** Backend service only
- **Dependencies:** myquiz-iam (exclusive access)

#### myquiz-iam (JAR)
- **Type:** Spring Boot identity management service
- **Purpose:** User and role management
- **Contains:**
  - User CRUD operations
  - Role management
  - Permission policies
- **Database:** PostgreSQL (myquiz-db)
- **No GUI:** Backend service only
- **Used By:** myquiz-auth (exclusively)
- **Dependencies:** myquiz-db
- **Access Control:** No other service may access myquiz-iam directly

#### myquiz-db (PostgreSQL)
- **Type:** Database container
- **Purpose:** Persistent data storage
- **Contains:**
  - Business data (authors, questions, quizzes, courses)
  - User data (users, roles)
  - Error logs
- **Used By:** myquiz-app (business data), myquiz-iam (user data)

## 3. Communication Patterns

### 3.1 Frontend to Backend
**Pattern:** REST API over HTTP

**Example:**
```java
// ThyAuthorController (Frontend)
@GetMapping("/authors")
public String listAuthors(Model model) {
    String url = apiUrl + "/api/authors/filter";
    HttpEntity<AuthorFilterInputDto> request = sessionService.createAuthorizedRequest(filterDto);
    ResponseEntity<AuthorFilterDto> response = restTemplate.exchange(url, HttpMethod.POST, request, AuthorFilterDto.class);
    // Process response
}
```

### 3.2 Backend to Database
**Pattern:** JPA/Hibernate ORM

**Example:**
```java
// AuthorService (Backend)
@Transactional
public AuthorFilterDto filterAuthors(AuthorFilterInputDto input) {
    Specification<Author> spec = AuthorSpecification.withFilters(input);
    Page<Author> page = authorRepository.findAll(spec, pageable);
    // Map to DTO
}
```

### 3.3 Auth Flow
**Pattern:** JWT Token Authentication

**Flow:**
1. User logs in via myquiz-thymeleaf
2. Credentials sent to myquiz-auth
3. myquiz-auth validates with myquiz-iam
4. JWT token issued and stored in session
5. All subsequent requests include JWT token in Authorization header

## 4. Menu System

### 4.1 Main Navigation Menu

**Location:** `menu.html` (Thymeleaf fragment)

**Menu Structure:**
```
┌─────────────────────────────────────────────────┐
│ MyQuiz Application                       [User] │
├─────────────────────────────────────────────────┤
│ Home | Authors | Questions | Quizzes | Courses │
│ Upload | Errors | Logout                        │
└─────────────────────────────────────────────────┘
```

**Menu Items:**

| Item | Route | Description | Icon |
|------|-------|-------------|------|
| Home | `/` | Dashboard/home page | 🏠 |
| Authors | `/authors` | Author list and management | 👤 |
| Questions | `/questions` | Question list and management | ❓ |
| Quizzes | `/quizzes` | Quiz list and management | 📝 |
| Courses | `/courses` | Course list and management | 📚 |
| Upload | `/uploads` | File upload (Excel/Archive) | 📤 |
| Errors | `/errors` | Author error tracking | ⚠️ |
| Logout | `/logout` | User logout | 🚪 |

### 4.2 Menu Implementation

**Template Fragment:**
```html
<!-- menu.html -->
<nav th:fragment="menu" class="navbar">
    <div class="nav-brand">MyQuiz Application</div>
    <div class="nav-items">
        <a href="/" class="nav-link">🏠 Home</a>
        <a href="/authors" class="nav-link">👤 Authors</a>
        <a href="/questions" class="nav-link">❓ Questions</a>
        <a href="/quizzes" class="nav-link">📝 Quizzes</a>
        <a href="/courses" class="nav-link">📚 Courses</a>
        <a href="/uploads" class="nav-link">📤 Upload</a>
        <a href="/errors" class="nav-link">⚠️ Errors</a>
    </div>
    <div class="nav-user">
        <span th:text="${username}">User</span>
        <a href="/logout" class="nav-link">🚪 Logout</a>
    </div>
</nav>
```

**Usage in Templates:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>...</head>
<body>
    <div th:replace="~{menu :: menu}"></div>
    <div class="container mt-3">
        <!-- Page content -->
    </div>
</body>
</html>
```

### 4.3 Active Menu State
Highlight current page in menu:
```html
<a th:href="@{/authors}" 
   th:classappend="${#httpServletRequest.requestURI == '/authors'} ? 'active' : ''"
   class="nav-link">👤 Authors</a>
```

## 5. Controller Patterns

### 5.1 Thymeleaf Controller Pattern

**Structure:**
```java
@Controller
@RequestMapping("/resource")
public class ThyResourceController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private SessionService sessionService;
    
    @Value("${api.url}")
    private String apiUrl;
    
    @GetMapping
    public String list(Model model) {
        // 1. Validate session
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;
        
        // 2. Call backend API
        String url = apiUrl + "/api/resource";
        HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
        ResponseEntity<ResourceDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, ResourceDto.class);
        
        // 3. Add to model
        model.addAttribute("data", response.getBody());
        
        // 4. Return view
        return "resource-list";
    }
}
```

**Key Points:**
- Always validate session first
- Use SessionService for authorization
- Call backend via RestTemplate
- Add data to model
- Return view name

### 5.2 Backend Controller Pattern

**Structure:**
```java
@RestController
@RequestMapping("/api/resource")
public class ResourceController implements ResourceApi {
    
    @Autowired
    private ResourceService service;
    
    @PostMapping("/filter")
    public ResponseEntity<ResourceFilterDto> filter(@RequestBody ResourceFilterInputDto input) {
        try {
            ResourceFilterDto result = service.filterResources(input);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error filtering resources", e);
            return ResponseEntity.status(500).build();
        }
    }
}
```

**Key Points:**
- Implement API interface
- Minimal logic (delegate to service)
- Handle exceptions
- Return appropriate HTTP status
- Log errors

### 5.3 Service Pattern

**Structure:**
```java
@Service
@Transactional
public class ResourceService {
    
    @Autowired
    private ResourceRepository repository;
    
    public ResourceFilterDto filterResources(ResourceFilterInputDto input) {
        // 1. Build specification
        Specification<Resource> spec = ResourceSpecification.withFilters(input);
        
        // 2. Create pageable
        Pageable pageable = PageRequest.of(input.getPage(), input.getPageSize());
        
        // 3. Query database
        Page<Resource> page = repository.findAll(spec, pageable);
        
        // 4. Map to DTO
        List<ResourceDto> dtos = page.getContent().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
        
        // 5. Build filter DTO
        ResourceFilterDto result = new ResourceFilterDto();
        result.setResources(dtos);
        result.setTotalElements(page.getTotalElements());
        result.setTotalPages(page.getTotalPages());
        result.setCurrentPage(page.getNumber());
        
        return result;
    }
}
```

**Key Points:**
- Use Specification pattern for filtering
- Use pagination
- Map entities to DTOs
- Build complete filter DTO
- Handle transactions

### 5.4 SessionService Controller Refactoring

All Thymeleaf controllers have been refactored to use centralized SessionService methods for consistent session validation and authorization handling.

#### Controllers Refactored

| Controller | Methods Refactored | Lines Saved | Status |
|------------|-------------------|-------------|---------|
| ThyAuthorController | 12 | 42 | ✅ Complete |
| ThyQuizController | 13 | ~55 | ✅ Complete |
| ThyQuestionController | 9 | ~45 | ✅ Complete |
| ThyUploadController | 4 | ~35 | ✅ Complete |
| ThyAuthorErrorController | 4 | ~20 | ✅ Complete |
| ThyCourseController | 8 | ~40 | ✅ Complete |
| ThyHomeController | 1 | ~10 | ✅ Complete |
| ThyAuthController | 0 | 0 | ✅ Different pattern |
| **TOTAL** | **51** | **~247** | **✅ Complete** |

#### Refactoring Pattern Applied

**Before (Old Verbose Pattern):**
```java
@GetMapping("/path")
public String method(Model model, HttpSession session) {
    String jwtToken = (String) session.getAttribute(ControllerSettings.ATTR_JWT_TOKEN);
    if (jwtToken == null || jwtToken.isBlank()) {
        return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
    }
    HttpHeaders headers = new HttpHeaders();
    headers.set(ControllerSettings.HEADER_AUTHORIZATION, 
        ControllerSettings.BEARER_PREFIX + jwtToken);
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<SomeDto> entity = new HttpEntity<>(dto, headers);
    // ... rest of method
}
```

**After (New Clean Pattern):**
```java
@GetMapping("/path")
public String method(Model model) {
    String redirect = sessionService.validateSessionOrRedirect();
    if (redirect != null) return redirect;
    
    HttpEntity<SomeDto> entity = sessionService.createAuthorizedRequest(dto);
    // ... rest of method
}
```

#### Benefits Achieved

**✅ Consistency**
- All controllers use same SessionService patterns
- Predictable behavior across application
- Easy for new developers to understand

**✅ Maintainability**
- Authorization logic centralized in SessionService
- Changes to auth affect all controllers automatically
- Single source of truth

**✅ Code Quality**
- ~247 lines of boilerplate removed
- Reduced duplication
- Cleaner, more readable code

**✅ Error Handling**
- Consistent 403 handling with session invalidation
- Better logging throughout
- Proper exception handling

**✅ Security**
- Centralized token validation
- Consistent auth header creation
- Reduced chance of auth bypass bugs

#### Migration Pattern for Future Controllers

When adding new controllers or methods requiring authentication:

```java
// Step 1: Validate session
String redirect = sessionService.validateSessionOrRedirect();
if (redirect != null) return redirect;

// Step 2: Create authorized request
HttpEntity<DtoType> entity = sessionService.createAuthorizedRequest(dto);
// OR for GET requests:
HttpEntity<Void> entity = sessionService.createAuthorizedRequest();

// Step 3: Make API call with error handling
try {
    ResponseEntity<DtoType> response = restTemplate.exchange(url, method, entity, DtoType.class);
    // Process response
} catch (HttpClientErrorException.Forbidden ex) {
    log.error("403 Forbidden: Token expired");
    sessionService.invalidateCurrentSession();
    return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
} catch (Exception ex) {
    log.error("Error: {}", ex.getMessage(), ex);
    // Handle error
}
```

**See:** `auth-sd.md` for complete SessionService API documentation and usage patterns.

## 6. Data Flow Patterns

### 6.1 List/Filter Flow
```
User → Template → Thymeleaf Controller → Backend API → Service → Repository → Database
                                                                                    │
User ← Template ← Thymeleaf Controller ← Backend API ← Service ← Repository ←──────┘
```

### 6.2 Create/Update Flow
```
User → Form → Thymeleaf Controller → Backend API → Service → Repository → Database
                                                                              │
User ← Redirect ← Thymeleaf Controller ← Backend API ← Service ← Repository ←┘
```

### 6.3 Delete Flow
```
User → Button → Thymeleaf Controller → Backend API → Service → Repository → Database
                                                                               │
User ← Redirect ← Thymeleaf Controller ← Backend API ← Service ← Repository ←─┘
```

## 7. DTO Structure Patterns

### 7.1 Filter Input DTO
**Purpose:** Capture filter criteria from frontend

**Example:**
```java
public class ResourceFilterInputDto {
    private Integer page;
    private Integer pageSize;
    private String course;
    private Long resourceId;
    private String sortBy;
    // Getters and setters
}
```

### 7.2 Filter Output DTO
**Purpose:** Return filtered results with pagination metadata

**Example:**
```java
public class ResourceFilterDto {
    private List<ResourceDto> resources;
    private List<String> courses;
    private String selectedCourse;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    // Getters and setters
}
```

### 7.3 Entity DTO
**Purpose:** Transfer entity data

**Example:**
```java
public class ResourceDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime dateCreated;
    // Getters and setters
}
```

## 8. Specification Pattern

### 8.1 Purpose
- Type-safe query building
- Reusable filter logic
- Composable criteria
- Avoid SQL injection

### 8.2 Implementation
```java
public class ResourceSpecification {
    
    public static Specification<Resource> withFilters(ResourceFilterInputDto input) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (input.getCourse() != null) {
                predicates.add(cb.equal(root.get("course").get("name"), input.getCourse()));
            }
            
            if (input.getResourceId() != null) {
                predicates.add(cb.equal(root.get("id"), input.getResourceId()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

### 8.3 Usage
```java
Specification<Resource> spec = ResourceSpecification.withFilters(input);
Page<Resource> page = repository.findAll(spec, pageable);
```

## 9. Error Handling Strategy

### 9.1 Frontend Errors
- Session expired → Redirect to login
- Validation error → Show error message on form
- Backend error → Show user-friendly message

### 9.2 Backend Errors
- 400 Bad Request → Validation error
- 404 Not Found → Entity not found
- 500 Internal Server Error → Log and return generic message

### 9.3 Logging
- All errors logged with stack trace
- Session validation failures logged
- API calls logged (request/response)
- Database errors logged

## 10. Best Practices

### 10.1 Separation of Concerns
- **Frontend:** Only UI logic, no business logic
- **Backend:** Only business logic, no GUI
- **DTOs:** Only data transfer, no logic

### 10.2 Stateless Services
- Services should be thread-safe
- No instance variables for request data
- Use @Transactional for database operations

### 10.3 Pagination
- Always paginate large result sets
- Default page size: 10
- Allow user to change page size

### 10.4 Filtering
- Use Specification pattern
- Handle null/default values
- Return filter options in DTO

### 10.5 Security
- Always validate session
- Use JWT for authentication
- Sanitize user input
- Use prepared statements (JPA handles this)

### 10.6 Performance
- Use JOIN FETCH to avoid N+1 queries
- Index foreign keys
- Cache reference data (courses, templates)

## 11. Technology Stack

### 11.1 Backend
- **Framework:** Spring Boot 4.0.0
- **Language:** Java 21
- **ORM:** Hibernate/JPA
- **Database:** PostgreSQL 15
- **Build:** Maven 3.9.5
- **Security:** JWT tokens

### 11.2 Frontend
- **Template Engine:** Thymeleaf
- **CSS:** Custom (styles.css)
- **JavaScript:** Minimal (form validation)

### 11.3 Infrastructure
- **Containerization:** Docker
- **Orchestration:** Docker Compose
- **Proxy:** Nginx (optional)

## 12. Development Workflow

### 12.1 Adding New Feature
1. Define DTOs in myquiz-api
2. Create entity in myquiz-app
3. Create repository in myquiz-app
4. Create service in myquiz-app
5. Create controller in myquiz-app
6. Create Thymeleaf controller in myquiz-thymeleaf
7. Create template in myquiz-thymeleaf
8. Add menu item (if needed)
9. Test end-to-end

### 12.2 Code Organization
```
myquiz-app/
  src/main/java/com/unitbv/myquiz/app/
    controller/      # REST controllers
    service/         # Business logic
    repository/      # Data access
    entity/          # JPA entities
    specification/   # Query specifications
    config/          # Configuration classes

myquiz-thymeleaf/
  src/main/java/com/unitbv/myquiz/thy/
    controller/      # Thymeleaf controllers
    service/         # SessionService
  src/main/resources/
    templates/       # HTML templates
    static/
      css/           # Stylesheets
      js/            # JavaScript
```

## 13. Related Documentation

- See `auth-sd.md` for authentication details
- See `docker-sd.md` for deployment
- See `style-sd.md` for UI/UX standards
- See individual *-sd.md files for feature-specific details
- See `guidelines.md` for coding standards

---

**Status:** ✅ Production Ready

Core architecture is stable and follows microservices best practices.

## Author Operations

### Create / Update
- Authors create and update quizzes and questions through the Thymeleaf GUI in `myquiz-thymeleaf`, which forwards to `myquiz-app` APIs.
- Core architecture enforces that all author-facing modifications are funneled through the frontend service, never directly to backend microservices.

### View / List
- Authors access menus and navigation (Authors, Questions, Quizzes, Courses) defined by the core menu system, which routes to dedicated Thymeleaf controllers.
- Listing and detail views for authors, questions, and quizzes follow the patterns described in `author-sd.md`, `question-sd.md`, and `quiz-sd.md`.

### Delete / Archive
- Deletion operations initiated by authors (e.g., deleting quizzes or questions) are orchestrated by `myquiz-app` according to the cascade and integrity rules defined in this core design.

### Permissions & Roles
- Core design assumes authenticated authors (via `auth-sd.md`) for all protected menu entries.
- Role-based restrictions (e.g., admin-only features) are expressed at controller and service layer following this architecture.

---

## 12. Request Logging and Monitoring

### 12.1 Overview

The myquiz-thymeleaf module includes a comprehensive request logging interceptor for debugging and monitoring all HTTP requests.

**Implementation**: `RequestLoggingInterceptor.java` (233 lines)  
**Configuration**: `ThymeleafConfig.java` registers the interceptor  
**Status**: ✅ Production-ready

### 12.2 Architecture

```
HTTP Request
    ↓
RequestLoggingInterceptor.preHandle()
    ├── Generate Request ID
    ├── Log request details
    └── Store start time
    ↓
Controller Handler
    ↓
RequestLoggingInterceptor.postHandle()
    └── Log model attributes (DEBUG)
    ↓
View Rendering
    ↓
RequestLoggingInterceptor.afterCompletion()
    ├── Calculate execution time
    ├── Log response status
    ├── Warn if slow (>1s)
    └── Clean up thread-local storage
```

### 12.3 Features

#### 12.3.1 Request Tracking
- **Unique Request ID**: 8-character UUID for each request
- **X-Request-Id Header**: Added to all responses for client-side correlation
- **Thread-Local Storage**: Tracks request context throughout processing

#### 12.3.2 Performance Monitoring
- **Execution Time Tracking**: Measures request processing time in milliseconds
- **Slow Request Detection**: Warns when requests take >1000ms
- **Performance Metrics**: Easy to extract from logs for analysis

#### 12.3.3 Security
- **Sensitive Header Redaction**: Automatically redacts:
  - authorization
  - cookie / set-cookie
  - x-auth-token
  - x-csrf-token
- **Safe Logging**: No sensitive data exposed in logs
- **Client IP Detection**: Proxy-aware IP address detection (X-Forwarded-For, X-Real-IP, etc.)

#### 12.3.4 Comprehensive Logging
- **Request Details**: Method, URI, query string, remote IP, session ID, handler
- **Response Details**: Status code, execution time
- **Exception Tracking**: Captures and logs errors
- **Model Attributes**: DEBUG-level logging of Spring MVC model contents

### 12.4 Configuration

#### 12.4.1 Interceptor Registration

```java
@Configuration
public class ThymeleafConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error"
                );
    }
}
```

#### 12.4.2 Logging Levels

```properties
# INFO level - basic request logging (recommended for production)
logging.level.com.unitbv.myquiz.thy.interceptor.RequestLoggingInterceptor=INFO

# DEBUG level - detailed logging with headers and model attributes
logging.level.com.unitbv.myquiz.thy.interceptor.RequestLoggingInterceptor=DEBUG

# WARN level - only slow requests and errors
logging.level.com.unitbv.myquiz.thy.interceptor.RequestLoggingInterceptor=WARN
```

### 12.5 Log Format

#### 12.5.1 Incoming Request (INFO Level)
```
========== Incoming Request [a1b2c3d4] ==========
Timestamp: 2026-01-14 11:45:23.456
Method: GET
URI: /questions
Query String: page=1&pageSize=10&course=MATH
Remote Address: 192.168.1.100
Session ID: 5E8F9A2B3C4D5E6F
Handler: ThyQuestionController
==================================================
```

#### 12.5.2 Request Completed (INFO Level)
```
========== Request Completed [a1b2c3d4] ==========
Status: 200
Execution Time: 245 ms
==================================================
```

#### 12.5.3 Slow Request Warning (WARN Level)
```
========== Request Completed [i9j0k1l2] ==========
Status: 200
Execution Time: 1523 ms
⚠ WARNING: Slow request detected (>1s)
==================================================
```

#### 12.5.4 Request with Exception (ERROR Level)
```
========== Request Completed [m3n4o5p6] ==========
Status: 500
Execution Time: 89 ms
Exception: HttpClientErrorException - 404 Not Found
==================================================
```

#### 12.5.5 Request with Headers (DEBUG Level)
```
========== Incoming Request [e5f6g7h8] ==========
Timestamp: 2026-01-14 11:45:23.456
Method: POST
URI: /questions/123/correction/grammar
Remote Address: 192.168.1.100
Headers:
  accept: application/json
  content-type: application/json
  user-agent: Mozilla/5.0
  authorization: [REDACTED]
  cookie: [REDACTED]
Handler: ThyQuestionController
==================================================
```

### 12.6 Usage and Analysis

#### 12.6.1 Finding Requests

```powershell
# Find all requests to a specific endpoint
Select-String -Path logs/myquiz-thymeleaf.log -Pattern "URI: /questions"

# Find all slow requests
Select-String -Path logs/myquiz-thymeleaf.log -Pattern "WARNING: Slow request"

# Trace a specific request by ID
Select-String -Path logs/myquiz-thymeleaf.log -Pattern "a1b2c3d4"

# Find requests from specific IP
Select-String -Path logs/myquiz-thymeleaf.log -Pattern "Remote Address: 192.168.1.100"
```

#### 12.6.2 Performance Analysis

```powershell
# Count requests per endpoint
Get-Content logs/myquiz-thymeleaf.log | 
    Select-String "URI: " | 
    ForEach-Object { $_.Line -match "URI: (.+)"; $matches[1] } | 
    Group-Object | 
    Sort-Object Count -Descending | 
    Select-Object -First 10

# Calculate average execution time
$times = Get-Content logs/myquiz-thymeleaf.log | 
    Select-String "Execution Time: (\d+) ms" | 
    ForEach-Object { [int]($_.Matches.Groups[1].Value) }
$average = ($times | Measure-Object -Average).Average
Write-Host "Average Execution Time: $average ms"

# Count status codes
Get-Content logs/myquiz-thymeleaf.log | 
    Select-String "Status: (\d+)" | 
    ForEach-Object { $_.Matches.Groups[1].Value } | 
    Group-Object | 
    Sort-Object Count -Descending
```

### 12.7 Client IP Detection

The interceptor correctly detects client IP addresses even behind proxies by checking (in order):

1. X-Forwarded-For
2. X-Real-IP
3. Proxy-Client-IP
4. WL-Proxy-Client-IP
5. HTTP_X_FORWARDED_FOR
6. HTTP_X_FORWARDED
7. HTTP_X_CLUSTER_CLIENT_IP
8. HTTP_CLIENT_IP
9. HTTP_FORWARDED_FOR
10. HTTP_FORWARDED
11. HTTP_VIA
12. REMOTE_ADDR
13. request.getRemoteAddr() (fallback)

### 12.8 Performance Impact

- **INFO Level**: ~1-2ms per request (minimal)
- **DEBUG Level**: ~2-5ms per request (includes header iteration)
- **Thread-Local Storage**: Efficient memory usage with automatic cleanup
- **Async Logging**: Consider using async appenders for high-traffic production systems

### 12.9 Monitoring Recommendations

#### Development Environment
- Use **DEBUG** level for detailed troubleshooting
- Monitor all requests with headers
- Track model attributes passed to views

#### Staging Environment
- Use **INFO** level for general monitoring
- Track execution times and slow requests
- Verify production logging configuration

#### Production Environment
- Use **INFO** level for standard monitoring
- Consider **WARN** level for high-traffic systems
- Set up log rotation and archival
- Configure async appenders for performance

### 12.10 Integration with Application Logging

#### Request ID Propagation

```java
// In any controller method
@GetMapping("/questions/{id}")
public String getQuestion(@PathVariable Long id, 
                         HttpServletResponse response,
                         Model model) {
    String requestId = response.getHeader("X-Request-Id");
    log.info("Processing question {} with request ID {}", id, requestId);
    // ... rest of method
}
```

#### Correlation with External Services

When calling external services, pass the request ID:

```java
HttpHeaders headers = new HttpHeaders();
headers.set("X-Request-Id", requestId);
headers.set("X-Correlation-Id", requestId);
HttpEntity<RequestDto> entity = new HttpEntity<>(requestDto, headers);
```

### 12.11 Security Considerations

**Never Log**:
- Passwords (plain or hashed)
- Credit card numbers
- Social security numbers
- API keys or secrets
- Full authorization tokens (bearer tokens are redacted)

**Automatically Redacted**:
All headers containing these strings (case-insensitive):
- authorization
- cookie
- set-cookie
- x-auth-token
- x-csrf-token

**To Add Custom Sensitive Headers**:
```java
private static final String[] SENSITIVE_HEADERS = {
    "authorization", "cookie", "set-cookie", "x-auth-token", 
    "x-csrf-token", "your-custom-header"
};
```

### 12.12 Files

**Created**:
- `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/interceptor/RequestLoggingInterceptor.java` (233 lines)

**Modified**:
- `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/config/ThymeleafConfig.java`
  - Implements WebMvcConfigurer
  - Registers RequestLoggingInterceptor with path patterns

**Build Status**: ✅ SUCCESS

---

<!-- Remove explicit date annotations like 'Date: December 24, 2025' if present in narrative sections. -->
