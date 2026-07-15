# Author Software Design
## 1. Overview
This document defines the current author-management design in MyQuiz.
Author functionality spans:
- author CRUD and filtering
- author detail views
- author-aware question bank and question navigation
- cached author lookup data used across filters and forms
## 2. Functional Scope
### 2.1 Main Features
- list authors and filter by course or criteria
- create and update authors
- delete authors when allowed by business rules
- show author details and author-related navigation into question bank/question flows
- provide lightweight author lookup data for filters and dropdowns
### 2.2 Main End-to-End Calls
| Operation | Thymeleaf Route | Backend Route | Service Entry Point |
|---|---|---|---|
| List/filter authors | `/authors/...` | `/api/authors...` | `AuthorService` filtering/list methods |
| View author | `/authors/{id}` | `/api/authors/{id}` | `AuthorService.getAuthorById(...)` |
| Create author | `/authors/...` | `POST /api/authors` | `AuthorService.createAuthor(...)` |
| Update author | `/authors/...` | `PUT /api/authors/{id}` | `AuthorService.updateAuthor(...)` |
| Delete author | `/authors/...` | `DELETE /api/authors/{id}` | `AuthorService.deleteAuthor(...)` |
| Course-scoped lookup | UI helper routes / filters | `/api/authors/course/...` | `AuthorService.getAuthorsByCourse(...)` |
## 3. Architecture
### 3.1 Main Components
- `AuthorController` in `myquiz-app`
- `ThyAuthorController` in `myquiz-thymeleaf`
- `AuthorService` in `myquiz-app`
- author templates such as `author-list.html`, `author-edit.html`, and `author-details.html`
### 3.2 Relationship Model
Authors contribute content through `QuestionBankAuthor`, which links:
- one `Author`
- one `QuestionBank`
- many `Question`
This means author data is used both directly in author screens and indirectly in question bank, question, upload, and duplicate-management flows.
## 4. Data Model and DTOs
Common author-related DTOs include:
- `AuthorDto` for full author records
- `AuthorInfo` for lightweight id/name/initials lookups
- filter DTOs used by author list and related dependent dropdown flows
## 5. Flows
### 5.1 List and Filter
1. Thymeleaf author page collects filter inputs.
2. Authorized REST calls are sent to author endpoints.
3. Service layer applies repository/specification-based filtering.
4. Result list and supporting filter data are returned to the template.
### 5.2 Details and Navigation
1. User opens an author detail screen.
2. Backend returns the author plus linked question-bank/question context.
3. UI exposes navigation into related feature areas rather than duplicating those flows.
### 5.3 Create / Update / Delete
- create and update are standard CRUD flows driven by form submission and DTO mapping
- deletion must respect contribution relationships and any business rules that preserve referential integrity
- question-bank deletion may later trigger orphan-author cleanup if no contributions remain
## 6. Permissions and Security
- author pages require a valid session in Thymeleaf flows
- backend authorization still governs create/update/delete access
- menu visibility and action availability are derived from permission claims in session-backed JWT data
## 7. UI, API, and Service Responsibilities
### 7.1 Thymeleaf Layer
- render author list/edit/details pages
- translate UI filters and form submissions into authorized backend calls
- preserve navigation context into related question bank and question pages
### 7.2 API Layer
- expose stable CRUD and filter endpoints for authors
- return DTOs rather than entities
- provide course-scoped author lookup data for other features
### 7.3 Service Layer
- own author creation/update/delete rules
- supply lightweight author lookup data for filters and dropdowns
- support downstream features that need author context
## 8. Validation and Error Handling
- invalid author input should fail fast with `400`-class behavior
- missing authors should map to `404`
- UI failures should redirect safely or return the current page with meaningful messages
- deletions should avoid leaving orphaned relationship rows
## 9. Key Decisions
- treat author information as a reusable cross-feature lookup domain
- keep lightweight lookup DTOs separate from full detail DTOs
- centralize author filtering in service methods so dependent features reuse the same semantics
- allow orphan-author cleanup as part of question-bank lifecycle rather than scattering it across unrelated features
## 10. Implementation Notes
- Backend controller:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/controller/AuthorController.java`
- Main service:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/services/AuthorService.java`
- Thymeleaf controller:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyAuthorController.java`
- Templates:
  - `myquiz-thymeleaf/src/main/resources/templates/author-list.html`
  - `myquiz-thymeleaf/src/main/resources/templates/author-edit.html`
  - `myquiz-thymeleaf/src/main/resources/templates/author-details.html`
Related docs:
- `prompt/question-bank-sd.md`
- `prompt/question-sd.md`
- `prompt/author-error-sd.md`
- `prompt/upload-sd.md`
