# Core Ecosystem Software Design
## 1. Overview
This document defines the cross-cutting architecture of the MyQuiz ecosystem and the shared patterns that apply across feature-specific software design documents.
It covers:
- module boundaries
- request flow between services
- shared DTO and controller patterns
- menu and session behavior in the UI layer
- common error-handling and filtering conventions
## 2. Functional Scope
This is a platform-level document rather than a single feature document. It describes how major features fit together across the repository.
## 3. Architecture
### 3.1 Active Modules
- `myquiz-api`
- `myquiz-app`
- `myquiz-thymeleaf`
- `myquiz-auth`
- `myquiz-iam`
- `postgres`
### 3.2 Boundary Rules
- `myquiz-thymeleaf` talks to `myquiz-app` for business features and to `myquiz-auth` for authentication/admin-auth features.
- `myquiz-thymeleaf` should not call `myquiz-iam` directly.
- `myquiz-auth` is the only service that should call `myquiz-iam` for auth/admin identity concerns.
- `myquiz-api` contains contracts, not business logic.
### 3.3 Common Request Paths
- GUI feature flow: browser → `myquiz-thymeleaf` → `myquiz-app`
- authentication flow: browser → `myquiz-thymeleaf` → `myquiz-auth` → `myquiz-iam`
- admin identity flow: browser → `myquiz-thymeleaf` → `myquiz-auth` → `myquiz-iam`
## 4. Shared Data Model and Contract Patterns
### 4.1 DTO Strategy
Feature modules exchange DTOs from `myquiz-api`.
Common patterns:
- request DTOs for filters and form-like API calls
- response DTOs for paged/filter screens
- lightweight info DTOs for dropdowns and cross-feature lookups
- `ControllerSettings` constants reused by multiple controllers and templates
### 4.2 Persistence Patterns
`myquiz-app` typically follows:
- controller
- service
- repository
- specification (when filtering is needed)
- DTO mapping
## 5. Flows
### 5.1 UI Feature Flow
1. Thymeleaf controller validates session through `SessionService`.
2. Controller builds authorized request to API.
3. Backend controller delegates to service.
4. Service performs filtering, retrieval, mutation, or export logic.
5. DTO response is rendered in the template or streamed as a file.
### 5.2 Filtered List Pattern
Most filtered pages follow this structure:
1. UI submits filter criteria.
2. Thymeleaf controller normalizes defaults.
3. Backend accepts a request DTO or query params.
4. Service applies specification/repository filtering.
5. Response includes both result rows and enough metadata to repaint the filter UI.
### 5.3 Mutation Pattern
For create/update/delete/resolve actions:
1. Thymeleaf uses POST routes even when backend uses PUT/DELETE.
2. Backend returns status-oriented responses.
3. UI redirects with flash messages and preserves navigation context.
## 6. Permissions and Security
- UI session state is managed in `SessionService`.
- JWT tokens are issued by `myquiz-auth` and stored in server-side session state in `myquiz-thymeleaf`.
- Menu visibility is assembled in `ThyMenuController` using role/permission claims.
- Feature controllers still perform explicit checks where needed.
## 7. UI, API, and Service Responsibilities
### 7.1 Thymeleaf Layer
- route browser requests
- manage session validation and redirects
- call downstream APIs with authorization headers
- render templates and download responses
### 7.2 API Controllers
- expose REST endpoints
- perform HTTP-level validation and response shaping
- delegate domain logic to services
### 7.3 Services
- implement business rules
- own transactions for destructive or multi-step flows
- aggregate data into DTO-ready shapes
## 8. Validation and Error Handling
Common system-wide conventions:
- invalid user input should map to `400`-class behavior where applicable
- missing resources should map to `404`
- unexpected service failures should map to `500`
- Thymeleaf should prefer redirects or safe fallback pages instead of blank error screens
- session expiry should redirect users to login consistently
## 9. Key Decisions
- keep contracts centralized in `myquiz-api`
- keep GUI logic out of `myquiz-app`, `myquiz-auth`, and `myquiz-iam`
- keep auth/IAM responsibilities separate from domain feature responsibilities
- prefer reusable filter DTOs and shared constants over duplicated controller literals
- use feature-specific `*-sd.md` files for detailed behavior, not this document
## 10. Implementation Notes
- Shared constants and routes:
  - `myquiz-api/src/main/java/com/unitbv/myquiz/api/settings/ControllerSettings.java`
- Menu assembly:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyMenuController.java`
- Session helper:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/service/SessionService.java`
Related docs:
- `prompt/auth-sd.md`
- `prompt/admin-interface-sd.md`
- `prompt/question-bank-sd.md`
- `prompt/question-sd.md`
- `prompt/upload-sd.md`
- `prompt/duplicate-sd.md`
- `prompt/data-cleanup-sd.md`
