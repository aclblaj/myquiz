# Implementation Plan - Repair Question Duplicates Route

Fix the route `/questions/{id}/duplicates` to support pagination, display duplicate
question answers in the duplicates table, render a selection column with checkboxes
for duplicate removal, and fix the backend implementation so that only the
**selected** duplicates are unlinked rather than all of them.

## Codebase Findings (context for implementers)

- `QuestionDto.duplicates` is a `List<QuestionDuplicateDto>` (not a plain list of
  question IDs). Each `QuestionDuplicateDto` already carries **both** pair fields
  (`question1Id/question1Title`, `question2Id/question2Title`, `similarity`) **and**
  "other question" fields relative to the primary question
  (`questionId`, `title`, `text`, `response1..response4`, `type`, `author`, `course`,
  `questionBankName`, `row`, `cause`). The Answers column and the checkbox `value`
  must use `duplicate.questionId` / `duplicate.response1..4` / `duplicate.type`,
  which already exist on the DTO — no DTO changes are required.
- The current `fragments/tables::duplicate-table` fragment declares the
  `showSelection` and `showActions` parameters but **never uses `showSelection`**
  and always renders the Actions column/header unconditionally, so today the
  fragment silently ignores the flags. This must be fixed.
- `QuestionService.removeDuplicationLinks(questionId, duplicateIds)` currently calls
  `questionDuplicationService.removeDuplicateAssociations(questionId)`, which deletes
  **every** duplicate link for `questionId` regardless of `duplicateIds` — this is the
  root cause of the "removes all duplicates" bug. `duplicateIds` is otherwise plumbed
  correctly end-to-end (Thymeleaf form → `ThyQuestionController.removeSelectedDuplicates`
  → REST `QuestionController.removeQuestionDuplicates` → `DuplicateUnlinkRequestDto`
  → `QuestionService.removeDuplicationLinks`), so only the service-layer method body
  needs correction.
- `QuestionDuplicateRepository` already exposes
  `long deleteByQuestionIdAndDuplicateQuestionId(Long questionId, Long duplicateQuestionId)`,
  which is sufficient to implement selective deletion without adding new repository
  methods. Links can exist with the pair stored in either column order, so deletion
  must be attempted in both directions for each selected duplicate ID.
- `ThyQuestionController.showQuestionDuplicates` currently fetches
  `question.getDuplicates()` from the API and pushes the **full, unpaginated** list
  into the model — no `totalPages`/`totalElements`/`currentPage`/`pageSize` are
  computed for this view (unlike `question-list.html`, which already has a working
  pagination block/pattern to copy from).
- The **"✔️ Resolved" per-row action button** in `fragments/tables::duplicate-table`
  currently posts to `@{/duplicates/{id}/mark-resolved(id=${duplicate.id})}`
  (`duplicate.id` being the `QuestionDuplicate` link's own primary key), but
  **no such endpoint exists anywhere in the codebase**, and `QuestionDuplicate`
  has **no `status` field at all** today (unlike `QuestionError`, which already
  has exactly this pattern — see next bullet). This is dead/broken markup today
  — it happens to never actually render because the only other caller of the
  fragment, `question-bank-extended-details.html`, passes `showActions=false`.
  Once `showActions` is properly honored (see below), this button would start
  rendering on `question-duplicates.html` and must be implemented for real,
  following the **existing `QuestionError` status pattern** (see below) rather
  than being replaced with an unlink action.
- `QuestionError` already has exactly the status-flag pattern requested for
  duplicates: a `status` column (`String`, default
  `ControllerSettings.ERROR_STATUS_OPEN`, values `"OPEN"`/`"RESOLVED"`), a
  service method `QuestionErrorService.resolveErrorById(Long id)` that loads the
  error, sets `status = ERROR_STATUS_RESOLVED`, and saves it (**without deleting
  the row**), a standalone REST endpoint
  `PUT /api/errors/{id}/resolve` (`QuestionErrorController`, mapped at
  `/api/errors`, distinct from `/api/questions`), and a Thymeleaf endpoint
  `POST /errors/{id}/resolve` (`ThyErrorController`, mapped at `/errors`). The
  duplicate-table's "Resolved" action and new **Status** column should mirror
  this pattern exactly: `QuestionDuplicate` gains its own `status` field/column,
  a `markDuplicateResolved(Long duplicateLinkId)` service method that flips
  status without unlinking, a standalone `/api/duplicates/{id}/resolve` REST
  endpoint, and a `/duplicates/{id}/mark-resolved` Thymeleaf endpoint (matching
  the path the template **already** posts to — only the backend is missing).
  The existing badge styling (`.badge`, `.badge-status-open`,
  `.badge-status-resolved`) and row highlighting (`.resolved-row`), already used
  by `error-table`, already exist in `styles.css` and should be reused as-is —
  no new CSS is needed for the Status column/badges.
- `spring.jpa.hibernate.ddl-auto=update` is configured in
  `myquiz-app/src/main/resources/application.properties`, and there are no
  Liquibase/Flyway migration files in the repository — schema changes (like the
  new `status` column on `question_duplicate`) are picked up automatically by
  Hibernate on startup, exactly how the existing `question_error.status` column
  was introduced. No manual migration script is required.
- `QuestionDuplicateMapper.toDuplicateDto(QuestionDuplicate duplicateLink,
  Question question)` is the **single, centralized place** where
  `QuestionDuplicateDto` instances are built from `QuestionDuplicate` entities
  (used both for `question.getDuplicates()` and the
  `question-bank-extended-details.html` course-wide view) — this is the only
  place that needs to be updated to populate the new `status` field onto the DTO.
- `QuestionDuplicationService.removeAllDuplicateAssociationsForQuestion(Long
  questionId)` **already exists** and is fully implemented (used today by
  `QuestionService.deleteQuestionInternal` and `CourseService`) — it removes every
  duplicate link for a question in one call. This can be reused as-is for the new
  "delete all duplicates" button; only a thin service/controller passthrough is
  needed to expose it to the Thymeleaf UI, no new deletion logic.
- `com.unitbv.myquiz.api.util.PaginationParams` (a `record(int page, int pageSize)`,
  1-based page) and `com.unitbv.myquiz.api.util.PaginationSupport.normalize(Integer
  page, Integer pageSize)` already exist and are the standard way pagination is
  normalized across the codebase (used in `QuestionService.getQuestionsFiltered`,
  `QuestionController`, `AuthorService`, `QuestionBankService`,
  `QuestionErrorService`, etc.). `normalize()` applies the `ControllerSettings`
  default page/pageSize when the incoming values are null or `<= 0`. This should be
  reused for the in-memory pagination of duplicates instead of writing new
  page/pageSize defaulting logic.
- `question-duplicates.html` has no pagination controls or total-count block today.
- `ControllerSettings.ATTR_TOTAL_PAGES` (`"totalPages"`) and
  `ControllerSettings.ATTR_TOTAL_ELEMENTS` (`"totalElements"`) **already exist**
  and are already used elsewhere in `ThyQuestionController` (the question-list
  pagination code) — the new pagination model attributes for the duplicates view
  must reuse these existing constants rather than hardcoded string literals.
  Likewise `ATTR_CURRENT_PAGE` (`"currentPage"`), `ATTR_PAGE_SIZE` (`"pageSize"`),
  `ATTR_PAGE_NUMBER` (`"page"`), `ATTR_BACK_URL` (`"backUrl"`), `ATTR_COURSE_ID`
  (`"courseId"`), `ATTR_AUTHOR_ID` (`"authorId"`), `ATTR_QUESTION_BANK_ID`
  (`"questionBankId"`), `ATTR_SELECTED_TYPE`, `ATTR_SELECTED_COURSE_ID`,
  `ATTR_SELECTED_AUTHOR_ID`, `ATTR_SELECTED_QUESTION_BANK_ID`, and
  `ATTR_DUPLICATE_QUESTION_IDS` (`"duplicateQuestionIds"`) all already exist and
  are already used by the surrounding duplicates-page code — every hidden form
  field / request-param name / model-attribute key touched by this plan has a
  matching constant and **no raw string literals should be introduced** for these
  in any modified Java controller/service code.

## User Review Required

**IMPORTANT**

The duplicates table (`fragments/tables::duplicate-table`) will be updated to
display a new **Answers** column showing the responses/choices of the duplicate
questions (rendered as separate numbered lines, matching the style used in
`author-details.html`), to help identify why they were marked as duplicates. It
will also be updated to properly respect the `showSelection` and `showActions`
flags, and gain a new **Status** column with `OPEN`/`RESOLVED` badges — the same
visual pattern already used by the error-table (`.badge-status-open` /
`.badge-status-resolved` / `.resolved-row`). This requires adding a new
persisted `status` field to the `QuestionDuplicate` entity/DTO (mirroring the
existing `QuestionError.status` pattern exactly). Its per-row **Actions** column
will gain a working **"✔️ Resolved"** button (implementing the endpoint the
markup already targets, which is currently missing) that **flags the duplicate
pair as resolved without deleting the link** (same semantics as resolving a
question error) — it does **not** unlink the questions. Additionally,
`question-duplicates.html` will gain a **"🗑️ Delete All Duplicates"** button next
to "Remove Selected Duplicate Links" that unlinks every duplicate of the current
question in a single action, regardless of pagination.

## Logging

Logging today is inconsistent for duplicate-removal flows:
`QuestionService.removeDuplicationLinks` and `QuestionDuplicationService`'s
`cleanupDuplicateErrors` already log via the fluent Log4j2 API
(`logger.atInfo().addArgument(...).log("...")` / `logger.atError().setCause(e)...`),
but `removeAllDuplicateAssociationsForQuestion` has **no logging at all** today, and
`ThyQuestionController` uses plain SLF4J calls (`log.info(...)`, `log.error(...)`).
New/modified code should follow the existing convention of each layer (fluent API
in `myquiz-app` services, plain SLF4J in `myquiz-thymeleaf` controllers) and add
logging at the following points so duplicate-removal actions are traceable and
pagination/edge-case bugs are diagnosable in production logs:

- **`QuestionDuplicationService.removeSpecificDuplicateAssociations`**: log at
  `info` level on entry with `questionId` and the count/content of `duplicateIds`,
  and on completion log how many links were actually removed vs. how many IDs were
  requested (a mismatch indicates stale/already-removed links or a mismatched
  pair direction — useful for debugging). Log at `warn` if `questionId` is null or
  `duplicateIds` is empty (current no-op is silent).
- **`QuestionDuplicationService.removeAllDuplicateAssociationsForQuestion`**: add
  `info`-level logging for `questionId` and the number of links removed (currently
  has zero logging), plus a `warn` when `questionId` is null, mirroring the
  logging already present in `removeDuplicateAssociations`.
- **`QuestionService.removeDuplicationLinks` / `removeAllDuplicationLinks`**: keep
  the existing `info`/`error` logging pattern (already present for
  `removeDuplicationLinks`); ensure the new `removeAllDuplicationLinks` logs
  success/failure with `questionId` the same way.
- **`QuestionController.removeQuestionDuplicates` (existing) and the new
  remove-all REST endpoint**: keep/add `info` logging on request receipt and
  outcome (already present for `removeQuestionDuplicates`), including a `warn` log
  for invalid/empty selection payloads (already present).
- **`ThyQuestionController.showQuestionDuplicates`**: add a `debug`/`info` log
  after computing pagination (`questionId`, requested vs. normalized page,
  `pageSize`, `totalElements`, `totalPages`) — especially useful for diagnosing the
  page-clamping edge case (requested page beyond `totalPages`). Keep the existing
  `log.info("Loading duplicates for question id {}", id)` and
  `log.error(...)` calls on failure paths.
- **`ThyQuestionController.removeSelectedDuplicates`**: keep existing
  `log.error(...)` on exception; add an `info` log line before the REST call
  noting `questionId` and the selected `duplicateQuestionIds` size, so it's clear
  from logs alone which duplicates a user chose to unlink.
- **`ThyQuestionController.removeAllDuplicates` (new)**: add `info` logging before
  the call (`questionId`) and on success/failure of the REST call, and `error`
  logging with the exception on failure — matching the pattern used by
  `removeSelectedDuplicates`.
- **`QuestionDuplicationService.markDuplicateResolved` (new)**: log at `info`
  level on entry with the duplicate link `id`, and on completion log the old →
  new status transition; log at `warn` if the `id` doesn't exist (not found) or
  if it's called on an already-`RESOLVED` link (no-op, but worth surfacing in
  logs since it may indicate a stale page/double-submit).
- **`QuestionDuplicateController.resolveDuplicate` (new REST endpoint)**: `info`
  log on request receipt (`id`) and outcome, `warn` for not-found, mirroring
  `QuestionErrorController.resolveError`'s existing logging pattern exactly.
- **`ThyQuestionDuplicateController.markResolved` (new)**: `info` log before the
  REST call (`id`, `primaryQuestionId`) and on success/failure, `error` logging
  with the exception on generic failure — matching the logging style already
  used by `ThyErrorController.resolveError`.

## Exception Handling & User Feedback

`question-duplicates.html` already renders `successMessage`/`errorMessage` via
the two `<div class="alert">` blocks near the top of the page, and
`ControllerSettings.ATTR_SUCCESS_MESSAGE` (`"successMessage"`) /
`ATTR_ERROR_MESSAGE` (`"errorMessage"`) already exist and are used by
`removeSelectedDuplicates` (as flash attributes) and `showQuestionDuplicates`
(as plain model attributes, though only on the failure paths that redirect to
`VIEW_QUESTION_LIST`, not back to the duplicates page itself). Review found two
pre-existing gaps that should be fixed for consistency while this controller is
already being modified, plus requirements for the new handler:

- **Gap — missing session-expiry (403) handling**: `showQuestionDuplicates` and
  `removeSelectedDuplicates` both only catch `HttpClientErrorException.NotFound`
  (the former) and generic `Exception`, unlike `showQuestionForEdit` elsewhere in
  the same controller, which has a dedicated
  `catch (HttpClientErrorException.Forbidden ex)` branch that calls
  `sessionService.invalidateCurrentSession()` and redirects to
  `ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN`. Without this, an expired
  session on the duplicates page falls into the generic `catch (Exception ex)`
  and shows a confusing "unable to load/remove duplicates" error instead of
  sending the user back to login. **Add the same `Forbidden`-specific catch
  block** to `showQuestionDuplicates`, `removeSelectedDuplicates`, and the new
  `removeAllDuplicates` handler, in that order (most specific exception first),
  matching the existing pattern from `showQuestionForEdit`.
- **`showQuestionDuplicates` failure redirects lose page context**: on
  `NotFound`/generic `Exception`, it currently sets `ATTR_ERROR_MESSAGE` on the
  `Model` and returns `VIEW_QUESTION_LIST` directly (not a redirect), which is
  fine for a first GET load (no page to preserve), but means the error message is
  rendered on the *question list* page, not on `question-duplicates.html`. This
  is acceptable/by design here (there's nothing to show on the duplicates page if
  the question itself couldn't be loaded) — keep as-is, just ensure the message
  text is accurate and the two existing constants (`MSG_QUESTION_NOT_FOUND`,
  `MSG_UNABLE_LOAD_DUPLICATES`) continue to be used (no new ones needed for this
  path).
- **New `removeAllDuplicates` handler exception handling** must mirror
  `removeSelectedDuplicates`'s structure exactly so errors surface through the
  same alert divs on redirect back to `/questions/{id}/duplicates`:
  - `HttpClientErrorException.Forbidden` → invalidate session, redirect to
    `VIEW_REDIRECT_AUTH_LOGIN` (see gap above).
  - `HttpClientErrorException.NotFound` → flash `ATTR_ERROR_MESSAGE` =
    `MSG_QUESTION_NOT_FOUND`, then redirect back to the duplicates page (not
    `VIEW_QUESTION_LIST`, since this is a POST-action failure with page context
    to preserve, matching `removeSelectedDuplicates`'s pattern of always
    returning to `buildQuestionDuplicatesRedirectUrl`).
  - Generic `Exception` → `log.error(...)` with the exception, then flash
    `ATTR_ERROR_MESSAGE` = `MSG_ALL_DUPLICATES_REMOVED_FAILED`.
  - On success → flash `ATTR_SUCCESS_MESSAGE` = `MSG_ALL_DUPLICATES_REMOVED_SUCCESS`.
  - All paths redirect via `buildQuestionDuplicatesRedirectUrl(id, navigationContext)`
    so the user lands back on `question-duplicates.html` with the message
    rendered by the existing alert `<div>`s, at the same page/filter state they
    were on.
- **REST layer (`QuestionController`) exceptions already return proper HTTP
  status codes** (400 for invalid/empty selection, 404 for question not found,
  500 for unexpected errors) that the Thymeleaf layer's `HttpClientErrorException`
  subtypes catch. The new remove-all REST endpoint must follow the same
  try/catch/status-code structure as the existing `removeQuestionDuplicates` so
  the Thymeleaf-side `NotFound`/generic-`Exception` catches behave identically.
- **Edge case — "Delete All Duplicates" invoked with no duplicates left** (e.g.
  stale page after another tab already cleared them): `removeAllDuplicationLinks`
  should complete successfully as a no-op (consistent with
  `removeAllDuplicateAssociationsForQuestion`'s existing null-safe/empty-safe
  behavior) and still show the success message, rather than erroring — avoid
  introducing a "nothing to delete" error path that would be confusing UX
  (the button is only rendered `th:if` duplicates exist, so this can only be
  reached via a stale/duplicate submission, which should be harmless).
- **Per-row "✔️ Resolved" action (status-flip, not unlink) requires its own,
  parallel exception handling** in the new `ThyQuestionDuplicateController`,
  mirroring `ThyErrorController.resolveError`'s existing structure exactly (it
  is a materially different flow from `removeSelectedDuplicates` — it targets a
  standalone `/api/duplicates/{id}/resolve` REST endpoint, not
  `/api/questions/{id}/duplicates/remove`):
  - `HttpClientErrorException.Forbidden` → invalidate session, redirect to
    `VIEW_REDIRECT_AUTH_LOGIN`.
  - `HttpClientErrorException.NotFound` → flash `ATTR_ERROR_MESSAGE` =
    `MSG_DUPLICATE_RESOLVED_FAILED` (the duplicate link ID no longer exists,
    e.g. already deleted via "Remove Selected"/"Delete All" in another tab).
  - Generic `Exception` → `log.error(...)` with the exception, then flash
    `ATTR_ERROR_MESSAGE` = `MSG_DUPLICATE_RESOLVED_FAILED`.
  - On success → flash `ATTR_SUCCESS_MESSAGE` = `MSG_DUPLICATE_RESOLVED_SUCCESS`.
  - Redirects to `/questions/{primaryQuestionId}/duplicates` (falling back to
    `backUrl`, then `/questions`, if `primaryQuestionId` is absent — see the new
    `ThyQuestionDuplicateController` component) so the message renders via the
    same alert `<div>`s on `question-duplicates.html`.
  - The new REST endpoint (`QuestionDuplicateController.resolveDuplicate`) must
    return 404 for an unknown ID and 200 on success, following the same
    try/catch/status-code structure as `QuestionErrorController.resolveError`,
    so the Thymeleaf-side catches behave identically to the existing
    error-resolve flow.

## Styling

Per `prompt/style-sd.md`, the canonical stylesheet is
`myquiz-thymeleaf/src/main/resources/static/css/styles.css`, and templates must
reuse shared classes rather than introduce page-specific/inline styling. Review
of the current stylesheet against this plan's UI changes found:

- **Most needed classes already exist and should simply be reused, no new CSS
  required**: `.styled-table`, `.data-row`, `.action-cell`, `.text-cell`,
  `.list-empty-message`, `.alert`, `.form-actions`, `.btn-outline`, `.btn-delete`,
  `.btn-info`, `.btn-small`, `.pagination-controls`, `.total-elements`. The new
  **Answers** column, **Delete All Duplicates** button (`.btn-delete`), pagination
  block, and total-count block for `question-duplicates.html` should all reuse
  these exactly as `question-list.html` / other pages already do — do not
  duplicate their rules or add page-specific equivalents.
- **Pre-existing gap found: `.btn-success` has no CSS rule at all.** It's
  referenced today in `fragments/tables.html` (the error-table's "✔️ Resolve"
  button and the currently-broken duplicate "✔️ Resolved" button) but there is
  **no `.btn-success` definition anywhere in `styles.css`**, so these buttons
  currently render as unstyled default browser buttons. Since this plan reuses
  `class="btn-success"` for the new working "✔️ Resolved" per-row action,
  **add a `.btn-success` (+ `.btn-success:hover`) rule to `styles.css`**,
  following the same structure/pattern as the neighboring `.btn-info`/`.btn-edit`
  rules (gradient background, white text, bold, `border-radius: 0.5rem`,
  `box-shadow`, `padding: 0.35rem 0.65rem`), using a green tone consistent with
  the existing `.badge-success`/`.badge-status-resolved` success semantics (e.g.
  reuse existing `--btn-*` CSS custom properties in a green family if a suitable
  one already exists, otherwise define new `--btn-green-*` variables next to the
  other `--btn-*` tokens). This fixes the button styling for **both** the
  existing error-table "Resolve" action and the new duplicate "Mark Resolved"
  action in one change.
- **Status column/badges need no new CSS.** `.badge`, `.badge-status-open`,
  `.badge-status-resolved`, and `.resolved-row` already exist and are already
  used exactly this way by `error-table` — the new duplicate Status column
  reuses them as-is (only new `DUPLICATE_STATUS_*` `ControllerSettings`
  constants are needed on the Java/Thymeleaf side, no CSS changes).
- **New requirement: a "Select" checkbox column.** This is the first checkbox
  column added to any `.styled-table` in the codebase — there is currently no
  checkbox styling at all in `styles.css`. Add a small, reusable rule (not
  page-specific) such as:
  ```css
  .styled-table td.select-cell,
  .styled-table th.select-cell {
      text-align: center;
      width: 2.5rem;
  }
  ```
  and use `class="select-cell"` on the new Select `<th>`/`<td>` in
  `fragments/tables::duplicate-table`, so the checkbox column is centered/sized
  consistently and the rule is available for any future checkbox-selection table.
- **Minor inline-style cleanup (required for the new form, optional elsewhere,
  in scope since `tables.html` is already being edited)**: existing per-row
  action forms in `fragments/tables.html` use `style="display: inline;"`
  directly in the markup (e.g. the error "Resolve"/"Delete" forms). This is
  inconsistent with the style-sd.md rule against inline/page-specific styling.
  The new "✔️ Resolved" form uses `class="inline-form"` (a small reusable
  utility class, `.inline-form { display: inline; }`, added to `styles.css`)
  instead of a new inline `style` attribute; optionally migrate the neighboring
  existing inline styles in the same file to the new class while touching this
  area (nice-to-have beyond the new form, not required for this feature to
  function).
- No other new CSS classes are needed: the Answers column reuses `.text-cell`
  exactly as `author-details.html` and the "Source Question" table already do,
  and the "Delete All Duplicates" `<form>` reuses `.form-actions` for layout like
  the existing "Remove Selected Duplicate Links" form.

## Proposed Changes

### New `ControllerSettings` Constants Required

All new backend-facing route paths and flash messages introduced by this plan
must be added as constants in `ControllerSettings.java` (`myquiz-api` module),
following the existing naming/grouping conventions in that file, instead of
inline string literals in controllers/services:

| Constant | Value | Used by |
|---|---|---|
| `API_QUESTION_BANKS_DUPLICATES_REMOVE_ALL_BY_ID` | `"/{id:\\d+}/duplicates/remove-all"` | REST `QuestionController` `@PostMapping`, `QuestionApi` |
| `API_QUESTION_DUPLICATES_REMOVE_ALL` | `"/duplicates/remove-all"` | `ThyQuestionController` REST call to `apiBaseUrl + API_QUESTIONS + "/" + id + API_QUESTION_DUPLICATES_REMOVE_ALL` |
| `MSG_ALL_DUPLICATES_REMOVED_SUCCESS` | e.g. `"All duplicate links removed successfully."` | `ThyQuestionController.removeAllDuplicates` flash message |
| `MSG_ALL_DUPLICATES_REMOVED_FAILED` | e.g. `"Failed to remove all duplicate links."` | `ThyQuestionController.removeAllDuplicates` flash message |
| `DUPLICATE_STATUS_OPEN` | `"OPEN"` | `QuestionDuplicate.status` default, `duplicate-table` Status badge — mirrors `ERROR_STATUS_OPEN` |
| `DUPLICATE_STATUS_RESOLVED` | `"RESOLVED"` | `QuestionDuplicationService.markDuplicateResolved`, `duplicate-table` Status badge — mirrors `ERROR_STATUS_RESOLVED` |
| `API_DUPLICATES` | `"/api/duplicates"` | New standalone REST controller base path — mirrors `QuestionErrorController`'s `/api/errors` |
| `API_DUPLICATES_RESOLVE_BY_ID` | `"/{id}/resolve"` | New REST `@PutMapping` — mirrors `QuestionErrorApi`'s `/{id}/resolve` |
| `MSG_DUPLICATE_RESOLVED_SUCCESS` | e.g. `"Duplicate marked as resolved."` | `ThyQuestionDuplicateController.markResolved` flash message |
| `MSG_DUPLICATE_RESOLVED_FAILED` | e.g. `"Failed to mark duplicate as resolved."` | `ThyQuestionDuplicateController.markResolved` flash message |

No other new constants are required — every other route path, request-param
name, and model-attribute key touched by this feature (`ATTR_TOTAL_PAGES`,
`ATTR_TOTAL_ELEMENTS`, `ATTR_CURRENT_PAGE`, `ATTR_PAGE_SIZE`, `ATTR_PAGE_NUMBER`,
`ATTR_BACK_URL`, `ATTR_COURSE_ID`, `ATTR_AUTHOR_ID`, `ATTR_QUESTION_BANK_ID`,
`ATTR_SELECTED_TYPE`, `ATTR_SELECTED_COURSE_ID`, `ATTR_SELECTED_AUTHOR_ID`,
`ATTR_SELECTED_QUESTION_BANK_ID`, `ATTR_DUPLICATE_QUESTION_IDS`,
`API_QUESTION_BANKS_DUPLICATES_REMOVE_BY_ID`, `API_QUESTION_DUPLICATES_REMOVE`,
`MSG_DUPLICATES_REMOVED_SUCCESS`, `MSG_DUPLICATES_REMOVED_FAILED`,
`MSG_SELECT_AT_LEAST_ONE_DUPLICATE`, `MSG_UNABLE_LOAD_DUPLICATES`,
`VIEW_QUESTION_DUPLICATES`, `ATTR_SUCCESS_MESSAGE`, `ATTR_ERROR_MESSAGE`,
`VIEW_REDIRECT_AUTH_LOGIN`) already exists in `ControllerSettings` and must be
reused as-is — **no raw string literals for these should be added** in any
modified Java file. (Thymeleaf template `th:action` URLs remain hardcoded path
literals matching the `@PostMapping`/`@GetMapping` values, consistent with the
existing style used for e.g. `/questions/{id}/duplicates/remove`,
`/errors/{id}/resolve` elsewhere in these templates — only Java controller/service
code is expected to reference `ControllerSettings` constants directly.)

### Backend Components

**[NEW field] `QuestionDuplicate.java` (entity)**
- Add `@Column(name = "status", length = 20) private String status =
  ControllerSettings.DUPLICATE_STATUS_OPEN;`, mirroring `QuestionError.status`
  exactly (same column length/type/default pattern). Relies on
  `spring.jpa.hibernate.ddl-auto=update` to add the column automatically — no
  manual migration script needed (see **Codebase Findings**).

**[MODIFY] `QuestionDuplicateDto.java`**
- Add a `status` (`String`) field with `@JsonProperty("status")`, alongside the
  existing fields, so the Status badge can be rendered in the Thymeleaf table.

**[MODIFY] `QuestionDuplicateMapper.java`**
- In `toDuplicateDto(...)`, set
  `duplicateDto.setStatus(duplicateLink != null ? duplicateLink.getStatus() :
  ControllerSettings.DUPLICATE_STATUS_OPEN);` — this is the single centralized
  place that needs updating for both `question.getDuplicates()` and the
  `question-bank-extended-details.html` course-wide view to see the new field.

**[MODIFY] `QuestionDuplicationService.java`**
- Add a new `@Transactional` method
  `removeSpecificDuplicateAssociations(Long questionId, List<Long> duplicateIds)`
  that:
  - Returns/no-ops if `questionId` is null or `duplicateIds` is null/empty.
  - For each `duplicateId` in `duplicateIds`, deletes the link between
    `questionId` and `duplicateId` in **both directions** using
    `deleteByQuestionIdAndDuplicateQuestionId(questionId, duplicateId)` and
    `deleteByQuestionIdAndDuplicateQuestionId(duplicateId, questionId)`.
  - Tracks the set of affected question IDs (the primary `questionId` plus each
    selected `duplicateId`) and calls `cleanupDuplicateErrors(affectedQuestionIds)`
    once at the end, mirroring the pattern used in the existing
    `removeDuplicateAssociations` method.
  - Flushes the repository only if at least one row was actually removed.
  - Logs entry (`questionId`, `duplicateIds`) at `info`, a `warn` on the
    null/empty no-op guard, and the number of links actually removed at `info` on
    completion (see **Logging** section above).
- Add a new `@Transactional` method `markDuplicateResolved(Long duplicateLinkId)`
  that loads the `QuestionDuplicate` by its own `id` (not a question ID),
  returns/no-ops (or throws a not-found signal the REST layer maps to 404) if it
  doesn't exist, sets `status = ControllerSettings.DUPLICATE_STATUS_RESOLVED`,
  and saves it via `questionDuplicateRepository.save(...)` — **the link/rows are
  not deleted**, mirroring `QuestionErrorService.resolveErrorById` exactly. Logs
  entry/outcome at `info`, and `warn` if the link ID doesn't exist.

**[MODIFY] `QuestionService.java`**
- Update `removeDuplicationLinks(Long questionId, List<Long> duplicateIds)` to call
  `questionDuplicationService.removeSpecificDuplicateAssociations(questionId, duplicateIds)`
  instead of `removeDuplicateAssociations(questionId)`. Keep the existing
  null/empty guard, logging, `@CacheEvict`, and exception handling unchanged.
- Add a new method `removeAllDuplicationLinks(Long questionId)` (mirrors
  `removeDuplicationLinks`'s structure/annotations: same `@CacheEvict` value list,
  `@Transactional`, try/catch with logging) that calls the **already-existing**
  `questionDuplicationService.removeAllDuplicateAssociationsForQuestion(questionId)`
  and returns `true`/`false` for success/failure. No new duplication-removal logic
  is required in `QuestionDuplicationService` for this — it is reused as-is. Add
  `info`/`error` logging matching the existing `removeDuplicationLinks` pattern.

**[MODIFY] `QuestionController.java` (REST) / `QuestionApi.java`**
- Add a new endpoint, e.g. `@PostMapping(ControllerSettings.API_QUESTION_BANKS_DUPLICATES_REMOVE_ALL_BY_ID)`
  mapped to `/{id:\d+}/duplicates/remove-all`, calling
  `questionService.removeAllDuplicationLinks(id)` and returning `204 No Content` on
  success / `404` if the question wasn't found, following the same structure as
  `removeQuestionDuplicates`. Declare the corresponding method signature in the
  `QuestionApi` interface with matching `@Operation`/`@ApiResponses` Swagger
  annotations.

**[NEW] `QuestionDuplicateController.java` (REST) / `QuestionDuplicateApi.java`**
- Add a small standalone REST controller mapped at
  `ControllerSettings.API_DUPLICATES` (`"/api/duplicates"`), mirroring
  `QuestionErrorController`'s structure exactly (it cannot live under
  `QuestionController`'s `/api/questions` mapping since a duplicate link is
  addressed by its own ID, not a question ID):
  `@PutMapping(ControllerSettings.API_DUPLICATES_RESOLVE_BY_ID)` (`/{id}/resolve`)
  calling `questionDuplicationService.markDuplicateResolved(id)` and returning
  `204 No Content` on success / `404` if the link doesn't exist / `500` on
  unexpected errors — same try/catch/status-code structure as
  `QuestionErrorController.resolveError`. Add a matching `QuestionDuplicateApi`
  interface method with `@Operation`/`@ApiResponses` Swagger annotations,
  following the `QuestionErrorApi` pattern.

### Frontend Components

**[MODIFY] `ThyQuestionController.java`**
- In `showQuestionDuplicates`, after retrieving `question.getDuplicates()`, resolve
  the effective page/pageSize via
  `PaginationParams pagination = PaginationSupport.normalize(page, pageSize);`
  (same call already used by `QuestionService.getQuestionsFiltered` and
  `QuestionController`), instead of hand-rolled null/default handling. Use
  `pagination.page()` / `pagination.pageSize()` for all subsequent calculations,
  and pass these normalized values into `buildNavigationContext` so the
  back-link/action URLs stay consistent with the page actually rendered.
- Compute `totalElements` (full duplicates list size) and `totalPages`
  (`ceil(totalElements / pagination.pageSize())`, minimum 1). Clamp the normalized
  page into `[1, totalPages]` before slicing the sublist (in-memory pagination —
  `question.getDuplicates()` is fetched in full from the API in one call, then
  sliced locally; no new API/service pagination is introduced).
- Add model attributes `ControllerSettings.ATTR_TOTAL_PAGES` and
  `ControllerSettings.ATTR_TOTAL_ELEMENTS` (both **already exist** — reuse them,
  do not hardcode `"totalPages"`/`"totalElements"` string literals) in addition to
  the existing `ATTR_CURRENT_PAGE`/`ATTR_PAGE_SIZE` (now sourced from
  `pagination.page()` / `pagination.pageSize()`), so the Thymeleaf page can render
  pagination controls, and put the **paginated sublist** (not the full list) into
  `ATTR_DUPLICATES`/`model.duplicates`.
- Add `catch (HttpClientErrorException.Forbidden ex)` blocks (before the existing
  `NotFound`/generic `Exception` catches) to `showQuestionDuplicates`,
  `removeSelectedDuplicates`, and the new `removeAllDuplicates` handler, calling
  `sessionService.invalidateCurrentSession()` and returning
  `ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN`, matching the existing pattern in
  `showQuestionForEdit` (see **Exception Handling & User Feedback** section).
- Apply the same `PaginationSupport.normalize(page, pageSize)` call in
  `removeSelectedDuplicates` (and anywhere else in this controller that currently
  reads raw `page`/`pageSize` request params for this feature) so the redirect
  back to `/questions/{id}/duplicates` after removal uses consistent, normalized
  values rather than the raw (possibly null) request parameters.
- Add a new handler `removeAllDuplicates(Long id, Integer page, Integer pageSize,
  String courseIdParam, String authorIdParam, String type, String
  questionBankIdParam, String backUrl, RedirectAttributes redirectAttributes)`
  mapped `@PostMapping` to `ControllerSettings.API_QUESTION_BANKS_DUPLICATES_REMOVE_ALL_BY_ID`,
  mirroring `removeSelectedDuplicates`'s structure (session validation, parsing
  optional filter params, `PaginationSupport.normalize`) but calling the REST
  endpoint `apiBaseUrl + API_QUESTIONS + "/" + id + API_QUESTION_DUPLICATES_REMOVE_ALL`
  with no request body, wrapped in the same
  `Forbidden` → `NotFound` → generic `Exception` catch chain described in the
  **Exception Handling & User Feedback** section. On success, flash
  `MSG_ALL_DUPLICATES_REMOVED_SUCCESS`; on `NotFound`/generic failure, flash
  `MSG_ALL_DUPLICATES_REMOVED_FAILED` (with `log.error(...)` including the
  exception for the generic case). Redirect back to
  `/questions/{id}/duplicates` via `buildQuestionDuplicatesRedirectUrl`, same as
  the existing selective-removal handler, so the message renders in the existing
  `successMessage`/`errorMessage` alert `<div>`s on that page.

**[NEW] `ThyQuestionDuplicateController.java`**
- Add a new small Thymeleaf controller, `@RequestMapping("/duplicates")`
  (mirroring `ThyErrorController`'s `@RequestMapping("/errors")` structure), with
  a single handler:
  `@PostMapping("/{id}/mark-resolved")` — matching the path the `duplicate-table`
  fragment **already** posts to today — accepting `@PathVariable Long id`
  (the `QuestionDuplicate` link ID) plus the new hidden request params
  (`primaryQuestionId`, `backUrl`, `courseId`, `authorId`, `type`,
  `questionBankId`, `page`, `pageSize`) and `RedirectAttributes`. It:
  - Validates the session (`sessionService.validateSessionOrRedirect()`).
  - Calls the new REST endpoint
    `apiBaseUrl + ControllerSettings.API_DUPLICATES + "/" + id + "/resolve"` via
    `PUT`.
  - Uses the same `Forbidden` → `NotFound` → generic `Exception` catch chain
    (see **Exception Handling & User Feedback**), flashing
    `MSG_DUPLICATE_RESOLVED_SUCCESS` / `MSG_DUPLICATE_RESOLVED_FAILED`.
  - Redirects back to `/questions/{primaryQuestionId}/duplicates` (reusing
    `buildQuestionDuplicatesRedirectUrl`-style logic, or delegating to a shared
    helper) when `primaryQuestionId` is present (the `question-duplicates.html`
    context); if absent (a future/other caller with `showActions=true` but no
    primary question), falls back to `backUrl` if present, else `/questions`.

**[MODIFY] `styles.css`**
- Add `.btn-success` / `.btn-success:hover` rules (green gradient, matching the
  structure of `.btn-info`/`.btn-edit`), fixing the pre-existing unstyled-button
  gap described in **Styling** above — benefits both the error-table "Resolve"
  button and the new duplicate "Resolved" button.
- Add `.styled-table td.select-cell` / `.styled-table th.select-cell` (centered,
  fixed narrow width) for the new checkbox Select column.
- (Optional cleanup, but `.inline-form` itself is now required) Add
  `.inline-form { display: inline; }` utility class — the new "✔️ Resolved" form
  above uses `class="inline-form"` instead of an inline `style` attribute, so
  this class must be added. While in the area, optionally migrate the
  neighboring existing `style="display: inline;"` attributes on the other
  per-row action forms in `fragments/tables.html` (error "Resolve"/"Delete",
  duplicate bulk-removal forms) to the same class for consistency (nice-to-have,
  not required for this feature to function).
- No other new rules needed — all remaining new/changed elements reuse existing
  classes (`.text-cell`, `.action-cell`, `.form-actions`, `.btn-delete`,
  `.btn-outline`, `.pagination-controls`, `.total-elements`, `.alert`).

**[MODIFY] `tables.html`**
- Add a **Select** checkbox column, rendered only `th:if="${showSelection}"`, with
  input `name="duplicateQuestionIds"` and `value="${duplicate.questionId}"` (matching
  the `ATTR_DUPLICATE_QUESTION_IDS` request parameter name already expected by
  `removeSelectedDuplicates`), using `class="select-cell"` on both the `<th>` and
  `<td>` (new class added in `styles.css`, see **Styling** above).
- Add an **Answers** column that displays the duplicate's responses based on
  `duplicate.type`, using the same **numbered-line** layout as `author-details.html`
  (not the plain unlabeled `<div>`s currently used in the "Source Question" table
  of `question-duplicates.html`):
  ```html
  <td class="text-cell" th:if="${duplicate.type == T(com.unitbv.myquiz.api.types.QuestionType).MULTICHOICE}">
      <div><span>1. </span><span th:text="${duplicate.response1}"></span></div>
      <div><span>2. </span><span th:text="${duplicate.response2}"></span></div>
      <div><span>3. </span><span th:text="${duplicate.response3}"></span></div>
      <div><span>4. </span><span th:text="${duplicate.response4}"></span></div>
  </td>
  <td class="text-cell" th:if="${duplicate.type != T(com.unitbv.myquiz.api.types.QuestionType).MULTICHOICE}">
      <span th:text="${duplicate.response1}"></span> (TRUE/FALSE)
  </td>
  ```
- Add a new fragment parameter `primaryQuestionId` (the ID of the question whose
  duplicates page is being rendered) so the per-row "Mark Resolved" action below
  can target the correct unlink endpoint. Pass `${question.id}` for this parameter
  from `question-duplicates.html`, and `${null}` from
  `question-bank-extended-details.html` (which already renders with
  `showActions=false`, so the action button won't be shown there regardless).
- Wrap the existing **Actions** `<th>` and `<td>` in `th:if="${showActions}"` (both
  header and body cell), instead of always rendering them.
- Add a new **Status** column (rendered unconditionally, like the error-table's
  Status column — not gated by `showSelection`/`showActions`) showing an
  `OPEN`/`RESOLVED` badge, reusing the **exact same pattern, classes, and
  `ControllerSettings` constants as `error-table`**, just with the new
  `DUPLICATE_STATUS_*` constants instead of `ERROR_STATUS_*`:
  ```html
  <td>
      <span th:if="${duplicate.status == T(com.unitbv.myquiz.api.settings.ControllerSettings).DUPLICATE_STATUS_OPEN}" class="badge badge-status-open">OPEN</span>
      <span th:if="${duplicate.status == T(com.unitbv.myquiz.api.settings.ControllerSettings).DUPLICATE_STATUS_RESOLVED}" class="badge badge-status-resolved">RESOLVED</span>
      <span th:if="${duplicate.status == null}" class="badge badge-status-open">OPEN</span>
  </td>
  ```
  Also apply the same row-highlighting as `error-table`, appending `resolved-row`
  on the `<tr th:each="duplicate : ...">` when
  `duplicate.status == T(...).DUPLICATE_STATUS_RESOLVED`, reusing the existing
  `.resolved-row` CSS class as-is (no new styling needed for the Status column —
  see **Styling** above).
- **Implement the "✔️ Resolved" button for real** (it already targets the right
  path, `/duplicates/{id}/mark-resolved` with `id=${duplicate.id}` — only the
  backend was missing). This **flags the duplicate pair as resolved without
  deleting the link**, exactly like resolving a question error — it is
  **unrelated** to the selective-unlink flow used by "Remove Selected Duplicate
  Links"/"Delete All Duplicates". Add the hidden fields needed to redirect back
  to the correct duplicates page afterward (currently missing from the form):
  ```html
  <form method="post" th:action="@{/duplicates/{id}/mark-resolved(id=${duplicate.id})}" class="inline-form"
        th:if="${duplicate.status != T(com.unitbv.myquiz.api.settings.ControllerSettings).DUPLICATE_STATUS_RESOLVED}">
      <input type="hidden" name="primaryQuestionId" th:value="${primaryQuestionId}" />
      <input type="hidden" name="backUrl" th:value="${backToQuestionsUrl}" />
      <input type="hidden" name="courseId" th:value="${selectedCourseId}" />
      <input type="hidden" name="questionBankId" th:value="${selectedQuestionBankId}" />
      <input type="hidden" name="authorId" th:value="${selectedAuthorId}" />
      <input type="hidden" name="type" th:value="${selectedType}" />
      <input type="hidden" name="page" th:value="${currentPage}" />
      <input type="hidden" name="pageSize" th:value="${pageSize}" />
      <button type="submit" class="btn btn-success btn-small"
              onclick="return confirm('Mark this duplicate pair as resolved (it will remain linked, just flagged as reviewed)?')">✔️ Resolved</button>
  </form>
  ```
  (the `th:if` hides the button once already resolved, since there's nothing left
  to do — the Status badge alone communicates the resolved state at that point).
  This requires the **new** `ThyQuestionDuplicateController` (see Frontend
  Components below) — it is a materially different flow from "Remove Selected
  Duplicate Links" and must not reuse `removeSpecificDuplicateAssociations`.
- Fix the empty-row `colspan` to be computed dynamically from the active columns
  (base columns + 1 for Status + 1 if `showSelection` + 1 if `showActions`)
  instead of the hardcoded `6`.
- Verify/update the other current caller of this fragment,
  `question-bank-extended-details.html`, still passes valid values for the new
  parameters (e.g., explicit `showSelection=false`, `primaryQuestionId=${null}`)
  so it doesn't break when the fragment signature changes. It will now also show
  the new Status column (unconditional), which is desirable there too since
  `QuestionDuplicateDto.status` is populated by the same centralized mapper.

**[MODIFY] `question-duplicates.html`**
- Pass the new `primaryQuestionId=${question.id}` parameter through to the
  `fragments/tables::duplicate-table` fragment call.
- Add a **"🗑️ Delete All Duplicates"** button next to the existing
  "✖️ Remove Selected Duplicate Links" button, as its **own separate `<form>`**
  (not nested inside the checkbox-selection form, so it can't accidentally submit
  stray checkbox state and works even when the duplicates list spans multiple
  pages):
  ```html
  <form th:if="${duplicates != null and !#lists.isEmpty(duplicates)}"
        th:action="@{'/questions/' + ${question.id} + '/duplicates/remove-all'}"
        method="post" class="form-actions">
      <input type="hidden" name="backUrl" th:value="${backToQuestionsUrl}" />
      <input type="hidden" name="courseId" th:value="${selectedCourseId}" />
      <input type="hidden" name="questionBankId" th:value="${selectedQuestionBankId}" />
      <input type="hidden" name="authorId" th:value="${selectedAuthorId}" />
      <input type="hidden" name="type" th:value="${selectedType}" />
      <input type="hidden" name="page" th:value="${currentPage}" />
      <input type="hidden" name="pageSize" th:value="${pageSize}" />
      <button type="submit" class="btn btn-delete"
              onclick="return confirm('Delete ALL duplicate links for this question? This action cannot be undone and affects duplicates on every page, not just the current one.')">
          🗑️ Delete All Duplicates
      </button>
  </form>
  ```
- Add a pagination controls block below the duplicates table, matching the
  structure/CSS classes used in `question-list.html` (Prev/Next/First/Last links,
  current page / total pages display), targeting
  `/questions/{id}/duplicates` and preserving `courseId`, `questionBankId`,
  `authorId`, `type`, and `backUrl` query parameters.
- Add a total-elements count display block (`Total: <totalElements> duplicate(s)
  found`), matching the pattern in `question-list.html`.
- Ensure pagination links are rendered outside/independent of the selection
  `<form>` (or use `formaction`/separate GET links) so paging doesn't submit the
  checkbox selection form.
- (Optional consistency improvement, low risk) Update the "Source Question"
  table's existing "Responses" row to use the same numbered-line layout
  (`1. `/`2. `/`3. `/`4. ` prefixes) as the new Answers column and
  `author-details.html`, instead of the current unlabeled `<div>`s, so both
  sections of the page present responses identically.

## Verification Plan

### Automated Tests
- Run `mvn test` in `myquiz-app` and `myquiz-thymeleaf` modules to verify all unit
  and integration tests compile and pass.
- Add/extend unit tests for `QuestionDuplicationService.removeSpecificDuplicateAssociations`
  covering: only selected IDs are removed (others remain linked), links stored in
  either column order are removed, and `cleanupDuplicateErrors` is invoked for all
  affected questions.
- Add/extend a `QuestionService` unit test asserting `removeDuplicationLinks`
  delegates to `removeSpecificDuplicateAssociations` with the exact `duplicateIds`
  passed in (not the "remove all" method).
- Update/add test cases (e.g. in `ThyQuestionControllerBlankAuthorIdTest.java` or a
  new controller test) to verify the new pagination model attributes
  (`totalPages`, `totalElements`, `currentPage`, `pageSize`) are populated
  correctly for `showQuestionDuplicates`, including edge cases (null page/pageSize
  falling back to `PaginationSupport` defaults, empty duplicates list, and a
  requested page beyond the computed `totalPages`).
- Add/extend a `QuestionController` (REST) integration test verifying
  `POST /questions/{id}/duplicates/remove` with a subset of duplicate IDs leaves
  the non-selected duplicate links intact.
- Add a `QuestionService` unit test for `removeAllDuplicationLinks` asserting it
  delegates to `questionDuplicationService.removeAllDuplicateAssociationsForQuestion`
  and removes every link for the question (including ones beyond a single page).
- Add a `QuestionController` (REST) integration test for the new
  `POST /questions/{id}/duplicates/remove-all` endpoint verifying all duplicate
  links for the question are removed, including duplicates that would be on a
  different page than the one currently displayed.
- Add a Thymeleaf controller test for `removeAllDuplicates` verifying it calls the
  remove-all REST endpoint, sets the correct flash message on success/failure, and
  redirects back to `/questions/{id}/duplicates` preserving filter parameters.
- Add a `QuestionDuplicationService` unit test for `markDuplicateResolved`
  asserting: the link's `status` is set to `DUPLICATE_STATUS_RESOLVED` and saved
  (via a `save`/`findById` spy on the repository), the link itself is **not**
  deleted (still resolvable by ID afterward), and a not-found ID results in the
  expected exception/empty-result handling (mirroring
  `QuestionErrorService.resolveErrorById`'s existing test coverage).
- Add a `QuestionDuplicateMapper` unit test asserting `toDuplicateDto` correctly
  maps the entity's `status` field to the DTO (including the default-to-OPEN
  case for a null/legacy status value on pre-existing rows).
- Add a `QuestionDuplicateController` (REST, `/api/duplicates`) integration test
  for `PUT /api/duplicates/{id}/resolve` verifying: 200 + updated status on
  success, 404 for an unknown ID, and that the underlying link row still exists
  in the database afterward (not deleted).
- Add a Thymeleaf controller test for the new `ThyQuestionDuplicateController.markResolved`
  verifying: it calls the resolve REST endpoint, sets the correct flash message
  on success/failure, redirects to `/questions/{primaryQuestionId}/duplicates`
  when that param is present, and applies the `Forbidden`/`NotFound`/generic
  exception handling described above.
- Add/extend unit tests asserting the new log statements fire on the relevant
  paths where feasible (e.g., using a log capture appender for the `warn` on
  null/empty `duplicateIds`), or at minimum manually confirm via log output during
  manual verification below.
- Add Thymeleaf controller tests covering the exception-handling gap fix:
  - `showQuestionDuplicates` receiving `HttpClientErrorException.Forbidden` from
    the REST call invalidates the session and returns
    `VIEW_REDIRECT_AUTH_LOGIN` (currently untested/unhandled — falls through to
    the generic error path today).
  - `removeSelectedDuplicates` and `removeAllDuplicates` receiving
    `HttpClientErrorException.Forbidden` behave the same way.
  - `removeAllDuplicates` receiving `HttpClientErrorException.NotFound` sets
    `ATTR_ERROR_MESSAGE` = `MSG_QUESTION_NOT_FOUND` as a flash attribute and
    redirects back to `/questions/{id}/duplicates` (not `VIEW_QUESTION_LIST`).
  - `removeAllDuplicates` receiving a generic `Exception` logs the error and
    flashes `MSG_ALL_DUPLICATES_REMOVED_FAILED`, still redirecting back to the
    duplicates page (so the error alert `<div>` renders there).
  - `removeAllDuplicates` on success flashes `ATTR_SUCCESS_MESSAGE` =
    `MSG_ALL_DUPLICATES_REMOVED_SUCCESS`.

### Manual Verification
- Access
  `/questions/22164/duplicates?courseId=&questionBankId=52&authorId=&type=&page=1&pageSize=10`
  and inspect:
  - Checkbox selection column present, centered/sized via `.select-cell`, and
    functional.
  - Answers column displaying choice/true-false responses correctly per question
    type, on separate numbered lines (matching `author-details.html` style).
  - Pagination links/total-count at the bottom, correctly preserving filter
    parameters when navigating pages.
  - Per-row "✔️ Resolved" button renders with proper green button styling (not
    an unstyled default button — confirms the `.btn-success` fix), matching the
    visual weight of other `.btn-*` actions on the page.
  - New **Status** column shows an `OPEN` badge (grey/neutral, via
    `.badge-status-open`) for unresolved duplicates.
  - "🗑️ Delete All Duplicates" button renders with the same `.btn-delete` styling
    used elsewhere (e.g. question delete actions), visually distinct from the
    "Remove Selected Duplicate Links" save-style button.
- Test selecting a single duplicate (out of several) and clicking "Remove Selected
  Duplicate Links"; confirm only that duplicate is unlinked and the others remain
  listed after redirect.
- Test clicking "🗑️ Delete All Duplicates" (with a question that has duplicates
  spanning more than one page); confirm the confirmation dialog appears, and after
  confirming, **all** duplicates are unlinked (not just the current page's), with
  an appropriate success message and redirect.
- Test clicking the per-row "✔️ Resolved" button on one duplicate; confirm:
  - The row's Status badge changes from `OPEN` to `RESOLVED` (green, via
    `.badge-status-resolved`) and the row gets the `.resolved-row` highlight,
    matching the error-table's existing visual pattern.
  - The "✔️ Resolved" button disappears for that row afterward (since the
    `th:if` hides it once resolved).
  - The duplicate **remains listed** in the table (and remains linked in the
    database) — it is only flagged, not unlinked — distinguishing it clearly
    from "Remove Selected Duplicate Links"/"Delete All Duplicates".
  - The success message renders via the existing `successMessage` alert `<div>`
    after redirect back to the same page/filter state.
- Verify `question-bank-extended-details.html`'s use of the `duplicate-table`
  fragment still renders correctly after the fragment changes (no Select/Actions
  columns shown there, since `showSelection`/`showActions` are `false`).
- Verify the error-table's existing "✔️ Resolve" button (in `error-list.html` /
  other error-table callers) now also renders with proper green `.btn-success`
  styling as a side effect of the CSS fix, with no visual regression.
- Simulate failure/error feedback end-to-end:
  - Trigger a "select at least one duplicate" validation error (submit the
    selection form with nothing checked) and confirm the message renders in the
    `errorMessage` alert `<div>` on `question-duplicates.html`.
  - Stop/misconfigure the backing REST API (or point `apiBaseUrl` at an invalid
    host) temporarily and click "Remove Selected Duplicate Links" / "Delete All
    Duplicates" / "✔️ Resolved"; confirm a friendly error message appears in the
    `errorMessage` div (not a stack trace or blank page).
  - Simulate an expired session (e.g., manually invalidate/expire the auth token)
    and confirm the user is redirected to `/auth/login` instead of seeing a
    generic error, for both the GET duplicates page and each POST action.
  - Successfully remove a duplicate (selected or all) or resolve a duplicate pair
    and confirm the `successMessage` alert `<div>` renders the expected text after
    the redirect.
- Tail the application logs while performing the manual steps above and confirm:
  entry/exit logs appear for `removeSpecificDuplicateAssociations`,
  `removeAllDuplicateAssociationsForQuestion`, and `markDuplicateResolved` with
  correct `questionId`/link `id`/counts, the pagination debug/info log shows the
  correct normalized page and totals (and the clamped value when requesting an
  out-of-range page), and no stack traces/errors appear on the happy paths.
