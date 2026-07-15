# Data Management Software Design
## 1. Overview
This document defines the current data-management feature set in MyQuiz.
The feature includes:
- non-auth data cleanup
- non-auth SQL export and import
- duplicate recompute history backup/restore support
- database statistics for business data and, conditionally, IAM data
- admin-facing data-management pages in Thymeleaf
## 2. Functional Scope
### 2.1 Main Features
- delete all non-auth application data while preserving users, roles, and permissions
- export non-auth data as SQL backup
- import non-auth SQL backup with auth-table protection
- include duplicate recompute history in the backup set
- show database statistics
- optionally enrich statistics with IAM counts when permission allows it
### 2.2 Main End-to-End Calls
| Operation | Thymeleaf Route | Backend Route | Service Entry Point |
|---|---|---|---|
| Data dashboard | `GET /admin/data` | — | — |
| Delete all data | `POST /admin/data/deleteall` | `DELETE /api/data/deleteall` | `DataCleanupService.deleteAllDataExceptUsersRolesPermissions()` |
| Export SQL | `GET /admin/data/export-sql` | `GET /api/data/export-sql` | `DataCleanupService.exportNonAuthDataAsSql()` |
| Import SQL | `POST /admin/data/import-sql` | `POST /api/data/import-sql` | `DataCleanupService.importNonAuthDataFromSql(...)` |
| Statistics API | `/statistics` UI and admin flows | `GET /api/data/statistics` | `DataCleanupService.getDataStatistics()` |
## 3. Architecture
### 3.1 Main Components
- `DataCleanupController` in `myquiz-app`
- `DataCleanupService` in `myquiz-app`
- `ThyDataManagementController` in `myquiz-thymeleaf`
- templates:
  - `admin/data-management.html`
  - `database-statistics.html`
### 3.2 Protection Boundary
Auth/IAM data is not part of normal cleanup/import/export scope.
## 4. Data Model and Contracts
### 4.1 Statistics Contract
`GET /api/data/statistics` returns `Map<String, Long>` with counts for application tables and, when authorized, IAM statistics merged in from `myquiz-auth`.
### 4.2 SQL Backup Contract
The export/import pipeline is intentionally limited to non-auth tables.
## 5. Flows
### 5.1 Delete All
1. Admin submits POST from `admin/data-management.html`.
2. Thymeleaf controller calls backend `DELETE /api/data/deleteall`.
3. `DataCleanupService` deletes data in FK-safe order.
4. Users/roles/permissions are preserved.
5. UI redirects back with success or error feedback.
### 5.2 SQL Export
1. User requests `GET /admin/data/export-sql`.
2. Thymeleaf controller proxies backend bytes to browser download.
3. Backend builds SQL for non-auth tables only.
### 5.3 SQL Import
1. User uploads `.sql` file to `POST /admin/data/import-sql`.
2. Thymeleaf forwards multipart payload.
3. Service validates that the script does not modify auth tables.
4. SQL is executed against application data tables.
### 5.4 Statistics
1. User opens `/statistics` or admin dashboard.
2. Thymeleaf calls `GET /api/data/statistics`.
3. Service returns base counts.
4. If `VIEW_EXTENDED_STATISTICS` is present, IAM counts are fetched through `myquiz-auth` and merged in.
## 6. Permissions and Security
- delete-all requires `CLEAN_DATABASE`
- SQL export requires `BACKUP_DATABASE`
- SQL import requires `RESTORE_DATABASE`
- extended statistics require `VIEW_EXTENDED_STATISTICS`
- import validation explicitly blocks statements targeting auth tables
## 7. UI, API, and Service Responsibilities
### 7.1 Thymeleaf Layer
- render admin dashboard and import/export actions
- preserve user messages on redirects
- proxy download/upload content to backend
### 7.2 API Layer
- enforce permission checks at endpoint level
- shape HTTP status codes (`200`, `204`, `400`, `403`, `500`)
- convert service exceptions into stable API behavior
### 7.3 Service Layer
- own transaction boundaries for destructive operations
- determine export table order and sequence reset logic
- validate backup scripts before execution
- query base statistics and optionally enrich with IAM data
## 8. Validation and Error Handling
- empty uploads are rejected as bad requests
- auth-table mutations inside SQL imports are rejected
- delete-all failures raise a `DataCleanupException`
- Thymeleaf shows access-denied or generic failure messages without exposing stack traces
## 9. Key Decisions
- keep auth data out of cleanup/export/import scope
- keep delete-all transactional and ordered for referential integrity
- keep SQL import validation conservative and fail-fast
- keep admin UI destructive action on POST, not GET
## 10. Implementation Notes
- API contract:
  - `myquiz-api/src/main/java/com/unitbv/myquiz/api/interfaces/DataCleanupApi.java`
- Backend controller:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/controller/DataCleanupController.java`
- Service:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/services/DataCleanupService.java`
- Thymeleaf controller:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyDataManagementController.java`
Related docs:
- `prompt/admin-interface-sd.md`
- `prompt/core-sd.md`
