# Author Error Software Design Operations Handling

## 1. DTOs in `myquiz-api`
Inputs: filter criteria (course, author), pagination (page, pageSize)
Outputs:
- AuthorErrorFilterDto (fields: authorErrors, authorNames, courses, course, authorName, page, pageSize, totalElements, totalPages)
- AuthorErrorDto (existing: id, description, row, authorName, authorId, quizName, questionId, dateCreated, status)

## 2. Author Error Operations

| Section | Operation                           | UI Template       | Thymeleaf Endpoint        | Backend Endpoint                 | Service Action                                     |
|---------|-------------------------------------|-------------------|---------------------------|----------------------------------|-----------------------------------------------------|
| 2.1.1   | List Errors (Filter/Paginate)       | error-list.html   | GET /errors               | GET /api/errors                  | AuthorErrorService.getAuthorErrorsModel()           |
| 2.1.2   | Resolve Error                       | error-list.html   | POST /errors/{id}/resolve | PUT /api/errors/{id}/resolve     | AuthorErrorService.resolveErrorById() (future)       |
| 2.1.3   | Delete Error                        | error-list.html   | POST /errors/{id}         | DELETE /api/errors/{id}          | AuthorErrorService.deleteErrorById() (future)        |

For each operation, the flow cascades from UI template to Thymeleaf Controller, to backend endpoint, to service action. The items below outline steps, inputs/outputs, and error modes.

### 2.1 Actions from error-list.html
#### 2.1.1 List Errors (with Filtering & Pagination)
- Step 1: UI Template
  - Template: `error-list.html`
  - Action: User selects course/author and page size, submits form
  - Inputs: selectedCourse, selectedAuthor, page, pageSize
  - Output: AuthorErrorFilterDto rendered (subset per page)
  - Errors: Invalid params, empty result
- Step 2: Thymeleaf Controller
  - Endpoint: `GET /errors` (ThyAuthorErrorController)
  - Action: Validates session/JWT, builds URL with filters + page, calls backend
  - Inputs: selectedCourse, selectedAuthor, page, pageSize
  - Output: AuthorErrorFilterDto (with pagination metadata)
  - Errors: Backend error, invalid params
- Step 3: Backend Endpoint
  - Endpoint: `GET /api/errors` (AuthorErrorController)
  - Action: Accept filters, call service for server-side page slicing
  - Inputs: selectedCourse, selectedAuthor, page, pageSize
  - Output: AuthorErrorFilterDto (errors, courses, authors, totals)
  - Errors: Validation error, DB error
- Step 4: Service Action
  - Service: `AuthorErrorService.getAuthorErrorsModel(course, author, page, pageSize)`
  - Inputs: filters + Pageable
  - Output: Page-mapped AuthorErrorFilterDto with totals
  - Errors: Data access errors

## 3. Server-side Pagination Design
- Repository: `AuthorErrorRepository.findErrorsByCourseAndAuthor(String coursePattern, String authorPattern, Pageable pageable)`
  - Query: Use `LOWER(column) LIKE :pattern` and pass prebuilt `%value%` patterns from service.
- Service: Build patterns and PageRequest, map `Page<QuizError>` to `List<AuthorErrorDto>` and collect DTO metadata.
- Controller: Accept query params; set defaults (page=1, pageSize=10) and forward to service.
- UI: Use shared `searchfilter` styles, render only current page, and include Previous/Next controls that preserve course/author filters.

## 4. Contracts & Error Modes
- Inputs: course (String), author (String), page (Integer >=1), pageSize (Integer >=1)
- Outputs: AuthorErrorFilterDto (errors limited to page, totals for pagination)
- Error modes: 400 invalid params, 404 empty/none, 500 DB/server errors

## 5. Testing Checklist
- Happy path: course filter only; author filter only; both; none.
- Pagination: page 1, middle page, last page, bounds disabling.
- Case-insensitive filtering works; no SQL errors (`lower(bytea)` or `text~~bytea`).
- UI preserves filters across pages and refill dropdowns correctly.

## 6. Troubleshooting
- If Postgres error on LIKE: ensure service passes `%value%` strings and JPQL uses `LOWER(column) LIKE :pattern`.
- If totals mismatch: verify Page’s `getTotalElements()` and `getTotalPages()` mapped into DTO.
- If empty courses/authors: populate from AuthorService.getCourseNames() and error-derived author names.

## Author Operations

### Create / Update
- Authors generate error entries indirectly by performing uploads or question edits that fail validation.
- Authors can correct underlying data to transition associated errors from OPEN to RESOLVED.

### View / List
- Authors view their import and validation errors via dedicated error listing pages and author-details views.

### Delete / Archive
- Errors are typically not deleted by authors; they are resolved. Any archival or purging of old errors is handled by backend maintenance jobs or administrators.

### Permissions & Roles
- Authors can only see errors associated with their own data; administrators may see errors system-wide.

---

Note: Align naming and behavior with `author-sd.md` and keep DTOs/controllers consistent across the app.
