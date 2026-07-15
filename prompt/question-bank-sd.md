# Question Bank Software Design

## 1. Overview

This document defines the current Question Bank design across:

- `myquiz-thymeleaf` UI and page flow
- `myquiz-app` REST API and business services
- `myquiz-api` contracts (DTOs and API interface)

Question Bank functionality covers listing, filtering, details, extended export view, create/update/delete, statistics, and export (CSV/XML).

## 2. Functional Scope

### 2.1 Core Operations

- List and filter question banks by course with pagination.
- View question bank details (`question-bank-details.html`).
- View extended grouped export view (`question-bank-extended-details.html`).
- Create and update question banks.
- Delete question banks and clean orphaned authors.
- Export MC and TF CSV plus XML.
- View per-author statistics for one question bank.

### 2.2 Main End-to-End Calls

| Operation | Template | Thymeleaf Endpoint | Backend Endpoint | Service Entry Point |
|---|---|---|---|---|
| List/filter | `question-bank-list.html` | `GET /question-banks`, `GET /question-banks/course/id/{courseId}` | `POST /api/question-banks/filter` | `QuestionBankService.filterQuestionBanks(...)` |
| View details | `question-bank-details.html` | `GET /question-banks/{id}` | `GET /api/question-banks/{id}` | `QuestionBankService.getQuestionBankById(...)` |
| View extended | `question-bank-extended-details.html` | `GET /question-banks/{id}/extended` | `GET /api/question-banks/{id}/extended` | `QuestionBankService.getQuestionBankExtendedById(...)` |
| Create | `question-bank-editor.html` | `POST /question-banks`, `POST /question-banks/save` | `POST /api/question-banks` | `QuestionBankService.createQuestionBank(...)` |
| Update | `question-bank-editor.html` | `POST /question-banks/edit/{id}`, `POST /question-banks/save` | `PUT /api/question-banks/{id}` | `QuestionBankService.updateQuestionBank(...)` |
| Delete | `question-bank-list.html` | `POST /question-banks/delete/{id}` | `DELETE /api/question-banks/{id}` | `QuestionBankService.deleteQuestionBankById(...)` |
| Statistics | `question-bank-statistics.html` | `GET /question-banks/{id}/statistics` | `GET /api/question-banks/{id}/statistics` | `QuestionBankController.getQuestionBankStatistics(...)` |
| Export MC | list/details actions | `GET /question-banks/{id}/export-mc` | `GET /api/question-banks/{id}/export-mc` | `QuestionBankController.exportQuestionBankToCsv(...)` |
| Export TF | list/details actions | `GET /question-banks/{id}/export-tf` | `GET /api/question-banks/{id}/export-tf` | `QuestionBankController.exportQuestionBankToCsvTF(...)` |
| Export XML | list/details actions | `GET /question-banks/{id}/export-xml` | `GET /api/question-banks/{id}/export-xml` | `QuestionBankController.exportQuestionBankToXml(...)` |

Legacy compatibility note: `GET /question-banks/{id}/delete` still exists in `ThyQuestionBankController` and also calls the same backend delete endpoint.

## 3. Architecture

### 3.1 Module Responsibilities

- `myquiz-thymeleaf`
  - `ThyQuestionBankController` orchestrates session checks, API calls, redirects, and model attributes.
  - Templates render list/details/editor/statistics/extended views.
- `myquiz-app`
  - `QuestionBankController` exposes `/api/question-banks` REST endpoints.
  - `QuestionBankService` implements filtering, retrieval, and transactional delete logic.
  - `ExportService` generates XML payloads.
- `myquiz-api`
  - `QuestionBankApi` defines REST contract and OpenAPI-level operation metadata.
  - DTOs define payload structure between modules.

### 3.2 Persistence Topology

Question Bank flow depends on these relationships:

- `QuestionBank` (root aggregate for UI operations)
- `QuestionBankAuthor` (join-like contribution entity with `author`, `questionBank`, source, template type)
- `Question` (owned by `QuestionBankAuthor`)
- `QuestionError` (owned by `Question`)
- `QuestionDuplicate` (cross-question duplicate links)
- `Author` (candidate for orphan cleanup after deletion)

## 4. Data Model and DTOs

### 4.1 API Contract DTOs Used in This Feature

- `QuestionBankDto`
  - Basic identity and metadata (`id`, `name`, `course`, `studyYear`)
  - Derived counts (`mcQuestionsCount`, `tfQuestionsCount`, `numberOfDuplicates`, `noAuthors`)
  - Embedded details lists for details view (`questionsMultichoice`, `questionsTruefalse`, `authors`)
- `QuestionBankFilterRequestDto`
  - `page`, `pageSize`, `courseId`
- `QuestionBankFilterResponseDto`
  - `questionBanks`, `totalElements`, `totalPages`, `page`, `pageSize`, `courses`
- `QuestionBankExportDto`
  - `questionBank` + `authorSections`
- `QuestionBankExportAuthorSectionDto`
  - Author block in extended export: author profile, MC/TF questions, errors, duplicates
- `QuestionBankStatisticsDto`
  - `questionBank` + per-author aggregates (`mcCount`, `tfCount`, `errorCount`)

### 4.2 Service Output Characteristics

- Counts shown in list/detail are computed from `Question` data filtered by question bank.
- Duplicate count is derived from linked rows in `question_duplicate` intersected with the question bank question IDs.
- Extended export sections are grouped by `QuestionBankAuthor` and sorted by author name.

## 5. Flows

### 5.1 List and Filter

1. UI hits `GET /question-banks` (or course-specific routes).
2. `ThyQuestionBankController.renderQuestionBankList(...)` builds `QuestionBankFilterRequestDto`.
3. Thymeleaf calls `POST /api/question-banks/filter`.
4. Backend service applies paging guards, optional course filter, and maps DTO counts.
5. Response includes both page data and courses for dropdown rendering.

Failure behavior:

- Unauthorized/forbidden API responses invalidate session and redirect to login.
- Bad filter request path retries once with safe defaults.
- Generic failures render list view with fallback empty model and error message.

### 5.2 View Details and Extended Details

Details (`/question-banks/{id}`):

1. Thymeleaf calls `GET /api/question-banks/{id}`.
2. Backend builds `QuestionBankDto` with question lists, counts, authors, and source summary.

Extended (`/question-banks/{id}/extended`):

1. Thymeleaf calls `GET /api/question-banks/{id}/extended`.
2. Service builds `QuestionBankExportDto`.
3. Author sections include:
   - questions grouped by type
   - non-duplicate validation errors
   - deduplicated duplicate question projections

### 5.3 Create and Update

- Create path persists or reuses existing question bank by name/course/study year.
- Update path rewrites course/name/study year and returns refreshed basic DTO.
- Thymeleaf uses editor form routes (`/new`, `/edit/{id}`, `/save`) and redirects to list with flash messages.

### 5.4 Delete (Transactional Cleanup)

Deletion in `QuestionBankService.deleteQuestionBankById(...)` runs in one transaction:

1. Verify question bank exists.
2. Load all `QuestionBankAuthor` rows for that question bank with author data.
3. Collect deletion scope (`questionBankAuthorIds`, authors to re-check).
4. Delete questions for each contribution block.
5. Delete related question errors.
6. Delete `QuestionBankAuthor` entries.
7. Delete question bank root row.
8. Remove orphaned authors that have zero remaining contributions.

Design intent:

- Keep referential integrity.
- Preserve authors that still contribute to other question banks.
- Log per-step timing and counts.

### 5.5 Export and Statistics

- CSV exports are generated in API endpoints and streamed by Thymeleaf download routes.
- XML export delegates generation to `ExportService` and requires explicit permission.
- Statistics endpoint aggregates question type counts and error counts per author.

## 6. Permissions and Security

- XML export is permission-gated in API by `EXPORT_XML` authority.
- Thymeleaf controller checks session/JWT presence before API forwarding.
- Unauthorized and forbidden responses trigger session invalidation and redirect to login for protected pages.

## 7. UI, API, and Service Responsibilities

### 7.1 Thymeleaf Controller (`ThyQuestionBankController`)

- Session validation, fallback redirects, and user feedback messages.
- API orchestration with `RestTemplate`.
- Mapping request parameters (`page`, `pageSize`, `courseId`) to filter DTO.
- Streaming downloaded content to browser for CSV/XML exports.

### 7.2 REST Controller (`QuestionBankController`)

- Endpoint publishing under `/api/question-banks`.
- HTTP status mapping (`200`, `201`, `204`, `404`, `500`, `403`).
- Response DTO construction delegation to service layer.

### 7.3 Service Layer (`QuestionBankService`)

- Core business logic for create/update/delete/filter.
- DTO enrichment with counts and grouped extended structures.
- Transaction boundaries for destructive operations.
- Duplicate count and author-orphan cleanup calculations.

### 7.4 Repository and Specification Layer

- `QuestionSpecification` and `QuestionBankSpecification` encode filter logic.
- `QuestionBankAuthorSpecification` supports selective fetch strategy.
- Repository calls back service projections and cleanup operations.

## 8. Validation and Error Handling

Validation and guard rails in current implementation:

- Null or invalid filter input in service throws `IllegalArgumentException`.
- Page/pageSize are normalized to positive defaults.
- Not found question banks are surfaced as `404` in controller flows.
- Delete flow explicitly fails fast when question bank does not exist.
- UI routes return safe fallback pages with error messages on backend exceptions.

## 9. Key Decisions

- Use DTO-driven filter API (`POST /filter`) to support expandable filter contracts.
- Keep delete operation in service-level transaction to avoid partial cleanup.
- Return course dropdown data together with list filter response to minimize extra round-trips.
- Keep extended details as a dedicated endpoint to avoid overloading default details payloads.
- Keep XML authorization check in backend controller where authority context is guaranteed.

## 10. Implementation Notes

- Canonical API contract: `myquiz-api/src/main/java/com/unitbv/myquiz/api/interfaces/QuestionBankApi.java`
- REST controller: `myquiz-app/src/main/java/com/unitbv/myquiz/app/controller/QuestionBankController.java`
- Service logic: `myquiz-app/src/main/java/com/unitbv/myquiz/app/services/QuestionBankService.java`
- Thymeleaf controller: `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyQuestionBankController.java`
- Main templates:
  - `myquiz-thymeleaf/src/main/resources/templates/question-bank-list.html`
  - `myquiz-thymeleaf/src/main/resources/templates/question-bank-details.html`
  - `myquiz-thymeleaf/src/main/resources/templates/question-bank-extended-details.html`
  - `myquiz-thymeleaf/src/main/resources/templates/question-bank-editor.html`
  - `myquiz-thymeleaf/src/main/resources/templates/question-bank-statistics.html`

Related docs:

- `prompt/question-sd.md`
- `prompt/author-sd.md`
- `prompt/upload-sd.md`
- `prompt/style-sd.md`
