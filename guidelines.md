# MyQuiz Application Development Guidelines

## Software Requirements

- **Java 21**
- **Spring Boot 4.0.0**
- Local setup:
  - `JAVA_HOME=C:\Software\Java\jdk-21`
  - `MVN_HOME=C:\Software\Java\apache-maven-3.9.5`

---

## Solution Architecture

The solution is a microservice ecosystem with clear separation of concerns:

### myquiz-api

- OpenAPI REST API definitions
- Data DTOs
- JAR, not standalone (no main class)
- Used by other modules
- **No GUI logic**

### myquiz-app

- Spring Boot backend implementing REST API
- Handles quizzes, questions, authors, courses, user errors
- Pagination, filtering, error handling, validation
- JWT authentication and authorization
- Controller, service, repository layers
- No GUI logic or templates
- WAR, standalone (main class)
- Exposes REST API for myquiz-thymeleaf
- Directly accesses PostgreSQL database for business data
- **Does NOT communicate with myquiz-iam** (authentication is JWT-based)
- [http://localhost:APP_PORT/api](http://localhost:APP_PORT/api)

### myquiz-thymeleaf

- The GUI for the solution (Thymeleaf templates)
- Web application consuming REST API from myquiz-app
- Delegates authentication to myquiz-auth service
- WAR, standalone (main class)
- [http://localhost:FRONTEND_PORT](http://localhost:FRONTEND_PORT)
- All GUI logic, templates, and user interactions are here

### myquiz-auth

- Spring Boot authentication service
- **Exclusively integrates with myquiz-iam for user management**
- Handles login, registration, token issuance, password validation
- Acts as the sole gateway to user data from myquiz-iam
- No GUI logic
- [http://localhost:AUTH_PORT/api/auth](http://localhost:AUTH_PORT/api/auth)

### myquiz-iam

- Spring Boot user management service
- Manages users, roles, permissions, policies
- Stores user data in PostgreSQL (separate concern from business data)
- **Used exclusively by myquiz-auth service**
- No direct access from other services
- No GUI logic
- [http://localhost:IAM_PORT/api/users](http://localhost:IAM_PORT/api/users)

### myquiz-db

- PostgreSQL database
- Stores all business and user data
- Docker volume for persistence
- [localhost:5432]

---

## Service Communication Architecture

The microservices follow a strict communication pattern:

```
┌──────────────────┐
│  myquiz-thymeleaf│
│   (Frontend)     │
└────────┬─────────┘
         │ REST API
         ↓
┌──────────────────┐      ┌──────────────────┐
│   myquiz-app     │      │  myquiz-auth     │
│   (Backend)      │      │  (Auth Service)  │
└────────┬─────────┘      └────────┬─────────┘
         │                          │
         │ JPA                      │ REST API (EXCLUSIVE)
         ↓                          ↓
┌──────────────────┐      ┌──────────────────┐
│  PostgreSQL DB   │      │   myquiz-iam     │
│ (Business Data)  │      │ (User Management)│
└──────────────────┘      └────────┬─────────┘
                                   │ JPA
                                   ↓
                          ┌──────────────────┐
                          │  PostgreSQL DB   │
                          │   (User Data)    │
                          └──────────────────┘
```

**Key Communication Rules:**
- **myquiz-thymeleaf** → communicates with **myquiz-app** for business logic and **myquiz-auth** for authentication
- **myquiz-app** → accesses PostgreSQL directly for business data, uses JWT for authorization (no IAM calls)
- **myquiz-auth** → **EXCLUSIVELY** communicates with **myquiz-iam** for user operations
- **myquiz-iam** → accesses PostgreSQL directly for user data
- **No other service may call myquiz-iam directly** - all user operations must go through myquiz-auth

### Module Folder Structure

```
myquiz/
├── docker-compose.yml          # Docker orchestration
├── pom.xml                     # Parent POM (multi-module)
├── guidelines.md               # This file
├── README.md                   # Project overview
│
├── data/                       # Database scripts and test data
│   ├── init-database.sql
│   └── test-data/
│
├── myquiz-api/                 # Shared API module (DTOs, interfaces)
│   ├── pom.xml
│   └── src/main/java/com/unitbv/myquiz/api/
│       ├── dto/                # Data Transfer Objects
│       ├── interfaces/         # REST API interfaces
│       ├── settings/           # ControllerSettings constants
│       └── types/              # Enums and types
│
├── myquiz-app/                 # Backend REST API service
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/unitbv/myquiz/app/
│       ├── controller/         # REST controllers
│       ├── entities/           # JPA entities
│       ├── repositories/       # Spring Data repositories
│       ├── services/           # Business logic services
│       └── web/                # Web exceptions, handlers
│
├── myquiz-thymeleaf/           # Frontend GUI service
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/unitbv/myquiz/thy/
│       │   ├── config/         # Spring configuration
│       │   ├── controller/     # Thymeleaf controllers (Thy*)
│       │   ├── interceptor/    # Request logging interceptor
│       │   └── services/       # SessionService, etc.
│       └── resources/
│           ├── static/css/     # styles.css
│           └── templates/      # Thymeleaf HTML templates
│
├── myquiz-auth/                # Authentication service
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/unitbv/myquiz/auth/
│       ├── config/             # Security, JWT configuration
│       ├── controller/         # Auth REST controllers
│       └── services/           # Authentication services
│
├── myquiz-iam/                 # Identity & Access Management
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/unitbv/myquiz/iam/
│       ├── controller/         # User management controllers
│       ├── entities/           # User, Role, Permission entities
│       ├── repositories/       # User data repositories
│       └── services/           # User management services
│
├── nginx/                      # Reverse proxy configuration
│   └── nginx.conf
│
└── prompt/                     # Design documents
    ├── *-sd.md                 # Software design specs
    └── archive/                # Historical documentation
```

---

## Core Functionalities

### Courses
- Add, edit, list courses

### Authors
- Add, update authors
- List authors (filter by course, null/default values)
- Use `AuthorFilterDto` for data
- Single filter DTO structure

### Questions

- Add, update questions (by type)
- List questions (filter by author, course, quiz, null/default values)
- Use `QuestionFilterDto` for data
- Single filter DTO structure

---

## GUI Implementation

- Thymeleaf GUI is implemented only in myquiz-thymeleaf
- All other modules are backend microservices (no GUI, HTML, CSS, or client-side logic)
- Frontend interacts with backend via REST API calls
- Microservice architecture ensures clear separation between frontend (Thymeleaf) and backend services

---

## Best Practices Summary

- Use DTOs for all data transfer between frontend and backend
- Implement pagination to limit result set size, and filtering using filter DTOs
- All filter DTOs must extend `BasePaginationDto` (defined in `myquiz-api`) for consistent pagination fields (`page`, `pageSize`, `totalPages`, `totalElements`)
- Backend modules must not contain any GUI logic
- All user interactions and templates are handled in myquiz-thymeleaf
- Use environment variables for database and service configuration
- Ensure stateless, thread-safe services in backend modules
- Do not run `git` commands during code-change tasks unless explicitly requested by the user
- Put @RequestMapping, @Operation, @Parameters, @ApiResponses on the interface methods
- Implement in a @RestController class; keep only Spring wiring/logic there
- Use JOIN FETCH to avoid N+1 queries
- If session expired, redirect to login
- If invalid parameters, return HTTP 400 with error message
- If database error, return HTTP 500 with generic message
- If empty results, return HTTP 200 with empty list (not an error)

---

## 1. Controller Layer Guidelines

### 1.1 Thymeleaf Controller Responsibilities
- **DO NOT** render templates directly if another controller already handles that view properly
- **DO** redirect to the appropriate controller instead of duplicating logic
- **ALWAYS** set required model attributes before rendering a view
- **ALWAYS** validate session and JWT token before proceeding with any operation
- **USE** consistent attribute names across controllers (refer to ControllerSettings constants)

### 1.2 Model Attribute Consistency
- If a template requires specific model attributes (e.g., `quizFilter`), **EVERY** controller method that returns that view must set those attributes
- Document required model attributes for each view in comments
- Use ControllerSettings constants for attribute names to prevent typos

### 1.3 Example: Home Controller Pattern
```java
// ❌ BAD: Rendering quiz-list directly with wrong attributes
@GetMapping("/")
public String home(Model model, HttpSession session) {
    // ... fetch data ...
    model.addAttribute("quizzes", quizzes);  // Wrong attribute!
    return "quiz-list";  // Template expects quizFilter, not quizzes
}

// ✅ GOOD: Redirect to proper controller
@GetMapping("/")
public String home(Model model, HttpSession session) {
    // ... validate session ...
    return "redirect:/quiz";  // Let QuizController handle it properly
}
```

## 2. Template Development Guidelines

### 2.1 Null Safety in Thymeleaf Templates
- **ALWAYS** check if objects are null before accessing their properties
- **USE** defensive programming: assume any model attribute could be null
- **PREFER** `th:if` conditions before accessing nested properties

### 2.2 Template Null Safety Patterns
```html
<!-- ❌ BAD: Direct property access -->
<option th:selected="${quizFilter.pageSize == 5}" value="5">5</option>

<!-- ✅ GOOD: Null-safe property access -->
<option th:selected="${quizFilter != null && quizFilter.pageSize == 5}" value="5">5</option>

<!-- ❌ BAD: Direct collection check -->
<tr th:if="${#lists.isEmpty(quizFilter.quizzes)}">

<!-- ✅ GOOD: Null-safe collection check -->
<tr th:if="${quizFilter == null || #lists.isEmpty(quizFilter.quizzes)}">

<!-- ✅ GOOD: Elvis operator for default values -->
<input th:value="${quizFilter != null ? quizFilter.page : 1}" />
```

### 2.3 Template Block Safety
- Even if a property is checked in a parent `th:if`, still add null checks in nested expressions
- This protects against future refactoring and provides redundant safety

## 3. Navigation and Redirect Guidelines

### 3.1 Post-Success Navigation
- After successful operations (upload, create, update), redirect to a meaningful page
- Options for post-upload navigation:
  1. Redirect to related list view (e.g., quiz list after quiz upload)
  2. Show success page with clear navigation options
  3. Use RedirectAttributes to pass success messages

### 3.2 Success Page Pattern
```html
<!-- Provide multiple navigation options -->
<a href="javascript:history.back()">⬅️ Go Back</a>
<a th:href="@{/quiz}">📝 View Quizzes</a>
<a th:href="@{/}">🏠 Home</a>
```

## 4. Error Handling Guidelines

### 4.1 Controller Error Handling
- **ALWAYS** set fallback model attributes in catch blocks
- **NEVER** let a view render without its required attributes
- **LOG** all errors with appropriate severity

### 4.2 Error Handling Pattern
```java
try {
    // ... business logic ...
    model.addAttribute("quizFilter", result);
    return "quiz-list";
} catch (Exception e) {
    log.error("Failed to fetch quizzes: {}", e.getMessage(), e);
    model.addAttribute("quizFilter", new QuizFilterDto());  // Fallback!
    model.addAttribute("errorMsg", "Could not load quizzes");
    return "quiz-list";  // Can still render safely
}
```

## 5. Session and Authentication Guidelines

### 5.1 JWT Token Validation
- Check both loggedInUser AND jwtToken
- Check for null AND blank/empty
- Redirect to login if either is missing

```java
Object loggedInUser = session.getAttribute(ControllerSettings.ATTR_LOGGED_IN_USER);
String jwtToken = (String) session.getAttribute(ControllerSettings.ATTR_JWT_TOKEN);
if (loggedInUser == null || jwtToken == null || jwtToken.isBlank()) {
    return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
}
```

### 5.2 Session Tracking Configuration
- **ALWAYS** use cookie-based session tracking
- **NEVER** allow JSESSIONID in URLs (prevents `/quiz/;jsessionid=...`)
- **USE** SessionConfig to enforce cookie-only tracking

**Configuration Pattern:**
```java
@Configuration
public class SessionConfig {
    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {
            servletContext.setSessionTrackingModes(
                Collections.singleton(SessionTrackingMode.COOKIE)
            );
        };
    }
}
```

**Benefits:**
- Cleaner URLs without session IDs
- Better security (session IDs not exposed in URLs)
- Prevents routing issues with Spring MVC
- Modern web application standard

### 5.3 Spring Bean Configuration
- **AVOID** duplicate bean definitions across configuration classes
- **USE** dedicated configuration classes for related beans (e.g., `RestClientConfig`, `SecurityConfig`)
- **KEEP** the main `@SpringBootApplication` class clean and focused on bootstrapping
- **DOCUMENT** the purpose of each configuration class

**Bad Practice:**
```java
// ❌ BAD: Bean defined in main application class
@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**Good Practice:**
```java
// ✅ GOOD: Clean main application class
@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

// ✅ GOOD: Dedicated configuration class
@Configuration
public class RestClientConfig {
    /**
     * RestTemplate bean for communication with myquiz-iam service.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**Benefits:**
- Prevents `BeanDefinitionOverrideException` (Spring Boot 4.0 disables overriding by default)
- Better organization and separation of concerns
- Easier to locate and maintain configuration
- Self-documenting through dedicated configuration classes

## 6. Clean Architecture Principles

### 6.1 Controller Separation of Concerns
- **Thymeleaf Controllers**: HTTP handling, session management, view rendering
- **REST Controllers**: API endpoints, input validation, HTTP status codes
- **Services**: Business logic, transaction management, orchestration
- **Domain Services**: Specific domain operations

### 6.2 Don't Repeat Yourself (DRY)
- If multiple controllers need the same data for a view, extract to a common method
- If a view is complex, create a dedicated controller for it
- Redirect to existing controllers instead of duplicating their logic

## 7. Testing After Changes

### 7.1 Manual Testing Checklist
- [ ] Navigate to the affected page directly
- [ ] Navigate via each possible link/button that leads to the page
- [ ] Test browser back button functionality
- [ ] Test with expired session
- [ ] Test with missing JWT token
- [ ] Test error scenarios

### 7.2 Template Testing
- [ ] Check all th:if conditions
- [ ] Verify null safety for all object property access
- [ ] Test with empty collections
- [ ] Test with null model attributes

### 7.3 Database Integration Test Rules
- Run database-dependent integration tests only when the Docker PostgreSQL service is up and reachable.
- Keep file-path-dependent tests configurable via system properties instead of hardcoded local paths.
- Prefer `@Transactional` for integration tests that should rollback changes automatically.
- Use explicit assumptions (`assumeTrue`) for optional external prerequisites (folders, archives, Excel files).
- Keep test credentials and connection parameters in test configuration (`src/test/resources/application.properties`) and avoid embedding secrets in test code.

### 7.4 Database Integration Test Commands (PowerShell)

```powershell
Set-Location "C:\work\cla22\myquiz"
docker-compose up postgres -d
docker ps | Select-String "myquiz-postgres"
```

```powershell
Set-Location "C:\work\cla22\myquiz\myquiz-app"
mvn test "-Dtest=QuestionServiceTest"
mvn test "-Dtest=QuestionServiceTest#testDatabaseConnectivity"
mvn test "-Dtest=QuestionServiceTest#getServerEncoding"
```

### 7.5 Troubleshooting Patterns for Integration Tests
- **Connection refused**: verify `docker-compose` PostgreSQL is running and the mapped port is exposed.
- **Authentication errors**: verify test datasource credentials match `docker-compose.yml` and test properties.
- **Missing test input files**: provide the configured folders/files or override via JVM system properties.
- **Encoding checks fail**: verify UTF-8 and locale settings in runtime environment.
- **Non-deterministic failures**: isolate with single-test runs and validate assumptions before parsing/upload flows.

## 8. Common Pitfalls to Avoid

### 8.1 Template Rendering Issues
❌ Returning a view name without setting required model attributes
❌ Accessing object properties without null checks
❌ Using wrong attribute names (not matching controller)
❌ Forgetting pagination controls rely on specific DTO structure

### 8.2 Controller Issues
❌ Duplicating view rendering logic across controllers
❌ Not validating session/JWT before operations
❌ Catching exceptions without setting fallback model attributes
❌ Using redirects when model attributes are needed

## 9. Documentation Requirements

### 9.1 Controller Method Documentation
Document each controller method with:
- Required session attributes (e.g., jwtToken, loggedInUser)
- Model attributes set by the method
- Possible return views/redirects
- Error handling behavior

### 9.2 Template Documentation
Document each template with:
- Required model attributes
- Optional model attributes
- Expected data types
- Dependencies on other templates/fragments

## 10. Code Review Checklist

When reviewing code changes:
- [ ] Are all model attributes set consistently?
- [ ] Are null checks present in templates?
- [ ] Is error handling comprehensive?
- [ ] Are redirects used appropriately?
- [ ] Is session validation present?
- [ ] Are constants used instead of hardcoded strings?
- [ ] Is logging appropriate and informative?
- [ ] Are unused imports/fields removed?

## 11. Specific Lessons Learned

### 11.1 Quiz List Template Issue (2025-12-13)
**Problem:** NullPointerException when accessing quizFilter.quizzes
**Root Cause:** Home controller rendered quiz-list without setting quizFilter
**Solution:** 
1. Home controller redirects to quiz controller instead
2. Quiz list template has defensive null checks
**Lesson:** Never bypass the proper controller for a complex view

### 11.2 Upload Success Navigation
**Problem:** Users clicking "Go Back" after upload caused issues
**Root Cause:** Browser history with cached pages
**Solution:** Success page provides multiple clear navigation options
**Lesson:** Don't rely solely on browser back button for navigation

### 11.3 Database Sequence Synchronization (2025-12-13)
**Problem:** DataIntegrityViolationException - duplicate key value violates unique constraint "quiz_pkey"
**Root Cause:** Entity used GenerationType.AUTO which created sequences not synchronized with manually inserted IDs
**Solution:** 
1. Changed all entities to use GenerationType.SEQUENCE with explicit sequence names
2. Created fix-all-sequences.sql to synchronize sequences with existing data
**Lesson:** Always use explicit SEQUENCE strategy in PostgreSQL, especially when manual data inserts are involved

### 11.4 JSESSIONID in URLs Issue (2025-12-19)
**Problem:** URLs contained session IDs like `/quiz/;jsessionid=9C0F822BB5964DE8EF6E347183173231`
**Root Cause:** Servlet container's default behavior appends JSESSIONID to URLs when cookies are disabled or on first request
**Solution:** 
1. Created SessionConfig with ServletContextInitializer
2. Configured servlet context to use cookie-only session tracking
**Lesson:** Explicitly configure session tracking mode to prevent URL pollution and routing issues

## 12. Database and JPA Best Practices

### 12.1 ID Generation Strategy

**Always use SEQUENCE for PostgreSQL entities:**
```java
@Entity
public class MyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "my_entity_gen")
    @SequenceGenerator(name = "my_entity_gen", sequenceName = "my_entity_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;
    
    // ... other fields
}
```

**Why SEQUENCE over AUTO:**
- ✅ Explicit sequence names make debugging easier
- ✅ Can synchronize sequences after manual data imports
- ✅ Predictable behavior across database reloads
- ✅ Works correctly with dummy/test data inserts
- ❌ AUTO may create hidden sequences with wrong values

**Why allocationSize = 1:**
- ✅ No gaps in ID sequences (better UX)
- ✅ Easier debugging and testing
- ✅ Consistent with user expectations
- ❌ Default is 50, creating 50-number gaps

### 12.2 Sequence Synchronization

**After manual data inserts, always synchronize sequences:**
```sql
-- Synchronize sequence with existing data
SELECT setval('entity_seq', COALESCE((SELECT MAX(id) FROM entity), 0) + 1, false);

-- Verify sequence value
SELECT currval('entity_seq') AS next_id, 
       (SELECT MAX(id) FROM entity) AS max_existing_id;
```

**When to synchronize:**
- After importing dummy/test data with explicit IDs
- After database migrations with data
- After manual data corrections
- When seeing duplicate key violations

### 12.3 Naming Conventions

**Sequence naming pattern:**
- Sequence name: `{table_name}_seq` (e.g., `quiz_seq`)
- Generator name: `{table_name}_gen` (e.g., `quiz_gen`)
- Column name: `id`

**Example:**
```java
@SequenceGenerator(
    name = "quiz_gen",           // Generator name used in @GeneratedValue
    sequenceName = "quiz_seq",   // Actual database sequence name
    allocationSize = 1           // Increment by 1 (no gaps)
)
```

### 12.4 Common Database Pitfalls

❌ **Using GenerationType.AUTO in production**
- Can cause sequence synchronization issues
- Hidden sequence names hard to debug

❌ **Not synchronizing sequences after data imports**
- Leads to duplicate key violations
- New records conflict with imported IDs

❌ **Using large allocationSize without understanding**
- Creates confusing ID gaps (1, 51, 101, etc.)
- Makes manual data correlation difficult

❌ **Mixing manual IDs with generated IDs**
- Always let JPA manage IDs for new records
- Only set IDs explicitly during imports/migrations

### 12.5 Entity Design Best Practices

**Always include these on entities:**
```java
@Entity
public class MyEntity {
    // ID with explicit SEQUENCE
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "my_entity_gen")
    @SequenceGenerator(name = "my_entity_gen", sequenceName = "my_entity_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;
    
    // Default constructor for JPA
    public MyEntity() {
        // Required by JPA
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
}
```

### 12.6 Transaction Management

**Use @Transactional for data modifications:**
```java
@Service
public class MyService {
    @Transactional
    public MyEntity createEntity(MyDto dto) {
        MyEntity entity = new MyEntity();
        // ... set properties ...
        return repository.save(entity);
    }
}
```

**Transaction best practices:**
- ✅ Keep transactions as short as possible
- ✅ Use @Transactional on service methods, not controllers
- ✅ Handle exceptions within transactional boundaries
- ✅ Let transactions rollback on unchecked exceptions
- ❌ Don't call external APIs within transactions
- ❌ Don't perform file operations within transactions

### 12.7 Database Migration Guidelines

**When adding new entities:**
1. Use explicit SEQUENCE strategy from the start
2. Let Hibernate auto-create sequence on first run
3. Document sequence name in entity comments

**When importing data:**
1. Create entity without data first (generates sequence)
2. Import data with explicit IDs
3. Run sequence synchronization script
4. Verify sequence value > max imported ID

**Migration script template:**
```sql
-- Create sequence if not exists
CREATE SEQUENCE IF NOT EXISTS entity_seq START WITH 1 INCREMENT BY 1;

-- Synchronize with existing data
SELECT setval('entity_seq', COALESCE((SELECT MAX(id) FROM entity), 0) + 1, false);

-- Verify
SELECT currval('entity_seq'), (SELECT MAX(id) FROM entity);
```

---

## 14. QuestionService Best Practices

### 14.1 Duplicate Detection & Management

**Automatic Integration During Upload:**
```java
// Duplication checking is automatically integrated into parseFileSheets()
// When questions are uploaded:
1. Questions parsed from Excel file
2. Questions saved to database
3. QuestionDuplicationService.checkDuplicatesInCourse() called
4. Duplicates detected against same course questions
5. QuestionDuplicate associations created
6. QuestionError records linked to questions
```

**Methods Available:**
- `getQuestionDuplicatesView(Long questionId)` - View all duplicates for a question
- `removeDuplicateQuestion(Long questionId)` - Delete a duplicate question
- `removeDuplicationLinks(Long questionId, List<Long> duplicateIds)` - Unlink specific duplicates
- `getQuestionsWithDuplicates(String course)` - List all questions with duplicates in course

### 14.2 Error Tracking & Resolution

**All import errors are tracked and linked to questions:**
```
Question → QuestionError → QuizError (detailed error info)
          ↓
       Multiple error reasons possible:
       - Duplicate title and answers
       - Missing answer options
       - Invalid weight values
       - Encoding issues
```

**Error Resolution Workflow:**
1. Import file → errors detected → questions created with error markers
2. User reviews questions with errors
3. User corrects question or marks duplicate as keep/remove
4. Error cleared from question

### 14.3 Cache Management

**QuestionService uses caching for performance:**
- `allQuestions` - Cache of all questions
- `questions` - Cache by ID
- `questionsByQuiz` - Cache by quiz ID
- `questionsByAuthor` - Cache by author
- `questionsByAuthorName` - Cache by author name

**Cache Invalidation:**
- Create/update/delete operations evict ALL question caches
- Future optimization: consider fine-grained invalidation by quiz/author

**Recommendation:** For large deployments, implement Redis cache for better distributed cache management.

### 14.4 String Normalization for Comparison

**QuestionDuplicationService normalizes text for accurate comparison:**
```java
private String normalize(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim().toLowerCase(Locale.ROOT);
}
```

**Normalization includes:**
- Trimming whitespace
- Converting to lowercase
- UTF-8 encoding standardization
- Special character handling

**This ensures:**
- "Is Java a Language?" matches "is java a language?"
- Leading/trailing spaces don't affect matching
- Unicode characters handled consistently

### 14.5 Service Dependencies

**QuestionService coordinates with multiple services:**
```
QuestionService
├── AuthorService (author lookups)
├── QuizService (quiz validation)
├── CourseService (course lookups)
├── QuestionErrorService (error tracking)
├── QuestionDuplicationService (duplicate detection)
├── QuestionWeightValidationService (weight validation)
├── ExcelParsingService (Excel file parsing)
├── CellConversionService (cell value conversion)
├── EncodingSevice (UTF-8 handling)
├── ExportService (Moodle XML export)
└── DataCleanupService (data maintenance)
```

**Best Practice:** Each service is independently testable; use dependency injection for easy mocking.

### 14.6 Common Issues & Solutions

**Issue: "Duplicate key value violates unique constraint 'question_pkey'"**
- **Cause**: Sequence not synchronized with manually inserted IDs
- **Solution**: Run `maintenance-fix-sequences.sql` after any manual data imports

**Issue: Questions not showing duplicates after import**
- **Cause**: Duplication check not triggered
- **Solution**: Verify `parseFileSheets()` completes successfully
- **Verification**: Check QuestionDuplicate table entries

**Issue: Performance degradation with large question sets**
- **Cause**: Caching disabled or ineffective
- **Solution**: Verify caching annotations present, consider Redis for distributed systems

---

## 15. Data Deduplication Patterns

### 15.1 Shared Utility Services

**QuestionDuplicationUtility provides shared utility methods:**
```java
@Service
public class QuestionDuplicationUtility {
    // Core utilities
    normalize(String value)
    buildTitleSet(List<Question> questions)
    buildAnswerSet(List<Question> questions)
    isAnswerDuplicate(Question, Set<String>)
    isTitleDuplicate(Question, Set<String>)
    hasAllAnswers(Question)
    // Error handling
    createQuizError(...)
    getDescriptionWithTitle(...)
    isDuplicateErrorDescription(String)
}
```

**Benefits:**
- Single source of truth for duplicate logic
- 67% reduction in duplication between services
- Better testability
- Consistent behavior across services

### 15.2 Service Coordination

**Multiple services coordinate for complete duplicate management:**
```
QuestionValidationService
├── High-level validation orchestration
├── Author question validation
└── Uses QuestionDuplicationUtility

QuestionDuplicationService
├── Advanced duplicate management
├── Duplicate linking & cleanup
├── Duplicate viewing & deletion
└── Uses QuestionDuplicationUtility
```

**Pattern:**
- Utility handles common logic
- Services handle business orchestration
- Clear separation of concerns

### 15.3 Error Linking Pattern

**Standard pattern for linking errors to questions:**
```java
// Create error with question context
QuizError error = questionDuplicationUtility.createQuizError(
    question,
    "Duplicate title and answers",
    questionDuplicate
);

// Link error to question through repository
quizErrorRepository.save(error);

// Question becomes associated with error automatically
// through database relationship
```

**Result:**
- Questions show all associated errors
- Errors can be filtered and reviewed
- Clear audit trail of what errors occurred during import

---

## 16. Deployment Checklist

### Pre-Deployment
- [ ] Verify all services compile successfully: `mvn clean install`
- [ ] Run integration tests: `mvn test`
- [ ] Check API documentation (Swagger UI)
- [ ] Verify JWT token configuration
- [ ] Verify role/permission assignments in database

### Database Initialization
- [ ] Run `data/init-database.sql` for new deployments
- [ ] Run `data/maintenance-fix-sequences.sql` after any manual data inserts
- [ ] Verify admin user created with ADMINISTRATOR role
- [ ] Verify role-permission mappings correct

### Docker Deployment
- [ ] Build all modules: `mvn clean install`
- [ ] Build Docker images: `docker-compose build --no-cache`
- [ ] Start services: `docker-compose up -d`
- [ ] Verify all services running: `docker-compose ps`
- [ ] Check logs for startup errors: `docker-compose logs -f`

### Post-Deployment Verification
- [ ] Frontend accessible: http://localhost:8080
- [ ] Backend API responsive: http://localhost:8082/api
- [ ] Login page functional
- [ ] Can create admin user
- [ ] Can upload question files
- [ ] Can view questions and quizzes

### Optional Services
- [ ] Dev profile (Adminer): `docker-compose --profile dev up`
- [ ] AI profile (Ollama): `docker-compose --profile ai up` (requires Ollama on host)
- [ ] Production profile (Nginx): `docker-compose --profile production up`

### Performance Monitoring
- [ ] Monitor CPU usage of Java services
- [ ] Monitor database query performance
- [ ] Monitor question cache effectiveness
- [ ] Consider Redis caching for large deployments

## 17. Role & Permission System Architecture

### 17.1 System Architecture Layers

The MyQuiz RBAC system spans five integrated layers:

```
┌────────────────────────────────────────────────┐
│    USER INTERFACE LAYER (myquiz-thymeleaf)     │
│    - Menu with dynamic filtering                │
│    - User/Role Management UI                    │
│    - Permission-based content visibility        │
└────────────────┬─────────────────────────────┘
                 │ HTTP Requests with JWT
┌────────────────▼─────────────────────────────┐
│    API LAYER (myquiz-app)                     │
│    - JwtFilter validates tokens               │
│    - SecurityConfig enforces permissions      │
│    - Protected REST Controllers                │
└────────────────┬─────────────────────────────┘
                 │ Auth Requests
┌────────────────▼─────────────────────────────┐
│    AUTHENTICATION LAYER (myquiz-auth)         │
│    - Login handler with credentials           │
│    - JWT token generation with claims         │
│    - Communicates with IAM for user data      │
└────────────────┬─────────────────────────────┘
                 │ IAM API Calls
┌────────────────▼─────────────────────────────┐
│    IDENTITY & ACCESS (myquiz-iam)             │
│    - User Service                              │
│    - Role Service                              │
│    - Permission Service                        │
└────────────────┬─────────────────────────────┘
                 │ JDBC
┌────────────────▼─────────────────────────────┐
│    DATABASE LAYER (PostgreSQL)                │
│    - users, roles, permissions                │
│    - user_roles, role_permissions              │
└────────────────────────────────────────────────┘
```

### 17.2 Database Schema

**Core Tables:**

```
users                    user_roles              roles
├─ user_id (PK)          ├─ user_id              ├─ role_id (PK)
├─ username              ├─ role_id              ├─ name
├─ email                 └─ assigned_at          ├─ description
├─ password                                      └─ created_at
├─ enabled
└─ created_at           role_permissions        permissions
                        ├─ role_id              ├─ permission_id (PK)
                        └─ permission_id        ├─ name
                                                ├─ description
                                                ├─ resource
                                                └─ action
```

### 17.3 Default Roles

**ADMINISTRATOR:**
- Permissions: ALL (15 permissions)
- Default User: admin
- Access: Full system access, can manage everything

**GUEST:**
- Permissions: READ_COURSE, READ_QUIZ, READ_QUESTION, READ_AUTHOR, READ_USER, READ_ROLE (6)
- Default for: New registrations
- Access: Read-only, cannot modify anything

**TEACHER:**
- Permissions: READ/MODIFY_COURSE, READ/MODIFY_QUIZ, READ/MODIFY_QUESTION, READ_AUTHOR (7)
- Assignment: Manual
- Access: Can create and manage educational content

**CONTENT_MANAGER:**
- Permissions: READ/MODIFY_AUTHOR, READ_COURSE, READ_QUIZ, READ_QUESTION, UPLOAD_FILES, AI_TOOLS (8)
- Assignment: Manual
- Access: Manage authors, upload files, use AI tools

### 17.4 User Login Flow

```
1. User submits credentials (username/password) via login form
2. Thymeleaf POSTs to /api/auth/login
3. Auth Service validates with IAM:
   - GET /api/users/find/{username} → User entity
   - GET /api/users/{id}/roles → User's assigned roles
   - GET /api/users/{id}/permissions → Merged permissions
4. Generate JWT with claims:
   {
     "sub": "username",
     "roles": ["ADMINISTRATOR"],
     "permissions": ["READ_COURSE", "MODIFY_COURSE", ...]
   }
5. Return JWT → Store in session
6. Redirect to home → Menu filtered by permissions
```

### 17.5 API Request Security Flow

```
1. User Action (e.g., Edit Course)
2. Thymeleaf makes request with JWT in Authorization header
3. JwtFilter intercepts:
   - Extract JWT from Authorization header
   - Validate JWT signature & expiration
   - Extract username and permissions from token
   - Create Authentication with permissions as authorities
4. SecurityConfig validates:
   - Endpoint requires: MODIFY_COURSE
   - User has permissions: [READ_COURSE, MODIFY_COURSE, ...]
   - ✅ Has permission → Continue to controller
   - ❌ No permission → Return 403 Forbidden
5. Controller processes request and returns response
```

### 17.6 Permission Hierarchy

**Resources and Operations:**

```
COURSE
├─ READ_COURSE → List, View details
└─ MODIFY_COURSE → Create, Edit, Delete

QUIZ
├─ READ_QUIZ → List, View details
└─ MODIFY_QUIZ → Create, Edit, Delete

QUESTION
├─ READ_QUESTION → List, View details
└─ MODIFY_QUESTION → Create, Edit, Delete

AUTHOR
├─ READ_AUTHOR → List, View details
└─ MODIFY_AUTHOR → Create, Edit, Delete

USER
├─ READ_USER → List, View details
└─ MODIFY_USER → Create, Edit, Delete, Assign roles

ROLE
├─ READ_ROLE → List, View details
└─ MODIFY_ROLE → Create, Edit, Delete, Assign permissions

AI
└─ AI_TOOLS → Access AI correction features

UPLOAD
└─ UPLOAD_FILES → Upload Excel, Archives
```

### 17.7 Menu Filtering Example

**ADMINISTRATOR sees:**
- 📝 Quizzes (List, Add New)
- 👥 Authors (List, Add New)
- 📚 Courses (List, Add New)
- 📤 Upload Files
- 🤖 AI Tools
- 🔧 Questions (List, Add MC, Add TF)
- ⚙️ Administration (Users, Roles, Permissions)
- ℹ️ Help

**GUEST sees (read-only):**
- 📝 Quizzes (List only)
- 👥 Authors (List only)
- 📚 Courses (List only)
- 🔧 Questions (List only)
- ℹ️ Help

### 17.8 Implementation Checklist

**Phase 1: Database & IAM Layer**
- [ ] Run database migration scripts
- [ ] Create Permission and Role entities
- [ ] Update User entity with roles
- [ ] Create PermissionRepository and RoleRepository
- [ ] Create PermissionService and RoleService
- [ ] Implement IAM controllers

**Phase 2: Auth Service Enhancement**
- [ ] Enhance JwtUtil to include roles/permissions in tokens
- [ ] Update AuthService login method
- [ ] Create UserManagementController
- [ ] Create RoleManagementController

**Phase 3: API Security**
- [ ] Update JwtFilter to extract permissions
- [ ] Update SecurityConfig permission rules
- [ ] Add 403 error handler
- [ ] Test endpoint protection

**Phase 4: Thymeleaf UI**
- [ ] Create ThyMenuController with permission filtering
- [ ] Update fragments.html with conditional menu items
- [ ] Create user/role management templates
- [ ] Create 403 error page

**Phase 5: Integration Testing**
- [ ] Test admin user (all permissions)
- [ ] Test guest user (read-only)
- [ ] Test custom role assignments
- [ ] Verify menu filtering by role
- [ ] Test API 403 responses

### 17.9 Quick Verification Commands

**Check admin user roles and permissions:**
```sql
SELECT u.username, r.name as role, COUNT(p.permission_id) as permissions
FROM users u
JOIN user_roles ur ON u.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.role_id
JOIN role_permissions rp ON r.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE u.username = 'admin'
GROUP BY u.username, r.name;
```

**Test login and JWT:**
```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

**Decode JWT claims (online tool or jq):**
```bash
# Copy JWT from response, decode at jwt.io or with:
echo $JWT_TOKEN | cut -d. -f2 | base64 -d | jq .
```

---

---

## 13. UI/UX Formatting Standards (All Modules)

### 13.1 Container and Layout Standards

#### Standard Container
**All templates MUST use the standard `.container` class** (except login/register forms):

```html
<div th:insert="~{fragments::menu}"></div>
<div class="container mt-3">
    <!-- Page content -->
</div>
```

**Container Properties:**
- Max-width: 99%
- Auto margins for centering
- Standard padding: 1rem
- Consistent across all list, detail, and editor views

#### Spacing Utilities
Use standard spacing classes consistently:
- `.mt-1` to `.mt-4`: Margin top (0.25rem to 1rem)
- `.mb-1` to `.mb-4`: Margin bottom
- `.p-1` to `.p-4`: Padding
- `.mt-3` is the standard top margin for container content

### 13.2 Table Formatting Standards

#### Standard Table Structure
**All data tables MUST use `.styled-table` class:**

```html
<table class="styled-table">
    <thead>
    <tr>
        <th>Column 1</th>
        <th>Column 2</th>
        <th>Actions</th>
    </tr>
    </thead>
    <tbody>
    <tr th:each="item : ${items}">
        <td th:text="${item.field1}">Data</td>
        <td th:text="${item.field2}">Data</td>
        <td class="actions">
            <a th:href="@{'/path/' + ${item.id}}" class="btn btn-info">View</a>
        </td>
    </tr>
    <tr th:if="${#lists.isEmpty(items)}">
        <td colspan="3">No items found</td>
    </tr>
    </tbody>
</table>
```

**Table Properties:**
- Width: 100% (automatic - defined in CSS)
- Border-collapse: collapse
- Background: White with alternating row colors
- Header: Blue background (#1e3a8a), white text
- Even rows: Light blue background (#eff6ff)
- Hover: Slightly darker blue (#dbeafe)
- Consistent padding: 12px 15px

**Table Rules:**
- ✅ DO use `.styled-table` for all data tables
- ✅ DO include empty state row with appropriate colspan
- ✅ DO use `.actions` class for action column cells
- ❌ DON'T add inline `style="width:100%"` (already in CSS)
- ❌ DON'T create custom table classes per template
- ❌ DON'T use different table styling per page

### 13.3 Form Formatting Standards

#### Standard Form Container
**Regular forms use `.form-container` class:**

```html
<div class="form-container">
    <h3>Form Title</h3>
    <form th:action="@{/path}" method="post">
        <div class="form-group">
            <label class="form-label">Field Label</label>
            <input type="text" name="field" class="form-input"/>
        </div>
        <div class="form-actions">
            <button type="submit" class="btn-save">Save</button>
            <a th:href="@{/cancel-path}" class="btn-cancel">Cancel</a>
        </div>
    </form>
</div>
```

**Exception: Login/Register forms have unique styling** (not covered by this standard)

### 13.4 Button Color Standards (Critical)

**Buttons MUST use consistent colors based on action type:**

#### View/Info Actions (Blue)
```html
<a class="btn btn-info">👁️ View</a>
<a class="btn btn-info">Details</a>
```
- Background: Light peach (#fff4e6)
- Text: Dark brown (#7c4a18)
- Border: Peach (#ffd6a5)
- Use for: View, Details, Info, Show

#### Edit Actions (Peach)
```html
<a class="btn btn-edit">✏️ Edit</a>
```
- Background: Light peach (#fff4e6)
- Text: Dark brown (#7c4a18)
- Border: Peach (#ffd6a5)
- Use for: Edit, Update, Modify

#### Delete Actions (Red - via btn-outline or custom)
```html
<button type="submit" class="btn btn-outline">🗑️ Delete</button>
```
- Use for: Delete, Remove

#### Primary Actions (Blue)
```html
<button type="submit" class="btn btn-primary">Submit</button>
```
- Background: Blue (#2563eb)
- Text: White
- Use for: Submit, Create, Confirm

#### Secondary Actions (Gray)
```html
<a class="btn btn-secondary">Action</a>
```
- Background: Gray (#6b7280)
- Text: White
- Use for: Secondary actions

#### Cancel/Back Actions (Peach Outline)
```html
<a class="btn btn-cancel">&larr; Back</a>
<a class="btn btn-cancel">Cancel</a>
```
- Background: Light peach (#fff8f0)
- Text: Dark brown (#7c4a18)
- Border: Peach (#ffd6a5)
- Use for: Cancel, Back, Return

#### Save Actions (Peach Gradient)
```html
<button type="submit" class="btn-save">💾 Save</button>
```
- Background: Peach gradient
- Text: Dark brown (#7c4a18)
- Border: Orange (#ff9800)
- Use for: Save, Submit forms

#### Filter Actions (Green Gradient)
```html
<button type="submit" class="btn-filter">🔍 Filter</button>
```
- Background: Green gradient (#10b981 to #059669)
- Text: White
- Use for: Filter, Search

**Button Consistency Rules:**
- ✅ SAME action = SAME class across ALL templates
- ✅ View/Details = `.btn btn-info` (everywhere)
- ✅ Edit = `.btn btn-edit` (everywhere)
- ✅ Delete = `.btn btn-outline` or custom delete class (everywhere)
- ✅ Save = `.btn-save` (everywhere)
- ✅ Cancel/Back = `.btn btn-cancel` (everywhere)
- ✅ Filter/Search = `.btn-filter` (everywhere)
- ❌ DON'T use different button classes for same action
- ❌ DON'T create template-specific button styles

### 13.5 Card/Section Formatting

#### Standard Card Component
```html
<div class="card">
    <div class="card-header">Section Title</div>
    <!-- Card content -->
</div>
```

**Card Properties:**
- Background: White (#fff)
- Border: Light gray (#e5e7eb)
- Border-radius: 0.75rem
- Padding: 1rem
- Box-shadow: Subtle shadow

**Use cards for:**
- Grouped information sections
- Forms
- Detail views
- Contained content areas

### 13.6 Alert/Message Formatting

#### Standard Alert
```html
<div class="alert">Message text</div>
```

**Alert Properties:**
- Background: Light yellow (#fef3c7)
- Text: Dark yellow (#92400e)
- Border: Yellow (#fde68a)
- Use for: General messages, warnings

#### Badge Formatting
```html
<span class="badge badge-success">Active</span>
<span class="badge badge-warn">Pending</span>
<span class="badge badge-error">Error</span>
```

**Badge Types:**
- `.badge-success`: Green - for success states
- `.badge-warn`: Yellow - for warnings
- `.badge-error`: Red - for errors

### 13.7 Filter Section Formatting

#### Standard Search Filter
**All list views MUST use consistent filter section:**

```html
<div class="searchfilter">
    <form method="post" th:action="@{/path/filter}" class="searchfilter-form">
        <div>
            <label for="filter1" class="searchfilter-label">Filter 1</label>
            <select id="filter1" name="filter1" class="searchfilter-select">
                <option value="">All</option>
                <!-- Options -->
            </select>
        </div>
        <button type="submit" class="btn-filter">🔍 Filter</button>
    </form>
</div>
```

**Filter Section Properties:**
- Background: Purple gradient (#667eea to #764ba2)
- Border-radius: 16px
- Padding: 1.5rem
- White labels with text-shadow
- Box-shadow for depth

**Filter Consistency Rules:**
- ✅ USE `.searchfilter` container
- ✅ USE `.searchfilter-form` for form
- ✅ USE `.searchfilter-label` for labels
- ✅ USE `.searchfilter-select` for dropdowns and inputs
- ✅ USE `.btn-filter` for submit button
- ❌ DON'T create custom filter styles per template

### 13.8 CSS Class Naming Convention

#### General Principles
- Use **semantic names** that describe purpose, not appearance
- Use **kebab-case** for all class names
- Prefix **component-specific** classes only when truly unique
- Keep class names **short but descriptive**

#### Standard Class Patterns
```
Component:      .card, .table, .alert, .badge
Component part: .card-header, .table-actions
State:          .active, .disabled, .selected
Utility:        .mt-3, .mb-2, .p-4
Action:         .btn-save, .btn-filter, .btn-cancel
```

#### Examples
✅ **GOOD** (general, reusable):
- `.container`, `.card`, `.card-header`
- `.styled-table`, `.btn`, `.btn-info`
- `.alert`, `.badge`, `.badge-success`
- `.searchfilter`, `.form-container`

❌ **BAD** (template-specific):
- `.author-details-container` (use `.container`)
- `.author-header` (use `.card` with `.card-header`)
- `.quiz-section` (use `.card` or `.mt-4`)
- `.status-open` (use `.badge .badge-error`)

### 13.9 Template Structure Pattern

**Standard template structure for all pages:**

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Page Title</title>
    <link rel="stylesheet" type="text/css" th:href="@{/css/styles.css}">
</head>
<body>
<!-- 1. Menu fragment -->
<div th:insert="~{fragments::menu}"></div>

<!-- 2. Standard container -->
<div class="container mt-3">
    <!-- 3. Navigation/Back button (if needed) -->
    <a th:href="@{/back-path}" class="btn btn-cancel">&larr; Back</a>
    
    <!-- 4. Page heading -->
    <h2>Page Title</h2>
    
    <!-- 5. Alerts/Messages -->
    <div th:if="${message}" class="alert mt-3" th:text="${message}"></div>
    
    <!-- 6. Filter section (for list views) -->
    <div class="searchfilter">
        <!-- Filter form -->
    </div>
    
    <!-- 7. Main content (table, cards, form) -->
    <table class="styled-table">
        <!-- Table content -->
    </table>
    
    <!-- OR -->
    
    <div class="card mt-3">
        <!-- Card content -->
    </div>
    
    <!-- 8. Pagination (if needed) -->
    <div class="pagination-controls">
        <!-- Pagination -->
    </div>
</div>
</body>
</html>
```

### 13.10 Responsive Design Rules

- Containers use **max-width** (99%), not fixed width
- Tables are **100% width** by default
- Use **flexbox** for form layouts (`.searchfilter-form`)
- Include **media queries** for mobile (already in styles.css)
- Test on different screen sizes

### 13.11 Accessibility Requirements

- Use **semantic HTML5** elements (`<header>`, `<nav>`, `<main>`, `<section>`)
- Include **proper labels** for all form inputs
- Maintain **keyboard navigation** support
- Ensure **color contrast** meets WCAG AA standards
- Provide **meaningful alt text** for icons/images
- Use **proper heading hierarchy** (h1 > h2 > h3)

### 13.12 Forbidden Practices

❌ **NEVER do these:**
1. Add inline `style=""` attributes in templates
2. Create template-specific CSS classes when general ones exist
3. Duplicate table styles per template
4. Use different button classes for the same action
5. Hard-code widths or heights (use CSS)
6. Skip the standard `.container` wrapper
7. Create custom filter section styles
8. Use inconsistent spacing (use utility classes)
9. Add page-specific styles to `styles.css` (use general names)
10. Override general styles for specific pages

### 13.13 When to Create New CSS

**Only create new CSS classes when:**
1. No existing general class serves the purpose
2. The component is truly unique and reusable
3. The style will be used across multiple templates
4. The class name is semantic and general

**Before creating new CSS:**
1. Check if existing classes can be combined
2. Review similar templates for patterns
3. Ask: "Is this specific to one template or general?"
4. Consult this formatting standards section

### 13.14 Code Review Checklist - Formatting

When reviewing template changes:
- [ ] Uses `.container mt-3` wrapper?
- [ ] Uses `.styled-table` for data tables?
- [ ] No inline `style=""` attributes?
- [ ] Buttons use correct classes for actions?
- [ ] Same action = same button class as other templates?
- [ ] Filter section uses `.searchfilter` pattern?
- [ ] Cards use `.card` and `.card-header`?
- [ ] Alerts use `.alert` class?
- [ ] Badges use `.badge .badge-*` classes?
- [ ] No template-specific CSS classes created?
- [ ] Spacing uses utility classes (`.mt-3`, etc.)?
- [ ] Follows standard template structure?

---

## 14. General Improvements - Grouped by Architecture

### 14.1 Presentation Layer (Thymeleaf Views)

#### Styling Standards
- **No Inline Styles**: All styling must be in `styles.css` - no `style=""` attributes in templates
- **Use General Classes**: Prefer general, reusable classes over template-specific ones
- **Color Scheme**: Blue theme for tables (#1e3a8a, #3b82f6, #eff6ff, #dbeafe)
- **Tables**: Always use `.styled-table` class (100% width and blue theme)
- **Consistency**: Apply same class names and patterns across all views
- **Responsiveness**: Use `.container` with max-width, never fixed widths

#### Component Reusability
- Extract common styles to reusable classes in `styles.css`
- Create Thymeleaf fragments for repeated components:
  - Table templates with consistent headers
  - Pagination controls
  - Error alerts (use `.alert` class)
  - Navigation breadcrumbs
  - Status badges (`.badge .badge-*`)

### 14a.2 Controller Layer

#### Session Validation Pattern
**ALWAYS use SessionService.validateSessionOrRedirect() at the start of methods requiring authentication:**

```java
@GetMapping("/path")
public String someMethod(Model model) {
    String redirect = sessionService.validateSessionOrRedirect();
    if (redirect != null) return redirect;
    
    // ... method logic
}
```

**Benefits:**
- One-line session validation
- Consistent across all methods
- Returns null if valid, redirect view if invalid

#### Authorization Header Patterns

**Pattern 1: GET Request with Authorization**
```java
HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
ResponseEntity<SomeDto> response = restTemplate.exchange(
    url, HttpMethod.GET, entity, SomeDto.class);
```

**Pattern 2: POST/PUT Request with Body and Authorization**
```java
HttpEntity<SomeDto> entity = sessionService.createAuthorizedRequest(someDto);
ResponseEntity<SomeDto> response = restTemplate.exchange(
    url, HttpMethod.POST, entity, SomeDto.class);
```

**Pattern 3: POST Request (alternative)**
```java
restTemplate.postForEntity(url, 
    sessionService.createAuthorizedRequest(someDto), 
    SomeDto.class);
```

**❌ DON'T create headers manually:**
```java
// DON'T DO THIS
HttpHeaders headers = new HttpHeaders();
headers.set("Authorization", "Bearer " + sessionService.getJwtToken());
headers.setContentType(MediaType.APPLICATION_JSON);
HttpEntity<SomeDto> entity = new HttpEntity<>(someDto, headers);
```

**✅ DO use SessionService methods:**
```java
// DO THIS
HttpEntity<SomeDto> entity = sessionService.createAuthorizedRequest(someDto);
```

#### Error Handling Pattern
```java
try {
    // API call
    ResponseEntity<DetailsDto> response = restTemplate.exchange(...);
    populateModelFromDto(model, response.getBody());
    return VIEW_NAME;
} catch (HttpClientErrorException.Forbidden ex) {
    log.error("403 Forbidden: Token expired");
    sessionService.invalidateCurrentSession();
    return VIEW_REDIRECT_AUTH_LOGIN;
} catch (HttpClientErrorException.NotFound ex) {
    log.warn("Resource not found: {}", id);
    populateFallbackModel(model, "Resource not found");
    return VIEW_NAME;
} catch (Exception ex) {
    log.error("Error fetching data: {}", ex.getMessage(), ex);
    populateFallbackModel(model, "Could not load data");
    return VIEW_NAME;
}
```

**Key Points:**
- Use `sessionService.invalidateCurrentSession()` for 403 errors
- Don't manually access RequestContextHolder
- Log errors with appropriate severity

#### SessionService Methods Reference

**Session Validation:**
- `validateSessionOrRedirect()` - Returns null if valid, redirect view if invalid
- `containsValidVars()` - Returns boolean (use only in special cases)

**Authorization Headers:**
- `getAuthorizationHeader()` - For GET requests (returns HttpEntity<Void>)
- `createAuthorizedRequest(T body)` - For POST/PUT with body (returns HttpEntity<T>)
- `createAuthorizedRequest()` - For requests without body (returns HttpEntity<Void>)
- `createAuthHeaders()` - Returns HttpHeaders (for custom scenarios)

**Session Management:**
- `invalidateCurrentSession()` - Invalidate current session (use on 403 errors)
- `invalidateSession(HttpSession)` - Invalidate specific session
- `getLoggedInUser()` - Get logged-in user from session
- `getJwtToken()` - Get JWT token from session

**Usage Example:**
```java
@PostMapping("/create")
public String create(@ModelAttribute SomeDto dto, RedirectAttributes redirectAttributes) {
    // 1. Validate session
    String redirect = sessionService.validateSessionOrRedirect();
    if (redirect != null) return redirect;
    
    // 2. Create authorized request
    HttpEntity<SomeDto> entity = sessionService.createAuthorizedRequest(dto);
    
    try {
        // 3. Call backend
        restTemplate.postForEntity(apiBaseUrl + "/api/resource", entity, SomeDto.class);
        redirectAttributes.addFlashAttribute("message", "Created successfully");
        return "redirect:/list";
    } catch (HttpClientErrorException.Forbidden ex) {
        // 4. Handle 403
        log.error("403 Forbidden: Token expired");
        sessionService.invalidateCurrentSession();
        return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
    } catch (Exception ex) {
        log.error("Error creating resource: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute("errorMessage", "Failed to create");
        return "redirect:/list";
    }
}
```

#### Session Validation
- Always check `sessionService.containsValidVars()` at method start
- Return early with redirect to login if invalid: `return VIEW_REDIRECT_AUTH_LOGIN;`
- Log session validation failures at WARN level
- Never proceed with API calls if session is invalid

#### Model Population
- Create separate helper methods for success and fallback scenarios
- Use meaningful attribute names from `ControllerSettings` constants
- Always include `loggedInUser` attribute for menu display
- Never render a view without setting all required model attributes

**Example**:
```java
private void populateAuthorDetailsModelFromDto(Model model, AuthorDetailsDto dto) {
    model.addAttribute("author", dto.getAuthor());
    model.addAttribute("quizzes", dto.getQuizzes());
    model.addAttribute("questionsByQuiz", dto.getQuestionsByQuiz());
    model.addAttribute("errorsByQuiz", dto.getErrorsByQuiz());
}

private void populateAuthorDetailsModelFallback(Model model, String errorMessage) {
    model.addAttribute("errorMessage", errorMessage);
    model.addAttribute("quizzes", new ArrayList<>());
}
```

### 13.3 Service Layer (API Backend)

#### DTO Assembly Best Practices
- Use builder pattern for complex DTOs with many fields
- Group related data logically (e.g., questions by quiz ID in a Map)
- Validate data before returning - check for nulls
- Handle null cases gracefully with empty collections, not null values
- Reuse existing DTOs (QuestionDto, AuthorErrorDto) instead of creating duplicates

**Example of DTO reuse**:
```java
@Transactional(readOnly = true)
public AuthorDetailsDto getAuthorDetails(Long authorId) {
    // Fetch author
    Author author = authorRepository.findById(authorId)
        .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
    
    // Fetch quizzes
    List<Quiz> quizzes = quizRepository.findByAuthorId(authorId);
    
    // Group questions by quiz
    Map<Long, List<QuestionDto>> questionsByQuiz = new HashMap<>();
    for (Quiz quiz : quizzes) {
        List<QuestionDto> questions = questionService.findByQuizId(quiz.getId());
        questionsByQuiz.put(quiz.getId(), questions);
    }
    
    // Group errors by quiz
    Map<Long, List<AuthorErrorDto>> errorsByQuiz = new HashMap<>();
    for (Quiz quiz : quizzes) {
        List<AuthorErrorDto> errors = errorService.findByQuizId(quiz.getId());
        errorsByQuiz.put(quiz.getId(), errors);
    }
    
    // Assemble DTO
    AuthorDetailsDto dto = new AuthorDetailsDto();
    dto.setAuthor(authorMapper.toDto(author));
    dto.setQuizzes(quizMapper.toDtoArray(quizzes));
    dto.setQuestionsByQuiz(questionsByQuiz);
    dto.setErrorsByQuiz(errorsByQuiz);
    
    return dto;
}
```

#### Transaction Management

- Use `@Transactional(readOnly = true)` for all read operations
- Optimize queries to avoid N+1 problems (use JOIN FETCH or batch queries)
- Consider caching for frequently accessed data
- Keep transactions as short as possible

---

## Documentation Guidelines

### Structure
- Keep current, authoritative design docs in `prompt/*-sd.md`.
- Use `README.md` at the root for project overview and quick start instructions.
- Use this `guidelines.md` file as the central place for coding, architecture, and documentation standards.
- Move historical or narrow-scope Markdown docs into `prompt/archive/` rather than keeping many root-level `.md` files.

### Style
- Use Markdown with a single H1 per document and consistent `##`, `###` for sections.
- Prefer descriptive section names over dates; avoid lines like `Date: December 24, 2025` so docs remain timeless.
- Use fenced code blocks with language identifiers for all code and shell examples.
- Reference other docs by relative path (e.g., `prompt/question-sd.md`, `prompt/archive/question-validation-and-correction.md`).

### Author Operations Sections
- Every `*-sd.md` file under `prompt/` should contain an `## Author Operations` section.
- Use subsections `### Create / Update`, `### View / List`, `### Delete / Archive`, `### Permissions & Roles` where they make sense.
- For modules without direct author-facing behavior (e.g., Docker, Ollama), include a minimal `Author Operations` section that states authors interact only through the UI and references the relevant SD docs.

---

## 14a. Code Quality and Refactoring Guidelines

### 14a.1 SonarQube Compliance

#### Loop Control Flow
**Rule:** Reduce break and continue statements in loops to use at most one.

**Problem:** Multiple `break` and `continue` statements reduce code readability and violate single exit point principle.

**Solution:** Use state-based control flow with result enums.

**Example:**
```java
// ❌ BAD: Multiple break/continue statements
for (Row row : sheet) {
    if (isHeaderRow(row)) {
        continue;  // continue #1
    }
    
    int noNotNull = countValues(row);
    if (noNotNull == 0) {
        consecutiveEmpty++;
        if (consecutiveEmpty > MAX_EMPTY) {
            break;  // break #1
        }
        continue;  // continue #2
    }
    
    processRow(row);
}

// ✅ GOOD: State-based with single break
private enum RowProcessingResult {
    SKIP_HEADER,
    EMPTY_ROW,
    PROCESSED
}

boolean shouldStop = false;
for (Row row : sheet) {
    if (shouldStop) {
        break;  // Single break point
    }
    
    RowProcessingResult result = processRowWithResult(row);
    
    if (result == RowProcessingResult.EMPTY_ROW) {
        consecutiveEmpty++;
        if (consecutiveEmpty > MAX_EMPTY) {
            shouldStop = true;
        }
    } else if (result == RowProcessingResult.PROCESSED) {
        consecutiveEmpty = 0;
    }
    // SKIP_HEADER handled implicitly
}
```

**Benefits:**
- ✅ Single exit point (one break)
- ✅ Clearer intent with explicit states
- ✅ Easier to test (extract to method)
- ✅ Better maintainability

#### Exception Handling
**Rule:** Use specific exception types, not generic RuntimeException.

**Problem:** Generic exceptions lose semantic meaning and make error handling harder.

**Solution:** Create custom exception classes that extend appropriate base exceptions.

**Example:**
```java
// ❌ BAD: Generic exception
public void cleanupData() {
    try {
        deleteAllData();
    } catch (Exception e) {
        throw new RuntimeException("Failed to delete: " + e.getMessage(), e);
    }
}

// ✅ GOOD: Custom exception
public static class DataCleanupException extends RuntimeException {
    public DataCleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}

public void cleanupData() {
    try {
        deleteAllData();
    } catch (Exception e) {
        throw new DataCleanupException("Failed to delete: " + e.getMessage(), e);
    }
}
```

### 14a.2 Spring Cache Self-Invocation

#### The Problem
**Rule:** @Cacheable methods called from within the same class using `this` bypass Spring's cache proxy.

**Why it happens:** Spring's `@Cacheable` works through AOP proxies. Internal calls (`this.cachedMethod()`) go directly to the actual object, not the proxy, so caching never activates.

**Example of Problem:**
```java
@Service
public class MyService {
    @Cacheable("authors")
    public List<Author> getAllAuthors() {
        return repository.findAll();  // Expensive DB call
    }
    
    public List<Author> filterAuthors(String filter) {
        // ❌ BAD: Cache doesn't work!
        List<Author> authors = getAllAuthors();
        return authors.stream()
            .filter(a -> a.getName().contains(filter))
            .collect(Collectors.toList());
    }
}
```

#### The Solution
**Self-Injection Pattern:** Inject the service's own proxy to enable internal caching.

**Implementation:**
```java
@Service
public class MyService {
    private MyService self;  // Will be the Spring proxy
    
    @Autowired
    public void setSelf(@Lazy MyService self) {
        this.self = self;
    }
    
    @Cacheable("authors")
    public List<Author> getAllAuthors() {
        return repository.findAll();
    }
    
    public List<Author> filterAuthors(String filter) {
        // ✅ GOOD: Cache works!
        List<Author> authors = self.getAllAuthors();
        return authors.stream()
            .filter(a -> a.getName().contains(filter))
            .collect(Collectors.toList());
    }
}
```

**Key Points:**
- Use `@Lazy` to prevent circular dependency issues
- Document why self-reference is needed with comments
- Use `self.` for all internal calls to cached methods

**Performance Impact:**
- First call: Same (cache miss)
- Subsequent calls: ~90% faster (cache hit)
- Database load: Reduced by 90%+ for repeated queries

### 14a.3 Logging Best Practices

#### Log Level Strategy
**Rule:** Use appropriate log levels to reduce production noise.

**Levels:**
- **TRACE:** Detailed debug info (e.g., individual authority checks)
- **DEBUG:** Development debug info (e.g., permission check results)
- **INFO:** Important business operations (e.g., data cleanup started/completed)
- **WARN:** Unexpected but recoverable issues (e.g., permission denied)
- **ERROR:** Failures requiring attention (e.g., exceptions, database errors)

**Example:**
```java
// ❌ BAD: Too verbose for production
logger.info("Checking authority: {}", authority.getAuthority());
logger.info("User has admin: {}", hasAdmin);

// ✅ GOOD: Appropriate levels
if (logger.isTraceEnabled()) {
    authorities.forEach(a -> 
        logger.trace("Authority: {}", a.getAuthority())
    );
}
logger.debug("Permission check result: {}", hasPermission);
logger.info("Data cleanup started");
logger.warn("Permission denied for user: {}", username);
logger.error("Database error: {}", e.getMessage(), e);
```

#### Conditional Logging
**Rule:** Use `isXxxEnabled()` checks for expensive log operations.

**Example:**
```java
// ❌ BAD: Builds string even if TRACE disabled
logger.trace("Details: " + buildExpensiveString());

// ✅ GOOD: Only builds if TRACE enabled
if (logger.isTraceEnabled()) {
    logger.trace("Details: {}", buildExpensiveString());
}
```

### 14a.4 Method Extraction

#### When to Extract
Extract methods when:
1. A method does more than one thing
2. Logic is repeated in multiple places
3. Complex conditionals make code hard to read
4. Method exceeds 30-50 lines
5. A block of code has its own responsibility

#### How to Extract
**Example:**
```java
// ❌ BAD: Long method with multiple responsibilities
public String processSheet(Sheet sheet) {
    for (Row row : sheet) {
        // 10 lines of header checking
        // 15 lines of validation
        // 20 lines of processing
        // 10 lines of error handling
    }
}

// ✅ GOOD: Extracted helper methods
public String processSheet(Sheet sheet) {
    for (Row row : sheet) {
        RowProcessingResult result = processRowWithResult(row);
        handleProcessingResult(result);
    }
}

private RowProcessingResult processRowWithResult(Row row) {
    if (isHeaderRow(row)) return RowProcessingResult.SKIP_HEADER;
    if (isEmptyRow(row)) return RowProcessingResult.EMPTY_ROW;
    processValidRow(row);
    return RowProcessingResult.PROCESSED;
}

private void handleProcessingResult(RowProcessingResult result) {
    // Handle result logic
}
```

### 14a.5 Code Review Checklist

Before committing code, verify:
- [ ] No SonarQube errors (warnings are acceptable if documented)
- [ ] Appropriate exception types used
- [ ] Cache self-invocation pattern used where needed
- [ ] Log levels appropriate for production
- [ ] Methods follow single responsibility principle
- [ ] Complex logic extracted to helper methods
- [ ] JavaDoc comments for public methods
- [ ] Tests pass
- [ ] No compilation warnings (or documented why acceptable)

### 14a.6 Refactoring Documentation

When refactoring, document:
1. **What was changed** - Specific files and methods
2. **Why it was changed** - Problem being solved
3. **How it was changed** - Technical approach
4. **Impact** - Performance, functionality, compatibility
5. **Testing** - How to verify the change

**Example template:**
```markdown
## Refactoring: [Feature Name]

### Problem
- SonarQube error: [description]
- Performance issue: [description]

### Solution
- Implemented [approach]
- Changed [specific details]

### Files Modified
- `ClassName.java` - [changes]

### Impact
- ✅ Performance: [improvement]
- ✅ Code Quality: [improvement]
- ✅ Functionality: Preserved

### Testing
1. [Test case 1]
2. [Test case 2]

### Status
✅ Complete - [date]
```

### 14a.7 Fluent Logging Pattern

**Standard:** Use SLF4J fluent logging API with concise pattern.

**Pattern:**
```java
// ✅ GOOD: Concise fluent pattern
log.atInfo().addArgument(value).log("Message with arg: {}");
log.atError().setCause(e).addArgument(id).log("Error processing item: {}");
log.atWarn().addArgument(id).log("Item not found: {}");
log.atDebug().addArgument(status).log("Processing status: {}");

// ❌ BAD: Verbose pattern with setMessage
log.atInfo().setMessage("Message with arg: {}").addArgument(value).log();

// ❌ BAD: Old-style logging
log.info("Message with arg: {}", value);
log.error("Error: {}", message, exception);
```

**Logger Naming:**
```java
// ✅ GOOD: Use 'log' consistently
private static final Logger log = LoggerFactory.getLogger(MyClass.class);

// ❌ BAD: Inconsistent naming
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
private static final Logger LOG = LoggerFactory.getLogger(MyClass.class);
```

**Exception Handling:**
```java
// ✅ GOOD: Use setCause() for exceptions
log.atError().setCause(e).addArgument(id).log("Failed to process: {}");

// ❌ BAD: Exception as last argument
log.error("Failed to process: {}", id, e);
```

---

## 15. Question Controller Guidelines

### 15.1 QuestionCorrectionService Pattern

**Decision**: ✅ Keep QuestionCorrectionService as a **separate service** - Do NOT merge into controller

**Rationale**:
- **Separation of Concerns**: Controller handles HTTP requests/responses, service handles API delegation logic
- **Single Responsibility**: Service encapsulates all correction-related API calls
- **Reusability**: Service can be used by other controllers if needed
- **Testability**: Service logic can be unit tested independently
- **Maintainability**: Changes to correction API calls are isolated to the service

### 15.2 Helper Method Organization

ThyQuestionController demonstrates good helper method patterns:
- **Private helpers** for repeated logic (renderQuestionList, prepareQuestionForSave)
- **Clear naming** that describes purpose
- **Focused responsibility** - each helper does one thing well
- **No code duplication** - DRY principle applied

### 15.3 Error Handling in Controllers

```java
try {
    // API call
    ResponseEntity<QuestionDto> response = restTemplate.exchange(...);
    return response.getBody();
} catch (HttpClientErrorException.Forbidden ex) {
    log.atError().log("403 Forbidden: Token expired");
    sessionService.invalidateCurrentSession();
    return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
} catch (Exception ex) {
    log.atError().setCause(ex).log("Unexpected error");
    model.addAttribute("errorMessage", "Unexpected error. Please try again.");
    return ControllerSettings.VIEW_ERROR;
}
```

### 15.4 InterruptedException Handling

When calling services that may be interrupted (AI operations, long-running tasks):

```java
try {
    QuestionCorrectionDto result = correctionService.correctGrammar(correctionDto);
    return ResponseEntity.ok(result);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // Restore interrupt flag
    log.atError().setCause(e).log("Operation interrupted");
    return ResponseEntity.internalServerError().build();
} catch (Exception e) {
    log.atError().setCause(e).log("Error processing request");
    return ResponseEntity.internalServerError().build();
}
```

---

## 16. Request Logging and Monitoring

### 18.1 Request Logging Interceptor

**Implementation**: RequestLoggingInterceptor in myquiz-thymeleaf

All requests are automatically logged with:
- **Unique Request ID** (8-character UUID)
- **Execution time** tracking
- **Performance warnings** for slow requests (>1s)
- **Security**: Sensitive headers automatically redacted
- **Client IP** detection (proxy-aware)

### 18.2 Logging Configuration

```properties
# INFO level - basic request logging (recommended for production)
logging.level.com.unitbv.myquiz.thy.interceptor.RequestLoggingInterceptor=INFO

# DEBUG level - detailed logging with headers (for development)
logging.level.com.unitbv.myquiz.thy.interceptor.RequestLoggingInterceptor=DEBUG

# WARN level - only slow requests and errors
logging.level.com.unitbv.myquiz.thy.interceptor.RequestLoggingInterceptor=WARN
```

### 18.3 Request ID Correlation

The interceptor adds `X-Request-Id` header to all responses:

```java
// Access in controller
String requestId = response.getHeader("X-Request-Id");
log.atInfo().addArgument(questionId).addArgument(requestId).log("Processing question {} with request ID {}");
```

### 18.4 Log Analysis

```powershell
# Find all requests to an endpoint
Select-String -Path logs/myquiz-thymeleaf.log -Pattern "URI: /questions"

# Find slow requests
Select-String -Path logs/myquiz-thymeleaf.log -Pattern "WARNING: Slow request"

# Trace specific request
Select-String -Path logs/myquiz-thymeleaf.log -Pattern "a1b2c3d4"
```

### 18.5 Sensitive Data Protection

**Never log**:
- Passwords (plain or hashed)
- Credit card numbers
- Social security numbers
- API keys or secrets
- Full authorization tokens

**Automatically redacted headers**:
- authorization
- cookie
- set-cookie
- x-auth-token
- x-csrf-token

### 18.6 Performance Monitoring

Monitor these metrics from logs:
- **Average response time**: Track execution times
- **Slow request frequency**: Count warnings
- **Error rate**: Track 4xx and 5xx status codes
- **Popular endpoints**: Analyze most frequently accessed URIs
- **Geographic distribution**: Track client IP addresses

---

## 17. Historical Documentation Archive

All implementation status documents, phase completion reports, and fix summaries should be moved to `archive/` directory to keep the root clean. These include:

- Phase completion documents (PHASE1-COMPLETE.md, etc.)
- Implementation status reports (IMPLEMENTATION-STATUS-PHASE1.md, etc.)
- Fix reports (FIX-403-ADMIN-ENDPOINTS.md, ALL-FIXES-COMPLETE.md, etc.)
- Feature completion summaries (ADMIN-INTERFACE-COMPLETE.md, etc.)
- Review and repair documents (AUTHORSERVICE-REVIEW-REPAIR.md, etc.)

**Keep in root:**
- README.md (project overview)
- guidelines.md (this file)
- docker-compose.yml and other config files

**Keep in prompt/:**
- *-sd.md files (system design documents)

**Move to archive/:**
- All other .md files documenting specific implementations, fixes, or phases

---

## 18. Database Management and Performance

### 18.1 Sequence Synchronization

PostgreSQL sequences can become out of sync with existing data when:
- Importing data with explicit IDs
- Restoring from backups
- Switching from GenerationType.AUTO to SEQUENCE
- Manual data inserts with specific IDs

**Symptoms:**
- "duplicate key value violates unique constraint" errors
- New entities fail to save

**Solution:**
```bash
# Run sequence synchronization script
psql -U postgres -d myquiz -f data/fix-all-sequences.sql

# Or via Docker
docker exec -i myquiz-db psql -U postgres -d myquiz < data/fix-all-sequences.sql
```

**What it does:**
- Drops and recreates all entity sequences
- Sets each sequence to MAX(id) + 1
- Displays verification summary

**When to run:**
- ✅ After deploying entity changes with SEQUENCE strategy
- ✅ After importing test/dummy data
- ✅ After database restore
- ✅ When encountering duplicate key errors

**Verification:**
```sql
-- Check current sequence values
SELECT 
    'quiz' AS table,
    currval('quiz_seq') AS next_value,
    (SELECT MAX(id) FROM quiz) AS max_id;

-- Verify all sequences
SELECT 
    schemaname,
    sequencename,
    last_value
FROM pg_sequences
WHERE schemaname = 'public';
```

**Safety:**
- ✅ Safe to run on live database (atomic updates)
- ✅ Does not modify existing data
- ✅ Idempotent - can run multiple times
- ✅ Only affects future inserts

### 18.2 Performance Indexes

Database indexes significantly improve query performance, especially for filtering and pagination operations.

**Performance Impact:**
- Before indexes: Full table scans, slow queries on large datasets (>10,000 records)
- After indexes: Index-based lookups, 50-90% query time reduction

**Critical Indexes:**

1. **idx_question_quiz_author_id**: Index on `question.quiz_author_id`
   - Critical for joining questions with quiz_author table
   - Used in all question filtering queries

2. **idx_question_type**: Index on `question.type`
   - Used when filtering by question type (MULTICHOICE/TRUEFALSE)

3. **idx_quiz_author_quiz_author**: Composite index on `quiz_author(quiz_id, author_id)`
   - Optimizes lookups by quiz and author combinations

4. **idx_quiz_course**: Index on `quiz.course`
   - Used when filtering questions by course

5. **idx_author_name**: Index on `author.name`
   - Used when looking up authors by name

6. **idx_question_crt_no**: Index on `question.crt_no`
   - Used for sorting questions by row number

**How to apply:**
```bash
# Run performance index script
psql -U postgres -d myquiz -f data/add-performance-indexes.sql

# Or via Docker
docker exec -i myquiz-db psql -U postgres -d myquiz < data/add-performance-indexes.sql
```

**Verification:**
```sql
-- Check if indexes exist
SELECT indexname, tablename 
FROM pg_indexes 
WHERE tablename IN ('question', 'quiz_author', 'quiz', 'author')
ORDER BY tablename, indexname;

-- Analyze query performance
EXPLAIN ANALYZE
SELECT q.* FROM question q
LEFT JOIN quiz_author qa ON q.quiz_author_id = qa.id
LEFT JOIN author a ON qa.author_id = a.id
LEFT JOIN quiz qz ON qa.quiz_id = qz.id
WHERE qz.course = 'Math 101'
LIMIT 10 OFFSET 0;
```

**When to run:**
- ✅ After initial database setup
- ✅ When experiencing slow queries
- ✅ In production environments
- ✅ After bulk data imports

**Notes:**
- All indexes use `IF NOT EXISTS` to prevent errors on re-run
- Script includes `ANALYZE` commands to update database statistics
- No data modification occurs, only index creation

**Rollback:**
```sql
DROP INDEX IF EXISTS idx_question_quiz_author_id;
DROP INDEX IF EXISTS idx_question_type;
DROP INDEX IF EXISTS idx_quiz_author_quiz_author;
DROP INDEX IF EXISTS idx_quiz_course;
DROP INDEX IF EXISTS idx_author_name;
DROP INDEX IF EXISTS idx_question_crt_no;
```

### 18.3 Database Setup Checklist

For a new MyQuiz deployment:

**1. Initial Database Setup**
```bash
psql -U postgres -d myquiz -f data/myquiz_dump.sql
```

**2. Add Role/Permission System**
```bash
psql -U postgres -d myquiz -f data/add-role-permission-tables.sql
```

**3. Verify Role/Permission Setup**
```bash
psql -U postgres -d myquiz -f data/verify-role-permission-system.sql
```

**4. Add Performance Indexes**
```bash
psql -U postgres -d myquiz -f data/add-performance-indexes.sql
```

**5. Insert Sample Data (Optional)**
```bash
psql -U postgres -d myquiz -f data/insert-dummy-courses.sql
psql -U postgres -d myquiz -f data/insert-dummy-quizzes.sql
```

**6. Fix Sequences (if needed)**
```bash
psql -U postgres -d myquiz -f data/fix-all-sequences.sql
```

### 18.4 Database Verification Commands

**Check Database Status:**
```sql
-- List all tables
\dt

-- Check table sizes
SELECT 
    schemaname as schema,
    tablename as table,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check user count
SELECT COUNT(*) FROM users;

-- Check role/permission setup
SELECT r.name, COUNT(p.permission_id) as permission_count
FROM roles r
LEFT JOIN role_permissions rp ON r.role_id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.permission_id
GROUP BY r.name;
```

**Check Sequences:**
```sql
-- List all sequences
\ds

-- Verify sequences are synchronized
SELECT 
    'SELECT SETVAL('||quote_literal(quote_ident(sequence_namespace.nspname)||'.'||quote_ident(class_sequence.relname))||', COALESCE(MAX('||quote_ident(pg_attribute.attname)||'), 1)) FROM '||quote_ident(table_namespace.nspname)||'.'||quote_ident(class_table.relname)||';'
FROM pg_depend 
INNER JOIN pg_class AS class_sequence ON class_sequence.oid = pg_depend.objid
INNER JOIN pg_class AS class_table ON class_table.oid = pg_depend.refobjid
INNER JOIN pg_attribute ON pg_attribute.attrelid = class_table.oid
INNER JOIN pg_namespace as table_namespace ON table_namespace.oid = class_table.relnamespace
INNER JOIN pg_namespace AS sequence_namespace ON sequence_namespace.oid = class_sequence.relnamespace
WHERE pg_depend.deptype = 'a'
  AND pg_depend.refobjsubid > 0
  AND class_sequence.relkind = 'S';
```

### 18.5 Common Database Issues and Solutions

**Issue: Duplicate key violations**
- **Cause:** Sequences out of sync with existing data
- **Solution:** Run `fix-all-sequences.sql`

**Issue: Slow queries on large datasets**
- **Cause:** Missing database indexes
- **Solution:** Run `add-performance-indexes.sql` and verify indexes are created

**Issue: "relation 'quiz_seq' does not exist"**
- **Cause:** Application hasn't created sequences yet
- **Solution:** Start application once to let Hibernate create sequences, then run synchronization script

**Issue: "permission denied for sequence"**
- **Cause:** Insufficient database privileges
- **Solution:** Grant sequence permissions:
  ```sql
  GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO myquiz;
  ```

**Issue: Sequence still generating duplicate IDs after fix**
- **Cause:** Sequence might be cached by application
- **Solution:** Restart application after running script to clear Hibernate caches

### 18.6 Database Maintenance Schedule

| Task | Frequency | Script/Command |
|------|-----------|----------------|
| Backup database | Daily | `pg_dump -U postgres myquiz > backup.sql` |
| Check sequences | After data import | `fix-all-sequences.sql` |
| Verify roles/permissions | After updates | `verify-role-permission-system.sql` |
| Review error logs | Weekly | Check `question_errors` table |
| Optimize indexes | Monthly | Review query performance with `EXPLAIN ANALYZE` |
| Vacuum database | Weekly | `VACUUM ANALYZE;` |

### 18.7 Database Documentation Resources

- `data/README.md` - Complete index of all database scripts and documentation
- `data/README-fix-sequences.md` - Detailed sequence synchronization guide
- `data/README-performance-indexes.md` - Performance optimization guide
- `data/README-role-permission-system.md` - Role/permission implementation guide
- `data/VERIFICATION-CHECKLIST.md` - Implementation verification checklist

---

## 19. Operations Runbook (Moved from README)

This section is the operational source of truth. Keep `README.md` concise and place detailed run/DB troubleshooting steps here.

### 19.1 Run Manually (Local Development)

Because service defaults differ between modules, verify active property values before running. The commands below are a working baseline for local PostgreSQL on `localhost:5432`.

**1. Initialize database once:**

```powershell
psql -U postgres -c "CREATE DATABASE myquiz;"
psql -h localhost -p 5432 -U postgres -d myquiz -f data/init-database.sql
```

**2. Build modules from repository root:**

```powershell
mvn clean install
```

**3. Start services in separate terminals (recommended order):**

```powershell
# Terminal 1 - IAM
cd myquiz-iam
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/myquiz"
$env:SPRING_DATASOURCE_USERNAME="myquiz_user"
$env:SPRING_DATASOURCE_PASSWORD="myquiz_password"
$env:IAM_PORT="8888"
mvn spring-boot:run
```

```powershell
# Terminal 2 - Auth
cd myquiz-auth
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/myquiz"
$env:SPRING_DATASOURCE_USERNAME="myquiz_user"
$env:SPRING_DATASOURCE_PASSWORD="myquiz_password"
$env:AUTH_PORT="8090"
$env:MYQUIZ_API_BASE_URL="http://localhost:8888/api"
mvn spring-boot:run
```

```powershell
# Terminal 3 - App API
cd myquiz-app
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/myquiz"
$env:SPRING_DATASOURCE_USERNAME="myquiz_user"
$env:SPRING_DATASOURCE_PASSWORD="myquiz_password"
$env:SERVER_PORT="8082"
mvn spring-boot:run
```

```powershell
# Terminal 4 - Thymeleaf UI
cd myquiz-thymeleaf
$env:SERVER_PORT="8080"
$env:MYQUIZ_API_BASE_URL="http://localhost:8082/api"
$env:AUTH_API_URL="http://localhost:8090/api/auth"
mvn spring-boot:run
```

**4. Verify access:**
- UI: `http://localhost:8080`
- API: `http://localhost:8082/api`
- Swagger: `http://localhost:8082/swagger-ui.html`

### 19.2 Run with Docker

In this repository, core services are in the `dev` profile.

```powershell
mvn clean install
docker-compose --profile dev up -d --build
```

**Notes:**
- `data/init-database.sql` runs automatically on first container startup (empty volume).
- To force re-initialization: `docker-compose down -v` then start again.
- Host ports: UI `8080`, API `8082`, DB `5433`, Adminer `8083`.

Useful commands:

```powershell
docker-compose --profile dev ps
docker-compose logs -f myquiz-app
docker exec -it myquiz-postgres psql -U myquiz_user -d myquiz
docker-compose down
```

### 19.3 SQL Scripts and When to Use Them

Current scripts in `data/`:

- `data/init-database.sql` - Initial schema/roles/admin setup for a clean DB.
- `data/test-data/insert-dummy-courses.sql` - Optional sample courses.
- `data/test-data/insert-dummy-quizzes.sql` - Optional sample quizzes.
- `data/verification/verify-role-permission-system.sql` - Verifies RBAC wiring.
- `data/verification/verify-extended-statistics-permission.sql` - Verifies statistics permission setup.
- `data/verification/fix-admin-password.sql` - Resets/repairs admin password state.
- `data/fix-study-year-constraint.sql` - Repairs study-year DB constraint.

Example script execution (host DB connection):

```powershell
psql -h localhost -p 5433 -U myquiz_user -d myquiz -f data/verification/verify-role-permission-system.sql
```

Example script execution (inside Docker container):

```powershell
Get-Content data\verification\verify-role-permission-system.sql | docker exec -i myquiz-postgres psql -U myquiz_user -d myquiz
```

### 19.4 Ollama (Optional AI Integration)

- Run Ollama on the host machine (`http://localhost:11434`).
- Start stack with AI-enabled configuration:

```powershell
docker-compose --profile dev --profile ai up -d
```

- Quick checks:

```powershell
curl http://localhost:11434/api/tags
curl http://localhost:8082/api/ollama/status
```

### 19.5 Common Operations Issues

**Issue: Core services are not reachable after `docker-compose up`**
- Cause: `dev` profile was not enabled.
- Fix: `docker-compose --profile dev up -d --build`.

**Issue: DB scripts seem ignored in Docker**
- Cause: initialization scripts only run when the PostgreSQL volume is empty.
- Fix: `docker-compose down -v` and start again.

**Issue: Manual run fails with connection to `postgres:5432`**
- Cause: some modules default to Docker hostnames.
- Fix: set `SPRING_DATASOURCE_URL` to `jdbc:postgresql://localhost:5432/myquiz` before `mvn spring-boot:run`.


