# Duplicate Management Software Design
## 1. Overview
This document defines the duplicate-management feature used to recompute, inspect, clear, and persist duplicate-analysis results.
Duplicate management operates on question data and supports three scopes:
- course
- question bank
- author within question bank/course context
## 2. Functional Scope
### 2.1 Main Features
- recompute duplicate links using a selected comparison strategy
- view duplicate statistics for a selected scope
- clear duplicate links and related duplicate-validation errors for a scope
- save recompute results to persistent history
- list and delete recompute history entries
### 2.2 Main End-to-End Calls
| Operation | Template | Thymeleaf Route | Backend Route | Service Entry Point |
|---|---|---|---|---|
| Open page | `duplicate-recompute.html` | `GET /duplicate-management` | history and lookup endpoints | `CourseService` history/lookup methods |
| Recompute | `duplicate-recompute.html` | `POST /duplicate-management/recompute` | `POST /api/courses/recompute-with-strategy` | `CourseService.recomputeDuplicates...` |
| Statistics | `duplicate-recompute.html` | `POST /duplicate-management/recompute` | `GET /api/courses/duplicate-statistics` | `CourseService.getDuplicateStatistics...` |
| Clear duplicates | `duplicate-recompute.html` | `POST /duplicate-management/recompute` | `POST /api/courses/clear-duplicates` | `CourseService.clearDuplicates...` |
| Save history | `duplicate-recompute.html` | `POST /duplicate-management/history/save` | `POST /api/courses/recompute-history` | `CourseService.saveRecomputeHistory(...)` |
| Delete history | `duplicate-recompute.html` | `POST /duplicate-management/history/{id}/delete` | `DELETE /api/courses/recompute-history/{id}` | `CourseService.deleteRecomputeHistoryEntry(...)` |
## 3. Architecture
### 3.1 Main Components
- `ThyDuplicateManagementController`
- `CourseController` duplicate endpoints
- `CourseService` duplicate orchestration
- duplicate DTOs in `myquiz-api`
- recompute-history persistence
### 3.2 Scope Cascade Model
The page loads scope options in a cascade:
1. course
2. question bank within course
3. author within question bank
## 4. Data Model and DTOs
Primary contracts include:
- `CourseDuplicateRecomputeResultDto`
- `DuplicateStatisticsDto`
- `DuplicateRecomputeHistoryDto`
- lightweight course/question-bank/author lookup DTOs for filters
## 5. Flows
### 5.1 Recompute
1. User selects scope and strategy.
2. Thymeleaf posts `action=recompute`.
3. Backend dispatches by scope to course/question-bank/author recompute logic.
4. Existing duplicate links and duplicate-related errors in scope are cleared.
5. Duplicate detection is rerun.
6. Result DTO is rendered and can be saved to history.
### 5.2 Statistics
1. User selects scope and posts `action=statistics`.
2. Backend returns `DuplicateStatisticsDto` for the same scope.
3. UI renders a statistics panel without mutating data.
### 5.3 Clear
1. User posts `action=clear`.
2. Backend deletes duplicate links and duplicate-validation errors for the selected scope.
3. Cleared count is returned to the UI.
### 5.4 History Save/Delete
- save path serializes current result metrics into `DuplicateRecomputeHistoryDto`
- delete path removes one persisted history record by id
## 6. Permissions and Security
- page access requires a valid session
- backend authorization remains the source of truth for duplicate-management operations
- unauthorized/forbidden API responses invalidate session and redirect to login in Thymeleaf flow
## 7. UI, API, and Service Responsibilities
### 7.1 Thymeleaf Layer
- populate cascade filter model
- dispatch action-specific requests
- preserve scope selection across requests
- render history, result, statistics, and clear-result sections
### 7.2 API Layer
- expose duplicate recompute/statistics/clear/history endpoints under course-oriented routes
- validate required parameters such as strategy and scope anchors where needed
### 7.3 Service Layer
- select the correct scope-specific duplicate computation path
- create/delete persistent recompute-history entries
- compute duplicate counts and duplicate-error counts
## 8. Validation and Error Handling
- missing or invalid scope parameters should fail fast as bad requests
- session expiry returns the user to login
- Thymeleaf falls back to a safe page render with error message when action processing fails
- history deletion surfaces not-found as a user-visible message rather than silent failure
## 9. Key Decisions
- keep duplicate-management UI consolidated into one page with action-based posts
- keep recompute history explicit and user-triggered rather than automatic
- support scope narrowing from course to question bank to author for operational control
- keep duplicate strategy user-selectable through `DuplicateComparisonStrategy`
## 10. Implementation Notes
- Thymeleaf controller:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyDuplicateManagementController.java`
- Backend controller:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/controller/CourseController.java`
- Main service:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/services/CourseService.java`
- Template:
  - `myquiz-thymeleaf/src/main/resources/templates/duplicate-recompute.html`
Related docs:
- `prompt/question-sd.md`
- `prompt/question-bank-sd.md`
