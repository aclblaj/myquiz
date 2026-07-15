# Question Error Software Design
## 1. Overview
This document defines the current error-listing feature used to inspect, resolve, and delete validation errors associated with imported or edited questions.
The active implementation is question-error centric and is backed by `ThyErrorController`, `QuestionErrorController`, and `QuestionErrorService`.
## 2. Functional Scope
### 2.1 Main Features
- list and filter errors by course, author, and question bank
- paginate server-side results
- mark individual errors as resolved
- delete individual errors
- preserve filter context while navigating and mutating rows
### 2.2 Main End-to-End Calls
| Operation | Template | Thymeleaf Route | Backend Route | Service Entry Point |
|---|---|---|---|---|
| List/filter errors | `error-list.html` | `GET /errors`, `POST /errors/filter` | `POST /api/errors/filter` | `QuestionErrorService.filter(...)` |
| Resolve error | `error-list.html` | `POST /errors/{id}/resolve` | `PUT /api/errors/{id}/resolve` | `QuestionErrorService.resolveErrorById(...)` |
| Delete error | `error-list.html` | `POST /errors/{id}/delete` | `DELETE /api/errors/{id}` | `QuestionErrorService.deleteErrorById(...)` |
| Get single error | — | — | `GET /api/errors/{id}` | `QuestionErrorService.getErrorById(...)` |
## 3. Architecture
### 3.1 Layering
1. `error-list.html` submits filters or row actions.
2. `ThyErrorController` validates session and builds API requests.
3. `QuestionErrorController` exposes `/api/errors` endpoints.
4. `QuestionErrorService` performs filtering and status changes.
## 4. Data Model and DTOs
Primary DTOs in the current flow:
- `QuestionErrorDto`
- `QuestionErrorFilterRequestDto`
- `QuestionErrorFilterResponseDto`
## 5. Flows
### 5.1 List and Filter
1. UI hits `GET /errors` or posts to `/errors/filter`.
2. `ThyErrorController.renderErrorList(...)` normalizes empty author values and paging defaults.
3. Controller sends `QuestionErrorFilterRequestDto` to `POST /api/errors/filter`.
4. Backend returns `QuestionErrorFilterResponseDto`.
5. UI renders the current page and preserves filter selections.
### 5.2 Resolve Error
1. User submits `POST /errors/{id}/resolve`.
2. Thymeleaf controller forwards authorized `PUT /api/errors/{id}/resolve`.
3. Success or failure is returned as flash messaging.
4. User is redirected back to the same filtered page context.
### 5.3 Delete Error
1. User submits `POST /errors/{id}/delete`.
2. Thymeleaf controller forwards authorized `DELETE /api/errors/{id}`.
3. Flash message is attached.
4. Redirect restores filter context.
## 6. Permissions and Security
- Thymeleaf route entry requires a valid session through `SessionService`
- backend authorization is still enforced by API security
- forbidden backend responses are converted into user-facing error messages
## 7. UI, API, and Service Responsibilities
### 7.1 `ThyErrorController`
- normalize request params
- create `QuestionErrorFilterRequestDto`
- preserve back URLs and filter state
- handle flash messages for resolve/delete actions
### 7.2 `QuestionErrorController`
- map HTTP endpoints to service actions
- return `400`, `404`, `204`, or `500` as appropriate
### 7.3 `QuestionErrorService`
- build filtered/paged result sets
- update status to resolved
- delete rows safely
- populate dropdown metadata required by the UI
## 8. Validation and Error Handling
- invalid filter payloads produce `400`
- unknown error IDs return `404`
- page/pageSize are normalized to valid positive values in Thymeleaf flow
- list fallback model is rendered when API responses fail or return empty/null unexpectedly
## 9. Key Decisions
- use a POST filter endpoint so the filter contract can evolve without query-string sprawl
- keep row actions on POST routes in Thymeleaf even when backend uses PUT/DELETE
- keep back-link reconstruction explicit so filter context survives row actions
## 10. Implementation Notes
- Thymeleaf controller:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyErrorController.java`
- API controller:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/controller/QuestionErrorController.java`
- Related service:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/services/QuestionErrorService.java`
- Main template:
  - `myquiz-thymeleaf/src/main/resources/templates/error-list.html`
Related docs:
- `prompt/author-sd.md`
- `prompt/question-sd.md`
