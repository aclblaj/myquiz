# Authentication and Session Software Design
## 1. Overview
This document defines the current authentication, registration, session, JWT, and authorization flow for MyQuiz.
Authentication spans three active modules:
- `myquiz-thymeleaf` for login/register/logout pages and session-backed UI access
- `myquiz-auth` for token issuance and auth-specific orchestration
- `myquiz-iam` for user, role, and permission persistence
## 2. Functional Scope
### 2.1 Main Features
- user login through `ThyAuthController`
- user registration with admin activation flow
- logout through session completion
- centralized JWT/session helpers through `SessionService`
- permission and role propagation into Thymeleaf menu visibility and page-level checks
- username availability and auth health endpoints in `myquiz-auth`
### 2.2 Main Routes
| Operation | Thymeleaf Route | Auth Route | IAM Responsibility |
|---|---|---|---|
| Show login | `GET /auth/login` | — | — |
| Login | `POST /auth/login` | `POST /api/auth/login` | load user details and role/permission data |
| Show register | `GET /auth/register` | — | — |
| Register | `POST /auth/register` | `POST /api/auth/register` | create user and assign default guest role |
| Logout | `GET /auth/logout` | — | — |
| Health | — | `GET /api/auth/health` | — |
| Username available | — | `GET /api/auth/username-available` | lookup username in IAM |
## 3. Architecture
### 3.1 Authentication Boundary
The UI does not talk to IAM directly for authentication tasks.
Canonical path:
1. user interacts with `myquiz-thymeleaf`
2. `ThyAuthController` calls `myquiz-auth`
3. `myquiz-auth` validates or creates users via `myquiz-iam`
4. `myquiz-auth` returns auth outcome and token information
5. UI stores JWT token in the server-side session
### 3.2 Session Model
The web layer stores:
- logged-in user name in session/model attributes
- JWT token in the HTTP session
`SessionService` retrieves the current request session lazily from `RequestContextHolder`, avoiding controller-specific session plumbing across the rest of the UI.
## 4. Data Model and Contracts
### 4.1 Auth Payloads
Current auth flow uses request/response objects or map-based payloads around:
- identifier / username
- password
- email for registration
- JWT token on successful login
- optional informational messages for registration and activation status
### 4.2 JWT Contents
`myquiz-auth` generates tokens containing:
- username
- roles
- permissions
These claims drive both backend authorization and Thymeleaf-side menu/page decisions.
## 5. Flows
### 5.1 Login
1. User submits login form to `POST /auth/login`.
2. `ThyAuthController` posts JSON credentials to `AUTH_API_URL + "/login"`.
3. `AuthController` loads the user from IAM.
4. Password hash is verified.
5. Enabled flag is checked.
6. User roles and permissions are fetched from IAM.
7. JWT token is generated and returned.
8. Thymeleaf stores token in session and redirects home.
Primary outcomes:
- `200` with token for successful login
- `403` for inactive user awaiting activation
- `401` for invalid password
- `404` for missing user
### 5.2 Registration
1. User submits registration form to `POST /auth/register`.
2. `ThyAuthController` forwards request to `myquiz-auth`.
3. `AuthController` checks username/email uniqueness through IAM.
4. Password is encoded.
5. User is created through IAM.
6. Default guest role is assigned.
7. Registration succeeds without immediate active login; user waits for admin activation.
### 5.3 Logout
1. User hits `GET /auth/logout`.
2. `ThyAuthController.logout(...)` calls `SessionStatus.setComplete()`.
3. UI returns to home route, which then behaves as unauthenticated.
### 5.4 Session Reuse in Other Controllers
Protected Thymeleaf controllers typically call one of these helpers first:
- `containsValidVars()`
- `validateSessionOrRedirect()`
- `createAuthorizedRequest(...)`
- `createMultipartRequest(...)`
This creates a uniform authentication gateway across UI controllers.
## 6. Permissions and Security
- `myquiz-auth` is the source of token generation.
- `myquiz-iam` remains the source of truth for users, roles, and permissions.
- `SessionService` can inspect token claims for:
  - admin-role presence
  - explicit permission presence
- Thymeleaf menu visibility and admin/action routing rely on token-derived permissions rather than repeated lookup calls.
Important rule:
- no feature controller should construct ad hoc authorization headers when `SessionService` already provides the helper method for that use case.
## 7. UI, API, and Service Responsibilities
### 7.1 `ThyAuthController`
- render login/register pages
- forward form submissions to `myquiz-auth`
- store or clear session state
- surface login/registration errors to the user
### 7.2 `SessionService`
- centralize session access and validation
- create authorized HTTP entities for JSON and multipart requests
- invalidate expired sessions
- expose convenience checks like admin role and named permission lookup
### 7.3 `AuthController`
- handle login, registration, health, and username-availability endpoints
- retrieve user details from IAM
- verify password hashes
- generate JWT tokens with roles and permissions
## 8. Validation and Error Handling
- invalid or blank tokens cause protected UI routes to redirect to login
- failed login keeps the user on the login screen with error flags/messages
- inactive users receive a dedicated activation message path
- registration errors stay on the register screen and surface the returned message where available
- downstream IAM failures are translated into `400`/`500` class auth responses rather than exposing internal details directly
## 9. Key Decisions
- keep IAM access behind `myquiz-auth`
- centralize all session/JWT mechanics in `SessionService`
- use session-backed server-side auth for Thymeleaf instead of storing auth state in browser-only logic
- include permissions in JWT to avoid excessive repeated authorization lookups across UI requests
- keep admin activation in the registration lifecycle rather than auto-enabling new users
## 10. Implementation Notes
- Thymeleaf auth controller:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyAuthController.java`
- Session helper:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/service/SessionService.java`
- Auth API controller:
  - `myquiz-auth/src/main/java/com.unitbv.myquiz.auth/controller/AuthController.java`
- Related admin controllers:
  - `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/controller/UserManagementController.java`
  - `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/controller/RoleManagementController.java`
  - `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/controller/PermissionManagementController.java`
Related docs:
- `prompt/admin-interface-sd.md`
- `prompt/core-sd.md`
