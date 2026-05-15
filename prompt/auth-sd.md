# Authentication & Session Software Design

## 1. Overview

This document describes the authentication, session management, and authorization handling in the MyQuiz application. The system uses JWT tokens for authentication and provides reusable session services for authorization across all Thymeleaf controllers.

## 2. Architecture Components

### 2.1 myquiz-auth Module
- Spring Boot authentication service
- Handles login, registration, token issuance, password validation
- **Exclusively integrates with myquiz-iam for user data**
- Acts as the sole gateway to user management
- Base URL: `http://localhost:AUTH_PORT/api/auth`
- **No GUI logic**

### 2.2 myquiz-iam Module
- Spring Boot user management service
- Manages users, roles, permissions, policies
- Stores user data in PostgreSQL
- **Used exclusively by myquiz-auth service**
- No other service may access myquiz-iam directly
- Base URL: `http://localhost:IAM_PORT/api/users`
- **No GUI logic**

### 2.3 SessionService (myquiz-thymeleaf)
- Centralized session and authorization management
- Provides reusable methods for all Thymeleaf controllers
- Handles JWT token validation and HTTP entity creation
- Location: `com.unitbv.myquiz.thy.service.SessionService`

## 2.5 Author Operations

### Create / Update
- Authors register accounts and update their own credentials through the authentication flow.
- Author creation and role assignment happen through `myquiz-auth` and `myquiz-iam` services; there is no direct UI for managing other authors here.

### View / List
- Authors can view their own authentication status (logged in / logged out) via the UI, but there is no listing of other authors in this module.

### Delete / Archive
- Account deactivation or deletion, when supported, is handled by `myquiz-auth` in coordination with `myquiz-iam`. Author-facing UI exposes only logout.

### Permissions & Roles
- Authors must authenticate to access protected resources.
- Roles and permissions are resolved by `myquiz-auth` based on data from `myquiz-iam`.

## 3. Authentication Operations

### 3.1 User Login

**Flow:**
1. User submits credentials via login.html
2. ThyAuthController receives POST /auth/login
3. Controller calls myquiz-auth REST API: POST /api/auth/login
4. myquiz-auth validates credentials with myquiz-iam (exclusive access)
5. On success: JWT token issued and stored in session
6. User redirected to home page

**DTOs:**
- `LoginRequest` (username, password)
- `LoginResponse` (token, username, roles)

**Error Modes:**
- 401 Unauthorized (invalid credentials)
- 500 Internal Server Error (service unavailable)

### 3.2 User Registration

**Flow:**
1. User submits registration form via register.html
2. ThyAuthController receives POST /register
3. Controller calls myquiz-auth REST API: POST /api/auth/register
4. myquiz-auth creates user in myquiz-iam
5. On success: User auto-logged in with JWT token
6. User redirected to home page

**DTOs:**
- `RegisterRequest` (username, email, password)
- `RegisterResponse` (token, username)

**Error Modes:**
- 400 Bad Request (validation error, duplicate username)
- 500 Internal Server Error

### 3.3 User Logout

**Flow:**
1. User clicks logout
2. ThyAuthController receives GET /logout
3. Session invalidated
4. JWT token removed
5. User redirected to login page

## 4. SessionService Methods

### 4.1 Session Validation

#### `validateSessionOrRedirect()`
**Purpose:** Validate session and JWT token at the start of every authenticated method

**Returns:**
- `null` if session is valid
- `"redirect:/login"` if session is invalid

**Usage:**
```java
String redirect = sessionService.validateSessionOrRedirect();
if (redirect != null) return redirect;
```

**Replaces:** Manual `containsValidVars()` checks

#### `containsValidVars()`
**Purpose:** Check if session contains valid user and JWT token

**Returns:** `true` if valid, `false` otherwise

**Details:**
- Validates session exists
- Validates logged-in user exists
- Validates JWT token exists and is not blank
- Logs detailed error messages for debugging

### 4.2 HTTP Entity Creation

#### `createAuthorizedRequest(T body)`
**Purpose:** Create HTTP entity with authorization headers and request body

**Parameters:**
- `body` (T): Request body object

**Returns:** `HttpEntity<T>` with Authorization header and JSON content type

**Usage:**
```java
SomeDto dto = new SomeDto();
HttpEntity<SomeDto> entity = sessionService.createAuthorizedRequest(dto);
ResponseEntity<ResultDto> response = restTemplate.exchange(url, HttpMethod.POST, entity, ResultDto.class);
```

**Replaces:** Manual header and entity creation

#### `createAuthorizedRequest()`
**Purpose:** Create HTTP entity with authorization headers (no body)

**Returns:** `HttpEntity<Void>` with Authorization header

**Usage:**
```java
HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
ResponseEntity<ResultDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, ResultDto.class);
```

**Use Case:** GET requests requiring authorization

#### `getAuthorizationHeader()`
**Purpose:** Get HTTP entity with authorization header (legacy method)

**Returns:** `HttpEntity<Void>` with Authorization header

**Note:** Prefer `createAuthorizedRequest()` for new code

#### `createAuthHeaders()`
**Purpose:** Create HTTP headers with Authorization and Content-Type

**Returns:** `HttpHeaders` with Bearer token and application/json

**Usage:** Custom scenarios requiring headers

### 4.3 Session Management

#### `invalidateCurrentSession()`
**Purpose:** Invalidate the current user's session

**Usage:**
```java
sessionService.invalidateCurrentSession();
```

**Replaces:** Manual RequestContextHolder access

**Details:**
- Safely handles session invalidation
- Logs session ID
- Try-catch for IllegalStateException

#### `invalidateSession(HttpSession session)`
**Purpose:** Invalidate a specific session

**Parameters:**
- `session` (HttpSession): Session to invalidate

**Details:**
- Try-catch for IllegalStateException
- Logs session ID
- Safe handling of already-invalidated sessions

## 5. Standard Patterns for Thymeleaf Controllers

### Pattern 1: Session Validation
**Use at start of EVERY authenticated method:**
```java
@GetMapping("/some-endpoint")
public String someMethod(Model model) {
    String redirect = sessionService.validateSessionOrRedirect();
    if (redirect != null) return redirect;
    
    // Continue with method logic
}
```

### Pattern 2: GET with Authorization
```java
HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
ResponseEntity<SomeDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, SomeDto.class);
```

### Pattern 3: POST/PUT with Body
```java
SomeDto requestDto = new SomeDto();
HttpEntity<SomeDto> entity = sessionService.createAuthorizedRequest(requestDto);
ResponseEntity<ResultDto> response = restTemplate.exchange(url, HttpMethod.POST, entity, ResultDto.class);
```

### Pattern 4: DELETE with Authorization
```java
HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
```

### Pattern 5: Logout
```java
@GetMapping("/logout")
public String logout() {
    sessionService.invalidateCurrentSession();
    return "redirect:/login";
}
```

## 6. Controllers Using SessionService

All Thymeleaf controllers should use SessionService for consistent session and authorization handling:

- ✅ ThyAuthorController
- ✅ ThyAuthorErrorController
- ✅ ThyQuestionController
- ✅ ThyUploadController
- ✅ ThyCourseController
- ✅ ThyHomeController
- ✅ ThyQuizController

## 7. Security Configuration

### 7.1 myquiz-app Security

**File:** `SecurityConfig.java` in myquiz-app

**Endpoints without authentication:**
- `/api/auth/login`
- `/api/auth/register`
- `/api/users/find/**`
- `/api/system/health`
- `/api/upload/**` (for service-to-service communication)
- `/css/**`, `/js/**`, `/images/**`
- `/swagger-ui/**`, `/v3/api-docs/**`

**Endpoints requiring authentication:**
- All other `/api/**` endpoints

**JWT Token Validation:**
- All authenticated requests require valid JWT token in Authorization header
- Token format: `Bearer <token>`

### 7.2 Service-to-Service Communication

**Upload endpoints** (`/api/upload/**`) allow requests without JWT authentication to support service-to-service communication between myquiz-thymeleaf and myquiz-app.

**Security Considerations:**
- File size limits should be enforced
- File type validation required
- Rate limiting recommended for production
- IP whitelisting optional (Docker network: 172.18.0.0/16)
- Virus scanning recommended

## 8. Error Handling

### 8.1 Session Errors
- **Session Expired:** Redirect to `/login` with message
- **Missing JWT Token:** Redirect to `/login`
- **Invalid JWT Token:** Redirect to `/login`

### 8.2 Authentication Errors
- **401 Unauthorized:** Invalid credentials
- **403 Forbidden:** Access denied (insufficient permissions)
- **500 Internal Server Error:** Service unavailable

### 8.3 Logging
- Session validation failures logged with details
- JWT token creation logged
- Session invalidation logged with session ID
- Authentication attempts logged

## 9. Benefits of SessionService Approach

### Code Reduction
- **42+ lines of boilerplate removed** per controller
- **Cleaner, more readable code**
- **Consistent error handling**

### Maintainability
- Centralized session logic
- Single source of truth for authorization
- Easy to update all controllers by changing SessionService

### Debugging
- Detailed logging for validation failures
- Clear error messages
- Consistent logging format

## 10. Migration Impact

### Changes Required
When refactoring a controller to use SessionService:

1. **Remove manual session checks:**
```java
// OLD
if (!sessionService.containsValidVars()) {
    return "redirect:/login";
}

// NEW
String redirect = sessionService.validateSessionOrRedirect();
if (redirect != null) return redirect;
```

2. **Remove manual header creation:**
```java
// OLD
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.set("Authorization", "Bearer " + session.getAttribute("jwtToken"));
HttpEntity<SomeDto> entity = new HttpEntity<>(dto, headers);

// NEW
HttpEntity<SomeDto> entity = sessionService.createAuthorizedRequest(dto);
```

3. **Remove manual session invalidation:**
```java
// OLD
RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
if (requestAttributes instanceof ServletRequestAttributes) {
    HttpSession session = ((ServletRequestAttributes) requestAttributes).getRequest().getSession(false);
    if (session != null) {
        session.invalidate();
    }
}

// NEW
sessionService.invalidateCurrentSession();
```

4. **Remove unused imports:**
- `HttpHeaders`
- `MediaType`
- `RequestContextHolder`
- `ServletRequestAttributes`

## 11. IAM Architecture Refactoring

### 11.1 Architectural Change

Successfully refactored the MyQuiz microservices architecture to establish **myquiz-iam as exclusively used by myquiz-auth**. This creates a clean authentication/authorization layer with proper separation of concerns.

**Key Achievement:** myquiz-app no longer has any direct dependency on myquiz-iam, ensuring that all user management operations flow through the dedicated authentication service.

### 11.2 Service Dependencies

**Before Refactoring:**
```
myquiz-app → myquiz-iam (direct access)
myquiz-auth → myquiz-iam
```

**After Refactoring:**
```
myquiz-app → postgres (direct)
myquiz-auth → myquiz-iam (exclusive access)
```

### 11.3 Docker Compose Changes

**myquiz-app service:**
- ❌ Removed dependency: `myquiz-iam:condition: service_started`
- ❌ Removed environment variable: `DB_SERVICE_URL`
- ✅ Now depends only on: `postgres:condition: service_healthy`

**myquiz-auth service:**
- ✅ Retains dependency: `myquiz-iam:condition: service_started`
- ✅ Retains environment variable: `MYQUIZ_IAM_URL`
- ✅ Exclusive gateway to user management

### 11.4 Code Changes

**Removed from myquiz-app:**
- `IamClient.java` - REST client for IAM service
- `DB_SERVICE_URL` configuration
- All direct IAM service calls

**Updated in myquiz-app:**
- `QuestionService.java` - Removed IAM client dependency
- `AuthorService.java` - Uses database directly for author operations

### 11.5 Session Management Changes

**Session Invalidation Fix:**
- Fixed logout to properly invalidate HTTP session
- Ensured JWT token removal from session attributes
- Proper redirect to login page after logout
- Session cleanup prevents unauthorized access

**Changes Made:**
```java
// ThyAuthController.java
@GetMapping("/logout")
public String logout(HttpSession session, HttpServletResponse response) {
    session.invalidate();  // Properly invalidates session
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    return "redirect:/auth/login";
}
```

**Session Summary:**
- All session operations use HttpSession properly
- JWT tokens stored in session attributes
- SessionService provides centralized session management
- Logout properly cleans up session state
- No session leaks or unauthorized access

### 11.6 Benefits

**Security:**
- ✅ Single point of user management (myquiz-auth)
- ✅ Reduced attack surface (no direct IAM access from app)
- ✅ Better access control and auditing

**Architecture:**
- ✅ Clear separation of concerns
- ✅ Reduced service coupling
- ✅ Easier to maintain and evolve

**Performance:**
- ✅ Fewer service dependencies
- ✅ Faster startup (myquiz-app doesn't wait for IAM)
- ✅ Better scalability

## 12. Testing Checklist

- [ ] Login with valid credentials succeeds
- [ ] Login with invalid credentials shows error
- [ ] Registration with valid data creates user and logs in
- [ ] Registration with duplicate username shows error
- [ ] Logout invalidates session and redirects to login
- [ ] Accessing authenticated endpoint without login redirects to login
- [ ] Session expiration redirects to login
- [ ] JWT token correctly added to all backend requests
- [ ] Authorization header correctly formatted
- [ ] SessionService methods work across all controllers
- [ ] myquiz-app runs without myquiz-iam dependency
- [ ] Session properly invalidated on logout

## 13. Related Documentation

- See `guidelines.md` for architecture overview
- See `core-sd.md` for microservices ecosystem
- See individual *-sd.md files for specific controller patterns
- See `docker-sd.md` for deployment configuration

---

## 14. Role and Permission Management

### 14.1 Overview

The MyQuiz application implements a comprehensive role-based access control (RBAC) system with granular permissions. This system ensures that users can only access functionalities they are authorized to use, both at the UI level (menu filtering) and at the API level (endpoint protection).

**Key Features:**
- Multi-role support: Users can have multiple roles
- Fine-grained permissions: Separate READ and MODIFY permissions for each entity
- Default roles: Administrator (full access) and Guest (read-only)
- JWT-based permission transport: Roles and permissions embedded in JWT tokens
- UI filtering: Menu items filtered based on user permissions
- API protection: Endpoints return 403 for unauthorized access
- GUI management: Full admin interface for managing users, roles, and permissions

### 14.2 Entity Model

#### 14.2.1 User Entity (Enhanced)
**Location:** `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/entity/User.java`

```java
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(name = "hashed_password", nullable = false, length = 60)
    private String hashedPassword;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // User can have multiple roles
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
```

#### 14.2.2 Role Entity
**Location:** `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/entity/Role.java`

```java
@Entity
@Table(name = "roles")
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;
    
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name; // e.g., "ADMINISTRATOR", "GUEST", "TEACHER"
    
    @Column(name = "description", length = 255)
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Role can have multiple permissions
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
```

#### 14.2.3 Permission Entity
**Location:** `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/entity/Permission.java`

```java
@Entity
@Table(name = "permissions")
@Data
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;
    
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name; // e.g., "READ_COURSE", "MODIFY_COURSE"
    
    @Column(name = "description", length = 255)
    private String description;
    
    @Column(name = "resource", length = 50)
    private String resource; // e.g., "COURSE", "QUIZ", "QUESTION"
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private PermissionAction action; // READ or MODIFY
}
```

#### 14.2.4 Permission Action Enum
**Location:** `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/entity/PermissionAction.java`

```java
public enum PermissionAction {
    READ,   // View, list operations
    MODIFY  // Create, update, delete operations
}
```

### 14.3 Database Schema

#### 14.3.1 Tables

**users table** (enhanced):
```sql
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    hashed_password VARCHAR(60) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

**roles table**:
```sql
CREATE TABLE roles (
    role_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**permissions table**:
```sql
CREATE TABLE permissions (
    permission_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    resource VARCHAR(50),
    action VARCHAR(10) NOT NULL CHECK (action IN ('READ', 'MODIFY'))
);
```

**user_roles table** (junction table):
```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);
```

**role_permissions table** (junction table):
```sql
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
);
```

#### 14.3.2 Default Permissions

The system initializes with these permissions:

| Permission Name | Resource | Action | Description |
|----------------|----------|--------|-------------|
| READ_COURSE | COURSE | READ | View and list courses |
| MODIFY_COURSE | COURSE | MODIFY | Create, edit, delete courses |
| READ_QUIZ | QUIZ | READ | View and list quizzes |
| MODIFY_QUIZ | QUIZ | MODIFY | Create, edit, delete quizzes |
| READ_QUESTION | QUESTION | READ | View and list questions |
| MODIFY_QUESTION | QUESTION | MODIFY | Create, edit, delete questions |
| READ_AUTHOR | AUTHOR | READ | View and list authors |
| MODIFY_AUTHOR | AUTHOR | MODIFY | Create, edit, delete authors |
| READ_USER | USER | READ | View and list users |
| MODIFY_USER | USER | MODIFY | Create, edit, delete users |
| READ_ROLE | ROLE | READ | View and list roles |
| MODIFY_ROLE | ROLE | MODIFY | Create, edit, delete roles |
| MODIFY_PERMISSION | PERMISSION | MODIFY | Assign permissions to roles |
| AI_TOOLS | AI | MODIFY | Access AI correction tools |
| UPLOAD_FILES | UPLOAD | MODIFY | Upload files and archives |

#### 14.3.3 Default Roles

**ADMINISTRATOR role:**
- Contains ALL permissions
- Assigned to admin user by default
- Full system access

**GUEST role:**
- Contains only READ permissions:
  - READ_COURSE
  - READ_QUIZ
  - READ_QUESTION
  - READ_AUTHOR
- Assigned to new users by default
- Read-only access

### 14.4 IAM Service Layer (myquiz-iam)

#### 14.4.1 Role Service
**Location:** `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/service/RoleService.java`

```java
public interface RoleService {
    Role createRole(String name, String description);
    List<Role> getAllRoles();
    Optional<Role> getRoleById(Long id);
    Optional<Role> getRoleByName(String name);
    Role updateRole(Long id, String name, String description);
    void deleteRole(Long id);
    Role addPermissionToRole(Long roleId, Long permissionId);
    Role removePermissionFromRole(Long roleId, Long permissionId);
    Set<Permission> getRolePermissions(Long roleId);
}
```

#### 14.4.2 Permission Service
**Location:** `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/service/PermissionService.java`

```java
public interface PermissionService {
    Permission createPermission(String name, String description, String resource, PermissionAction action);
    List<Permission> getAllPermissions();
    Optional<Permission> getPermissionById(Long id);
    Optional<Permission> getPermissionByName(String name);
    List<Permission> getPermissionsByResource(String resource);
    void deletePermission(Long id);
}
```

#### 14.4.3 User Service Enhancement
**Location:** `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/service/UsersService.java`

```java
// Additional methods:
User assignRoleToUser(Long userId, Long roleId);
User removeRoleFromUser(Long userId, Long roleId);
Set<Role> getUserRoles(Long userId);
Set<Permission> getUserPermissions(Long userId); // Merged from all roles
boolean hasPermission(Long userId, String permissionName);
```

#### 14.4.4 Repositories

**RoleRepository:**
```java
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
}
```

**PermissionRepository:**
```java
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
    List<Permission> findByResource(String resource);
}
```

#### 14.4.5 Data Initialization
**Location:** `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/config/RolePermissionInitializer.java`

Initializes default permissions, roles, and assigns administrator role to admin user:

```java
@Component
public class RolePermissionInitializer implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        // 1. Create permissions if not exist
        // 2. Create ADMINISTRATOR role with all permissions
        // 3. Create GUEST role with READ permissions
        // 4. Assign ADMINISTRATOR role to admin user
    }
}
```

### 14.5 Auth Service Layer (myquiz-auth)

#### 14.5.1 Enhanced JWT Token

**JwtUtil Enhancement:**
**Location:** `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/config/JwtUtil.java`

```java
// Enhanced generateToken method with roles and permissions
public String generateToken(String username, Set<String> roles, Set<String> permissions) {
    return Jwts.builder()
            .subject(username)
            .claim("roles", roles)
            .claim("permissions", permissions)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
            .signWith(getSecretKey())
            .compact();
}

// Extract roles from token
public Set<String> extractRoles(String token) {
    Claims claims = Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    List<String> rolesList = claims.get("roles", List.class);
    return new HashSet<>(rolesList);
}

// Extract permissions from token
public Set<String> extractPermissions(String token) {
    Claims claims = Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    List<String> permissionsList = claims.get("permissions", List.class);
    return new HashSet<>(permissionsList);
}
```

#### 14.5.2 Auth Service Enhancement
**Location:** `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/service/AuthService.java`

```java
// Enhanced login method
public LoginResponse login(String username, String password) {
    // 1. Validate credentials with IAM
    // 2. Get user roles from IAM
    // 3. Get merged permissions from all roles
    // 4. Generate JWT with username, roles, and permissions
    // 5. Return token with user info
}
```

#### 14.5.3 User Management Endpoints
**Location:** `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/controller/UserManagementController.java`

```java
@RestController
@RequestMapping("/api/auth/users")
public class UserManagementController {
    // User operations
    @GetMapping
    ResponseEntity<List<UserDTO>> getAllUsers();
    
    @GetMapping("/{id}")
    ResponseEntity<UserDTO> getUserById(@PathVariable Long id);
    
    @PutMapping("/{id}")
    ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request);
    
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteUser(@PathVariable Long id);
    
    @PutMapping("/{id}/enable")
    ResponseEntity<Void> enableUser(@PathVariable Long id);
    
    @PutMapping("/{id}/disable")
    ResponseEntity<Void> disableUser(@PathVariable Long id);
    
    // Role assignment
    @PostMapping("/{userId}/roles/{roleId}")
    ResponseEntity<Void> assignRole(@PathVariable Long userId, @PathVariable Long roleId);
    
    @DeleteMapping("/{userId}/roles/{roleId}")
    ResponseEntity<Void> removeRole(@PathVariable Long userId, @PathVariable Long roleId);
    
    @GetMapping("/{userId}/roles")
    ResponseEntity<Set<RoleDTO>> getUserRoles(@PathVariable Long userId);
}
```

#### 14.5.4 Role Management Endpoints
**Location:** `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/controller/RoleManagementController.java`

```java
@RestController
@RequestMapping("/api/auth/roles")
public class RoleManagementController {
    @GetMapping
    ResponseEntity<List<RoleDTO>> getAllRoles();
    
    @GetMapping("/{id}")
    ResponseEntity<RoleDTO> getRoleById(@PathVariable Long id);
    
    @PostMapping
    ResponseEntity<RoleDTO> createRole(@RequestBody RoleCreateRequest request);
    
    @PutMapping("/{id}")
    ResponseEntity<RoleDTO> updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request);
    
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteRole(@PathVariable Long id);
    
    // Permission assignment
    @PostMapping("/{roleId}/permissions/{permissionId}")
    ResponseEntity<Void> addPermission(@PathVariable Long roleId, @PathVariable Long permissionId);
    
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    ResponseEntity<Void> removePermission(@PathVariable Long roleId, @PathVariable Long permissionId);
    
    @GetMapping("/{roleId}/permissions")
    ResponseEntity<Set<PermissionDTO>> getRolePermissions(@PathVariable Long roleId);
}
```

#### 14.5.5 Permission Management Endpoints
**Location:** `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/controller/PermissionManagementController.java`

```java
@RestController
@RequestMapping("/api/auth/permissions")
public class PermissionManagementController {
    @GetMapping
    ResponseEntity<List<PermissionDTO>> getAllPermissions();
    
    @GetMapping("/{id}")
    ResponseEntity<PermissionDTO> getPermissionById(@PathVariable Long id);
    
    @GetMapping("/resource/{resource}")
    ResponseEntity<List<PermissionDTO>> getPermissionsByResource(@PathVariable String resource);
}
```

### 14.6 API Security (myquiz-app)

#### 14.6.1 Enhanced JWT Filter
**Location:** `myquiz-app/src/main/java/com/unitbv/myquiz/app/config/JwtFilter.java`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
    String jwt = extractJwt(request);
    if (jwt != null && jwtUtil.validateToken(jwt)) {
        String username = jwtUtil.extractUsername(jwt);
        Set<String> roles = jwtUtil.extractRoles(jwt);
        Set<String> permissions = jwtUtil.extractPermissions(jwt);
        
        // Create authorities from permissions
        List<GrantedAuthority> authorities = permissions.stream()
                .map(perm -> new SimpleGrantedAuthority(perm))
                .collect(Collectors.toList());
        
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        
        // Store roles and permissions in authentication details
        Map<String, Object> details = new HashMap<>();
        details.put("roles", roles);
        details.put("permissions", permissions);
        authentication.setDetails(details);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    chain.doFilter(request, response);
}
```

#### 14.6.2 Permission-based Authorization
**Location:** `myquiz-app/src/main/java/com/unitbv/myquiz/app/config/SecurityConfig.java`

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(authorize -> authorize
            // Public endpoints
            .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
            
            // Course endpoints
            .requestMatchers(HttpMethod.GET, "/api/courses/**").hasAuthority("READ_COURSE")
            .requestMatchers(HttpMethod.POST, "/api/courses/**").hasAuthority("MODIFY_COURSE")
            .requestMatchers(HttpMethod.PUT, "/api/courses/**").hasAuthority("MODIFY_COURSE")
            .requestMatchers(HttpMethod.DELETE, "/api/courses/**").hasAuthority("MODIFY_COURSE")
            
            // Quiz endpoints
            .requestMatchers(HttpMethod.GET, "/api/quizzes/**").hasAuthority("READ_QUIZ")
            .requestMatchers(HttpMethod.POST, "/api/quizzes/**").hasAuthority("MODIFY_QUIZ")
            .requestMatchers(HttpMethod.PUT, "/api/quizzes/**").hasAuthority("MODIFY_QUIZ")
            .requestMatchers(HttpMethod.DELETE, "/api/quizzes/**").hasAuthority("MODIFY_QUIZ")
            
            // Question endpoints
            .requestMatchers(HttpMethod.GET, "/api/questions/**").hasAuthority("READ_QUESTION")
            .requestMatchers(HttpMethod.POST, "/api/questions/**").hasAuthority("MODIFY_QUESTION")
            .requestMatchers(HttpMethod.PUT, "/api/questions/**").hasAuthority("MODIFY_QUESTION")
            .requestMatchers(HttpMethod.DELETE, "/api/questions/**").hasAuthority("MODIFY_QUESTION")
            
            // Author endpoints
            .requestMatchers(HttpMethod.GET, "/api/authors/**").hasAuthority("READ_AUTHOR")
            .requestMatchers(HttpMethod.POST, "/api/authors/**").hasAuthority("MODIFY_AUTHOR")
            .requestMatchers(HttpMethod.PUT, "/api/authors/**").hasAuthority("MODIFY_AUTHOR")
            .requestMatchers(HttpMethod.DELETE, "/api/authors/**").hasAuthority("MODIFY_AUTHOR")
            
            // Upload endpoints
            .requestMatchers("/api/upload/**").hasAuthority("UPLOAD_FILES")
            
            // AI tools
            .requestMatchers("/api/ai/**").hasAuthority("AI_TOOLS")
            
            // User management (admin only)
            .requestMatchers("/api/auth/users/**").hasAuthority("MODIFY_USER")
            .requestMatchers("/api/auth/roles/**").hasAuthority("MODIFY_ROLE")
            .requestMatchers("/api/auth/permissions/**").hasAuthority("MODIFY_PERMISSION")
            
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
}
```

### 14.7 Thymeleaf UI Layer (myquiz-thymeleaf)

#### 14.7.1 Menu Controller
**Location:** `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyMenuController.java`

```java
@Controller
public class ThyMenuController {
    private final SessionService sessionService;
    
    @ModelAttribute("menuPermissions")
    public Map<String, Boolean> getMenuPermissions(HttpSession session) {
        Map<String, Boolean> permissions = new HashMap<>();
        
        String token = (String) session.getAttribute("jwtToken");
        if (token == null) {
            return permissions; // Empty map for non-authenticated users
        }
        
        // Extract permissions from JWT
        Set<String> userPermissions = jwtUtil.extractPermissions(token);
        
        // Map permissions for menu items
        permissions.put("canReadCourse", userPermissions.contains("READ_COURSE"));
        permissions.put("canModifyCourse", userPermissions.contains("MODIFY_COURSE"));
        permissions.put("canReadQuiz", userPermissions.contains("READ_QUIZ"));
        permissions.put("canModifyQuiz", userPermissions.contains("MODIFY_QUIZ"));
        permissions.put("canReadQuestion", userPermissions.contains("READ_QUESTION"));
        permissions.put("canModifyQuestion", userPermissions.contains("MODIFY_QUESTION"));
        permissions.put("canReadAuthor", userPermissions.contains("READ_AUTHOR"));
        permissions.put("canModifyAuthor", userPermissions.contains("MODIFY_AUTHOR"));
        permissions.put("canUploadFiles", userPermissions.contains("UPLOAD_FILES"));
        permissions.put("canUseAiTools", userPermissions.contains("AI_TOOLS"));
        permissions.put("canManageUsers", userPermissions.contains("MODIFY_USER"));
        permissions.put("canManageRoles", userPermissions.contains("MODIFY_ROLE"));
        
        return permissions;
    }
}
```

#### 14.7.2 Enhanced Menu Fragment
**Location:** `myquiz-thymeleaf/src/main/resources/templates/fragments.html`

```html
<div th:fragment="menu" class="menu">
    <!-- Quizzes menu - visible if user can read quizzes -->
    <div th:if="${menuPermissions['canReadQuiz']}" class="main-menu-item" tabindex="0">
        <span>📝 Quizzes ▾</span>
        <div class="main-submenu">
            <a th:href="@{'/quiz/'}" aria-label="Quizzes List">Quizzes List</a>
            <a th:if="${menuPermissions['canModifyQuiz']}" th:href="@{'/quiz/new'}" aria-label="Add New Quiz">+ Add New Quiz</a>
        </div>
    </div>
    
    <!-- Authors menu - visible if user can read authors -->
    <div th:if="${menuPermissions['canReadAuthor']}" class="main-menu-item" tabindex="0">
        <span>👥 Authors ▾</span>
        <div class="main-submenu">
            <a th:href="@{'/authors/'}" aria-label="Authors Management">Authors Management</a>
            <a th:if="${menuPermissions['canModifyAuthor']}" th:href="@{'/authors/new'}" aria-label="Add New Author">+ Add New Author</a>
        </div>
    </div>
    
    <!-- Courses menu - visible if user can read courses -->
    <div th:if="${menuPermissions['canReadCourse']}" class="main-menu-item" tabindex="0">
        <span>📚 Courses ▾</span>
        <div class="main-submenu">
            <a th:href="@{'/courses/'}" aria-label="Course List">Course List</a>
            <a th:if="${menuPermissions['canModifyCourse']}" th:href="@{'/courses/new'}" aria-label="Add New Course">+ Add New Course</a>
        </div>
    </div>
    
    <!-- Upload menu - visible if user can upload files -->
    <div th:if="${menuPermissions['canUploadFiles']}" class="main-menu-item" tabindex="0">
        <span>📤 Upload ▾</span>
        <div class="main-submenu">
            <a th:href="@{'/uploads/excel-form'}" aria-label="Upload Excel">Excel</a>
            <a th:href="@{'/uploads/archive-form'}" aria-label="Upload Archive">Archive</a>
        </div>
    </div>
    
    <!-- AI Tools menu - visible if user has AI tools permission -->
    <div th:if="${menuPermissions['canUseAiTools']}" class="main-menu-item" tabindex="0">
        <span>🤖 AI Tools ▾</span>
        <div class="main-submenu">
            <a th:href="@{'/question-correction'}" aria-label="AI Correction">AI Correction</a>
            <a th:href="@{'/check-pdf'}" aria-label="Check PDF">Check PDF</a>
        </div>
    </div>
    
    <!-- Question menu - visible if user can read questions -->
    <div th:if="${menuPermissions['canReadQuestion']}" class="main-menu-item" tabindex="0">
        <span>🔧 Question ▾</span>
        <div class="main-submenu">
            <a th:href="@{'/questions/'}" aria-label="Question List">Question List</a>
            <a th:if="${menuPermissions['canModifyQuestion']}" th:href="@{'/questions/add?type=MULTICHOICE'}" aria-label="Add MC">+ Add MC</a>
            <a th:if="${menuPermissions['canModifyQuestion']}" th:href="@{'/questions/add?type=TRUEFALSE'}" aria-label="Add TF">+ Add TF</a>
        </div>
    </div>
    
    <!-- Administration menu - visible if user can manage users or roles -->
    <div th:if="${menuPermissions['canManageUsers'] or menuPermissions['canManageRoles']}" class="main-menu-item" tabindex="0">
        <span>⚙️ Administration ▾</span>
        <div class="main-submenu">
            <a th:if="${menuPermissions['canManageUsers']}" th:href="@{'/admin/users'}" aria-label="User Management">User Management</a>
            <a th:if="${menuPermissions['canManageRoles']}" th:href="@{'/admin/roles'}" aria-label="Role Management">Role Management</a>
            <a th:if="${menuPermissions['canManageRoles']}" th:href="@{'/admin/permissions'}" aria-label="Permission Management">Permission Management</a>
        </div>
    </div>
    
    <a th:href="@{'/help'}" class="main-menu-item" aria-label="Help">ℹ️ Help</a>
    
    <!-- User menu -->
    <div class="main-menu-item" tabindex="0" aria-label="User">
        <span th:if="${loggedInUser != null}"> <b th:text="${loggedInUser}"></b> ▾</span>
        <div class="main-submenu">
            <a th:if="${loggedInUser == null}" th:href="@{/auth/login}" aria-label="Login">Login</a>
            <a th:if="${loggedInUser == null}" th:href="@{/auth/register}" aria-label="Register">Register</a>
            <a th:if="${loggedInUser != null}" th:href="@{/auth/logout}" aria-label="Logout">Logout</a>
        </div>
    </div>
</div>
```

#### 14.7.3 User Management Controller
**Location:** `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyUserManagementController.java`

```java
@Controller
@RequestMapping("/admin/users")
public class ThyUserManagementController {
    
    @GetMapping
    public String listUsers(Model model, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;
        
        // Check MODIFY_USER permission
        if (!hasPermission(session, "MODIFY_USER")) {
            return "error/403"; // Forbidden
        }
        
        // Fetch users from auth service
        HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
        ResponseEntity<List<UserDTO>> response = restTemplate.exchange(
            authServiceUrl + "/api/auth/users",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<UserDTO>>() {}
        );
        
        model.addAttribute("users", response.getBody());
        return "admin/user-list";
    }
    
    @GetMapping("/new")
    public String showCreateUserForm(Model model, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;
        
        if (!hasPermission(session, "MODIFY_USER")) {
            return "error/403";
        }
        
        model.addAttribute("user", new UserDTO());
        return "admin/user-form";
    }
    
    @GetMapping("/{id}/edit")
    public String showEditUserForm(@PathVariable Long id, Model model, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;
        
        if (!hasPermission(session, "MODIFY_USER")) {
            return "error/403";
        }
        
        // Fetch user and available roles
        // ... implementation ...
        
        return "admin/user-edit";
    }
    
    @PostMapping("/{id}/roles/add")
    public String assignRole(@PathVariable Long id, @RequestParam Long roleId, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;
        
        if (!hasPermission(session, "MODIFY_USER")) {
            return "error/403";
        }
        
        // Call auth service to assign role
        HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
        restTemplate.exchange(
            authServiceUrl + "/api/auth/users/" + id + "/roles/" + roleId,
            HttpMethod.POST,
            entity,
            Void.class
        );
        
        return "redirect:/admin/users/" + id + "/edit";
    }
    
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;
        
        if (!hasPermission(session, "MODIFY_USER")) {
            return "error/403";
        }
        
        HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
        restTemplate.exchange(
            authServiceUrl + "/api/auth/users/" + id,
            HttpMethod.DELETE,
            entity,
            Void.class
        );
        
        return "redirect:/admin/users";
    }
    
    private boolean hasPermission(HttpSession session, String permission) {
        String token = (String) session.getAttribute("jwtToken");
        if (token == null) return false;
        Set<String> permissions = jwtUtil.extractPermissions(token);
        return permissions.contains(permission);
    }
}
```

#### 14.7.4 Role Management Controller
**Location:** `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyRoleManagementController.java`

```java
@Controller
@RequestMapping("/admin/roles")
public class ThyRoleManagementController {
    
    @GetMapping
    public String listRoles(Model model, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;
        
        if (!hasPermission(session, "MODIFY_ROLE")) {
            return "error/403";
        }
        
        // Fetch roles from auth service
        // ... implementation ...
        
        return "admin/role-list";
    }
    
    @GetMapping("/new")
    public String showCreateRoleForm(Model model, HttpSession session) {
        // ... implementation ...
        return "admin/role-form";
    }
    
    @GetMapping("/{id}/edit")
    public String showEditRoleForm(@PathVariable Long id, Model model, HttpSession session) {
        // Fetch role and available permissions
        // ... implementation ...
        return "admin/role-edit";
    }
    
    @PostMapping("/{id}/permissions/add")
    public String addPermissionToRole(@PathVariable Long id, @RequestParam Long permissionId, HttpSession session) {
        // Call auth service to add permission to role
        // ... implementation ...
        return "redirect:/admin/roles/" + id + "/edit";
    }
    
    @PostMapping("/{id}/permissions/remove")
    public String removePermissionFromRole(@PathVariable Long id, @RequestParam Long permissionId, HttpSession session) {
        // Call auth service to remove permission from role
        // ... implementation ...
        return "redirect:/admin/roles/" + id + "/edit";
    }
}
```

#### 14.7.5 Permission Management Controller
**Location:** `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyPermissionManagementController.java`

```java
@Controller
@RequestMapping("/admin/permissions")
public class ThyPermissionManagementController {
    
    @GetMapping
    public String listPermissions(Model model, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;
        
        if (!hasPermission(session, "MODIFY_ROLE")) {
            return "error/403";
        }
        
        // Fetch permissions from auth service
        HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
        ResponseEntity<List<PermissionDTO>> response = restTemplate.exchange(
            authServiceUrl + "/api/auth/permissions",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<PermissionDTO>>() {}
        );
        
        model.addAttribute("permissions", response.getBody());
        return "admin/permission-list";
    }
}
```

### 14.8 Thymeleaf Templates

#### 14.8.1 User Management Templates

**user-list.html** - List all users with actions:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>User Management</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}">
</head>
<body>
<div th:insert="~{fragments::menu}"></div>

<div class="container">
    <h1>User Management</h1>
    
    <a th:href="@{/admin/users/new}" class="btn btn-primary">+ Add New User</a>
    
    <table class="data-table">
        <thead>
            <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Email</th>
                <th>Roles</th>
                <th>Status</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="user : ${users}">
                <td th:text="${user.id}"></td>
                <td th:text="${user.username}"></td>
                <td th:text="${user.email}"></td>
                <td>
                    <span th:each="role : ${user.roles}" th:text="${role.name}" class="badge"></span>
                </td>
                <td>
                    <span th:if="${user.enabled}" class="badge badge-success">Active</span>
                    <span th:unless="${user.enabled}" class="badge badge-danger">Disabled</span>
                </td>
                <td>
                    <a th:href="@{/admin/users/{id}/edit(id=${user.id})}" class="btn btn-sm">Edit</a>
                    <form th:action="@{/admin/users/{id}/delete(id=${user.id})}" method="post" style="display:inline;">
                        <button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Delete this user?')">Delete</button>
                    </form>
                </td>
            </tr>
        </tbody>
    </table>
</div>
</body>
</html>
```

**user-edit.html** - Edit user and manage roles:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Edit User</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}">
</head>
<body>
<div th:insert="~{fragments::menu}"></div>

<div class="container">
    <h1>Edit User: <span th:text="${user.username}"></span></h1>
    
    <form th:action="@{/admin/users/{id}(id=${user.id})}" method="post" class="form">
        <div class="form-group">
            <label>Username:</label>
            <input type="text" name="username" th:value="${user.username}" required>
        </div>
        
        <div class="form-group">
            <label>Email:</label>
            <input type="email" name="email" th:value="${user.email}" required>
        </div>
        
        <div class="form-group">
            <label>Status:</label>
            <select name="enabled">
                <option value="true" th:selected="${user.enabled}">Active</option>
                <option value="false" th:selected="${!user.enabled}">Disabled</option>
            </select>
        </div>
        
        <button type="submit" class="btn btn-primary">Update User</button>
    </form>
    
    <hr>
    
    <h2>Assigned Roles</h2>
    <div class="roles-list">
        <div th:each="role : ${user.roles}" class="role-item">
            <span th:text="${role.name}" class="badge"></span>
            <form th:action="@{/admin/users/{id}/roles/remove(id=${user.id})}" method="post" style="display:inline;">
                <input type="hidden" name="roleId" th:value="${role.id}">
                <button type="submit" class="btn btn-sm btn-danger">Remove</button>
            </form>
        </div>
    </div>
    
    <h3>Add Role</h3>
    <form th:action="@{/admin/users/{id}/roles/add(id=${user.id})}" method="post" class="form-inline">
        <select name="roleId" required>
            <option value="">Select role...</option>
            <option th:each="role : ${availableRoles}" th:value="${role.id}" th:text="${role.name}"></option>
        </select>
        <button type="submit" class="btn btn-primary">Add Role</button>
    </form>
</div>
</body>
</html>
```

#### 14.8.2 Role Management Templates

**role-list.html** - List all roles:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Role Management</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}">
</head>
<body>
<div th:insert="~{fragments::menu}"></div>

<div class="container">
    <h1>Role Management</h1>
    
    <a th:href="@{/admin/roles/new}" class="btn btn-primary">+ Create New Role</a>
    
    <table class="data-table">
        <thead>
            <tr>
                <th>ID</th>
                <th>Role Name</th>
                <th>Description</th>
                <th>Permissions</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="role : ${roles}">
                <td th:text="${role.id}"></td>
                <td th:text="${role.name}"></td>
                <td th:text="${role.description}"></td>
                <td>
                    <span th:text="${role.permissions.size()} + ' permissions'"></span>
                </td>
                <td>
                    <a th:href="@{/admin/roles/{id}/edit(id=${role.id})}" class="btn btn-sm">Edit</a>
                    <form th:action="@{/admin/roles/{id}/delete(id=${role.id})}" method="post" style="display:inline;">
                        <button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Delete this role?')">Delete</button>
                    </form>
                </td>
            </tr>
        </tbody>
    </table>
</div>
</body>
</html>
```

**role-edit.html** - Edit role and manage permissions:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Edit Role</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}">
</head>
<body>
<div th:insert="~{fragments::menu}"></div>

<div class="container">
    <h1>Edit Role: <span th:text="${role.name}"></span></h1>
    
    <form th:action="@{/admin/roles/{id}(id=${role.id})}" method="post" class="form">
        <div class="form-group">
            <label>Role Name:</label>
            <input type="text" name="name" th:value="${role.name}" required>
        </div>
        
        <div class="form-group">
            <label>Description:</label>
            <textarea name="description" th:text="${role.description}"></textarea>
        </div>
        
        <button type="submit" class="btn btn-primary">Update Role</button>
    </form>
    
    <hr>
    
    <h2>Assigned Permissions</h2>
    <div class="permissions-grid">
        <div th:each="permission : ${role.permissions}" class="permission-item">
            <div class="permission-info">
                <strong th:text="${permission.name}"></strong>
                <span th:text="${permission.description}" class="text-muted"></span>
            </div>
            <form th:action="@{/admin/roles/{id}/permissions/remove(id=${role.id})}" method="post">
                <input type="hidden" name="permissionId" th:value="${permission.id}">
                <button type="submit" class="btn btn-sm btn-danger">Remove</button>
            </form>
        </div>
    </div>
    
    <h3>Add Permissions</h3>
    <form th:action="@{/admin/roles/{id}/permissions/add(id=${role.id})}" method="post" class="form-inline">
        <select name="permissionId" required>
            <option value="">Select permission...</option>
            <option th:each="permission : ${availablePermissions}" 
                    th:value="${permission.id}" 
                    th:text="${permission.name} + ' - ' + ${permission.description}"></option>
        </select>
        <button type="submit" class="btn btn-primary">Add Permission</button>
    </form>
</div>
</body>
</html>
```

#### 14.8.3 Permission List Template

**permission-list.html** - View all permissions:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Permission Management</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}">
</head>
<body>
<div th:insert="~{fragments::menu}"></div>

<div class="container">
    <h1>System Permissions</h1>
    
    <table class="data-table">
        <thead>
            <tr>
                <th>ID</th>
                <th>Permission Name</th>
                <th>Resource</th>
                <th>Action</th>
                <th>Description</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="permission : ${permissions}">
                <td th:text="${permission.id}"></td>
                <td th:text="${permission.name}"></td>
                <td>
                    <span th:text="${permission.resource}" class="badge badge-info"></span>
                </td>
                <td>
                    <span th:text="${permission.action}" 
                          th:class="${permission.action == 'READ' ? 'badge badge-success' : 'badge badge-warning'}"></span>
                </td>
                <td th:text="${permission.description}"></td>
            </tr>
        </tbody>
    </table>
</div>
</body>
</html>
```

#### 14.8.4 Error Page for 403 Forbidden

**error/403.html**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Access Denied</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}">
</head>
<body>
<div th:insert="~{fragments::menu}"></div>

<div class="container">
    <div class="error-page">
        <h1>🚫 Access Denied</h1>
        <p>You don't have permission to access this resource.</p>
        <p>If you believe this is an error, please contact your administrator.</p>
        <a th:href="@{/}" class="btn btn-primary">Return to Home</a>
    </div>
</div>
</body>
</html>
```

### 14.9 Data Transfer Objects (DTOs)

#### 14.9.1 Role DTOs
**Location:** `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/dto/`

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDTO {
    private Long id;
    private String name;
    private String description;
    private Set<PermissionDTO> permissions;
}

@Data
public class RoleCreateRequest {
    private String name;
    private String description;
}

@Data
public class RoleUpdateRequest {
    private String name;
    private String description;
}
```

#### 14.9.2 Permission DTOs

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDTO {
    private Long id;
    private String name;
    private String description;
    private String resource;
    private String action; // "READ" or "MODIFY"
}
```

#### 14.9.3 User DTOs (Enhanced)

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private Boolean enabled;
    private Set<RoleDTO> roles;
    private Set<String> permissions; // Merged from all roles
}

@Data
public class UserUpdateRequest {
    private String username;
    private String email;
    private Boolean enabled;
}
```

### 14.10 Database Migration Script

**Location:** `data/add-role-permission-tables.sql`

```sql
-- Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    permission_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    resource VARCHAR(50),
    action VARCHAR(10) NOT NULL CHECK (action IN ('READ', 'MODIFY'))
);

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    role_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create user_roles junction table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

-- Create role_permissions junction table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
);

-- Add enabled column to users table if not exists
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Insert default permissions
INSERT INTO permissions (name, description, resource, action) VALUES
('READ_COURSE', 'View and list courses', 'COURSE', 'READ'),
('MODIFY_COURSE', 'Create, edit, delete courses', 'COURSE', 'MODIFY'),
('READ_QUIZ', 'View and list quizzes', 'QUIZ', 'READ'),
('MODIFY_QUIZ', 'Create, edit, delete quizzes', 'QUIZ', 'MODIFY'),
('READ_QUESTION', 'View and list questions', 'QUESTION', 'READ'),
('MODIFY_QUESTION', 'Create, edit, delete questions', 'QUESTION', 'MODIFY'),
('READ_AUTHOR', 'View and list authors', 'AUTHOR', 'READ'),
('MODIFY_AUTHOR', 'Create, edit, delete authors', 'AUTHOR', 'MODIFY'),
('READ_USER', 'View and list users', 'USER', 'READ'),
('MODIFY_USER', 'Create, edit, delete users', 'USER', 'MODIFY'),
('READ_ROLE', 'View and list roles', 'ROLE', 'READ'),
('MODIFY_ROLE', 'Create, edit, delete roles', 'ROLE', 'MODIFY'),
('MODIFY_PERMISSION', 'Assign permissions to roles', 'PERMISSION', 'MODIFY'),
('AI_TOOLS', 'Access AI correction tools', 'AI', 'MODIFY'),
('UPLOAD_FILES', 'Upload files and archives', 'UPLOAD', 'MODIFY')
ON CONFLICT (name) DO NOTHING;

-- Insert default roles
INSERT INTO roles (name, description) VALUES
('ADMINISTRATOR', 'Full system access with all permissions'),
('GUEST', 'Read-only access to view data')
ON CONFLICT (name) DO NOTHING;

-- Assign all permissions to ADMINISTRATOR role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMINISTRATOR'
ON CONFLICT DO NOTHING;

-- Assign READ permissions to GUEST role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'GUEST' AND p.action = 'READ'
ON CONFLICT DO NOTHING;

-- Assign ADMINISTRATOR role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin' AND r.name = 'ADMINISTRATOR'
ON CONFLICT DO NOTHING;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_permissions_resource ON permissions(resource);
CREATE INDEX IF NOT EXISTS idx_permissions_action ON permissions(action);
```

### 14.11 Implementation Workflow

#### Phase 1: Database and IAM Layer
1. Run database migration script (`add-role-permission-tables.sql`)
2. Create Permission, Role entities in myquiz-iam
3. Create repositories for Permission, Role
4. Implement PermissionService, RoleService in myquiz-iam
5. Enhance UsersService with role management methods
6. Create RolePermissionInitializer for default data
7. Test IAM endpoints with Postman

#### Phase 2: Auth Service Enhancement
1. Enhance JwtUtil to include roles and permissions in JWT
2. Update AuthService login method to fetch roles/permissions
3. Create UserManagementController in myquiz-auth
4. Create RoleManagementController in myquiz-auth
5. Create PermissionManagementController in myquiz-auth
6. Create DTOs for roles, permissions, and user management
7. Test auth endpoints with Postman

#### Phase 3: API Security
1. Enhance JwtFilter in myquiz-app to extract permissions
2. Update SecurityConfig with permission-based authorization
3. Add @PreAuthorize annotations to controllers (optional)
4. Test endpoint protection (403 responses)
5. Add error handler for 403 Forbidden

#### Phase 4: Thymeleaf UI
1. Create ThyMenuController with permission checking
2. Update fragments.html menu with permission filtering
3. Create ThyUserManagementController
4. Create ThyRoleManagementController
5. Create ThyPermissionManagementController
6. Create user management templates (list, edit, form)
7. Create role management templates (list, edit, form)
8. Create permission list template
9. Create 403 error page
10. Test UI navigation and permission filtering

#### Phase 5: Integration Testing
1. Test login with admin user (should have all permissions)
2. Create test user with GUEST role (read-only)
3. Verify menu filtering for different roles
4. Verify endpoint protection (403 for unauthorized access)
5. Test role assignment and permission management UI
6. Test JWT token contains correct roles and permissions
7. Verify permission changes reflect immediately after re-login

### 14.12 Security Considerations

#### 14.12.1 JWT Token Security
- Keep JWT expiration time reasonable (1 hour default)
- Force re-login after role/permission changes
- Use HTTPS in production to protect token transmission
- Store JWT secret securely (environment variables)

#### 14.12.2 Permission Checking
- Always check permissions at both UI and API levels
- Never rely on UI hiding alone for security
- Return 403 Forbidden for unauthorized access attempts
- Log unauthorized access attempts for auditing

#### 14.12.3 Role Management
- Prevent deletion of ADMINISTRATOR role
- Prevent last administrator from being deleted
- Prevent users from modifying their own roles (admin UI only)
- Validate role assignments on server side

#### 14.12.4 Session Management
- Invalidate sessions on role changes
- Require re-authentication for sensitive operations
- Implement session timeout
- Clear JWT from session on logout

### 14.13 Testing Scenarios

#### 14.13.1 Administrator User
- ✅ Can access all menu items
- ✅ Can view all pages
- ✅ Can perform all CRUD operations
- ✅ Can access user/role management
- ✅ JWT contains all permissions

#### 14.13.2 Guest User
- ✅ Can access read-only menu items
- ✅ Can view list pages
- ✅ Cannot see "Add New" buttons
- ❌ Cannot access edit/delete endpoints (403)
- ❌ Cannot access admin pages (403)
- ✅ JWT contains only READ permissions

#### 14.13.3 Custom Role User
- ✅ Menu filtered based on assigned permissions
- ✅ Can access only authorized endpoints
- ❌ Gets 403 for unauthorized endpoints
- ✅ JWT contains merged permissions from all assigned roles

### 14.14 Performance Optimization

#### 14.14.1 Database Indexes
- Index on user_roles(user_id) for fast role lookup
- Index on role_permissions(role_id) for permission lookup
- Index on permissions(resource) for filtering

#### 14.14.2 Caching Strategy
- Cache user permissions in JWT (avoid DB lookup per request)
- Cache role-permission mappings
- Invalidate cache on role/permission changes

#### 14.14.3 Query Optimization
- Use EAGER fetching for roles and permissions (avoid N+1)
- Batch load permissions for multiple roles
- Use JOIN queries instead of multiple queries

### 14.15 Future Enhancements

#### 14.15.1 Advanced Features
- **Permission Groups**: Group related permissions for easier management
- **Permission Inheritance**: Roles inherit permissions from parent roles
- **Time-based Permissions**: Temporary role assignments with expiration
- **IP Restrictions**: Restrict access based on IP address
- **Audit Log**: Track all permission changes and access attempts

#### 14.15.2 UI Improvements
- **Bulk Operations**: Assign roles to multiple users at once
- **Permission Matrix**: Visual grid showing role-permission assignments
- **User Search/Filter**: Search users by name, email, or role
- **Activity Dashboard**: Show user activity and access patterns

### 14.16 Implementation Checklist

Use this checklist to verify the implementation of the role and permission management system at each phase.

#### Phase 1: Database & IAM Layer

**Database Migration:**
- [ ] Run migration script: `psql -U postgres -d myquiz -f data/add-role-permission-tables.sql`
- [ ] Verify tables created: `permissions`, `roles`, `user_roles`, `role_permissions`
- [ ] Verify permissions inserted: 15 permissions
- [ ] Verify roles created: ADMINISTRATOR, CONTENT_MANAGER, GUEST, TEACHER
- [ ] Verify admin has ADMINISTRATOR role
- [ ] Run verification script: `data/verify-role-permission-system.sql`

**Entity Classes:**
- [ ] PermissionAction enum created
- [ ] Permission entity created
- [ ] Role entity created
- [ ] User entity updated with roles relationship

**Repositories:**
- [ ] PermissionRepository created with findByName, findByResource
- [ ] RoleRepository created with findByName, existsByName

**Services:**
- [ ] PermissionService interface and implementation
- [ ] RoleService interface and implementation
- [ ] UsersService updated with role/permission methods

**Controllers:**
- [ ] RoleController created with CRUD endpoints
- [ ] PermissionController created with GET endpoints
- [ ] UsersController updated with role assignment endpoints

#### Phase 2: Auth Service Enhancement

**JWT Enhancement:**
- [ ] JwtUtil updated to include roles/permissions in token
- [ ] AuthService fetches user roles and permissions from IAM
- [ ] Login method generates JWT with roles and permissions

**DTOs:**
- [ ] RoleDTO created
- [ ] PermissionDTO created
- [ ] UserDTO updated with roles and permissions

**Controllers:**
- [ ] UserManagementController proxies to IAM
- [ ] RoleManagementController proxies to IAM
- [ ] PermissionManagementController proxies to IAM

#### Phase 3: API Security

**JWT Filter Enhancement:**
- [ ] JwtFilter extracts roles from JWT
- [ ] JwtFilter extracts permissions from JWT
- [ ] Creates authorities from permissions
- [ ] Stores roles/permissions in authentication details

**Security Configuration:**
- [ ] SecurityConfig updated with permission-based rules
- [ ] Course endpoints protected: READ_COURSE / MODIFY_COURSE
- [ ] Quiz endpoints protected: READ_QUIZ / MODIFY_QUIZ
- [ ] Question endpoints protected: READ_QUESTION / MODIFY_QUESTION
- [ ] Author endpoints protected: READ_AUTHOR / MODIFY_AUTHOR
- [ ] Admin endpoints protected: MODIFY_USER / MODIFY_ROLE

#### Phase 4: Thymeleaf UI

**Menu Filtering:**
- [ ] ThyMenuController created with @ModelAttribute for permissions
- [ ] fragments.html updated with permission-based conditionals
- [ ] Menu items show/hide based on permissions

**Admin Interface:**
- [ ] ThyUserManagementController and templates
- [ ] ThyRoleManagementController and templates
- [ ] ThyPermissionManagementController and templates
- [ ] 403 error page created

#### Phase 5: Integration Testing

**Test Scenarios:**
- [ ] Admin user can access all features
- [ ] Guest user has read-only access
- [ ] Teacher role works correctly
- [ ] Permission changes reflect after re-login
- [ ] JWT contains correct roles and permissions
- [ ] 403 errors handled properly

### 14.17 Quick Reference Diagrams

#### System Architecture
```
┌─────────────────────────────────────────────────────────────────┐
│                        MyQuiz Application                        │
└─────────────────────────────────────────────────────────────────┘
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
           ┌────────▼─────┐  ┌──▼────────┐  ┌▼────────────┐
           │ Thymeleaf UI │  │   Auth    │  │     App     │
           │   (Port 8082)│  │(Port 8081)│  │ (Port 8080) │
           └────────┬─────┘  └──┬────────┘  └┬────────────┘
                    │           │            │
                    │           │            │
                    │      ┌────▼────────┐   │
                    │      │    IAM      │   │
                    │      │ (Port 8888) │   │
                    │      └────┬────────┘   │
                    │           │            │
                    └───────────┼────────────┘
                                │
                        ┌───────▼────────┐
                        │   PostgreSQL   │
                        │   (Port 5432)  │
                        └────────────────┘
```

#### Login Flow with Roles/Permissions
```
User Login
   │
   ├─1─> Thymeleaf: POST /auth/login (username, password)
   │
   ├─2─> Auth Service: POST /api/auth/login
   │     │
   │     ├─3─> IAM Service: GET /api/users/find/{username}
   │     │     Returns: User + Roles
   │     │
   │     ├─4─> IAM Service: GET /api/users/{id}/permissions
   │     │     Returns: Merged permissions from all roles
   │     │
   │     ├─5─> Generate JWT with:
   │     │     - subject: username
   │     │     - claim "roles": ["ADMINISTRATOR"]
   │     │     - claim "permissions": ["READ_COURSE", "MODIFY_COURSE", ...]
   │     │
   │     └─6─> Return: JWT token
   │
   ├─7─> Store JWT in session
   │
   └─8─> Redirect to home page
```

#### API Request with Permission Check
```
User Action (e.g., Edit Course)
   │
   ├─1─> Thymeleaf: GET /courses/123/edit
   │
   ├─2─> App API: GET /api/courses/123
   │     Header: Authorization: Bearer <JWT>
   │
   ├─3─> JwtFilter intercepts request
   │     - Extract permissions from JWT
   │     - Create Authentication with authorities
   │
   ├─4─> SecurityConfig checks permission
   │     - Endpoint requires: MODIFY_COURSE
   │     - User has: ["READ_COURSE", "MODIFY_COURSE", ...]
   │     - ✅ ALLOWED or ❌ 403 FORBIDDEN
   │
   └─5─> Return course data or 403 error
```

#### Database Entity Relationships
```
┌─────────────┐
│    users    │
│─────────────│
│ user_id (PK)│───┐
│ username    │   │ Many-to-Many
│ email       │   │
│ enabled     │   │
└─────────────┘   │
                  │
        ┌─────────▼─────────┐
        │    user_roles     │
        │───────────────────│
        │ user_id (FK)      │
        │ role_id (FK)      │
        └─────────┬─────────┘
                  │
┌─────────────────▼──┐
│       roles        │
│────────────────────│
│ role_id (PK)       │───┐
│ name               │   │ Many-to-Many
│ description        │   │
└────────────────────┘   │
                         │
               ┌─────────▼──────────┐
               │  role_permissions  │
               │────────────────────│
               │ role_id (FK)       │
               │ permission_id (FK) │
               └─────────┬──────────┘
                         │
             ┌───────────▼─────────┐
             │    permissions      │
             │─────────────────────│
             │ permission_id (PK)  │
             │ name                │
             │ resource            │
             │ action (READ/MODIFY)│
             └─────────────────────┘
```

#### Permission Hierarchy
```
Resources
   │
   ├─ COURSE
   │  ├─ READ_COURSE    → View, List
   │  └─ MODIFY_COURSE  → Create, Edit, Delete
   │
   ├─ QUIZ
   │  ├─ READ_QUIZ      → View, List
   │  └─ MODIFY_QUIZ    → Create, Edit, Delete
   │
   ├─ QUESTION
   │  ├─ READ_QUESTION  → View, List
   │  └─ MODIFY_QUESTION → Create, Edit, Delete
   │
   ├─ AUTHOR
   │  ├─ READ_AUTHOR    → View, List
   │  └─ MODIFY_AUTHOR  → Create, Edit, Delete
   │
   ├─ USER
   │  ├─ READ_USER      → View, List
   │  └─ MODIFY_USER    → Create, Edit, Delete, Assign Roles
   │
   ├─ ROLE
   │  ├─ READ_ROLE      → View, List
   │  └─ MODIFY_ROLE    → Create, Edit, Delete, Assign Permissions
   │
   ├─ PERMISSION
   │  └─ MODIFY_PERMISSION → Assign Permissions to Roles
   │
   ├─ AI
   │  └─ AI_TOOLS       → Access AI Correction Tools
   │
   └─ UPLOAD
      └─ UPLOAD_FILES   → Upload Excel, Archives
```

### 14.18 Implementation Resources

#### Database Scripts Location
- `data/add-role-permission-tables.sql` - Creates tables, permissions, roles
- `data/verify-role-permission-system.sql` - Verification queries
- `data/add-performance-indexes.sql` - Performance indexes for role/permission tables

#### Documentation Files
- `data/VERIFICATION-CHECKLIST.md` - Detailed implementation checklist
- `data/ROLE-PERMISSION-SUMMARY.md` - Feature summary and overview
- `data/ROLE-PERMISSION-QUICK-REFERENCE.md` - Visual diagrams and quick reference
- `data/README-role-permission-system.md` - Step-by-step implementation guide with code

#### Testing Commands

**Test IAM Endpoints:**
```bash
# Get all roles
curl http://localhost:8888/api/roles

# Get all permissions
curl http://localhost:8888/api/permissions

# Get user permissions
curl http://localhost:8888/api/users/1/permissions
```

**Test Auth with JWT:**
```bash
# Login as admin
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.token')

# Decode JWT to see roles/permissions
echo $TOKEN | jwt decode -

# Test protected endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/courses
```

**Test Permission-Based Access:**
```bash
# Create guest user
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"guest1","email":"guest1@test.com","password":"test123"}'

# Login as guest
GUEST_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"guest1","password":"test123"}' | jq -r '.token')

# Test READ access (should work)
curl -H "Authorization: Bearer $GUEST_TOKEN" http://localhost:8080/api/courses

# Test MODIFY access (should get 403)
curl -X POST -H "Authorization: Bearer $GUEST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Course"}' \
  http://localhost:8080/api/courses
```

---

**Status:** ✅ Production Ready

Complete role-based access control system with JWT-based permissions, UI menu filtering, API endpoint protection, and comprehensive admin interface for managing users, roles, and permissions.
