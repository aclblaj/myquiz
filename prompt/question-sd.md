# Question Software Design
## 1. Overview
This document defines the current question-management feature in MyQuiz.
It covers question listing, filtering, viewing, create/update/delete, correction support, and question-bank/author-scoped navigation.
## 2. Functional Scope
### 2.1 Main Features
- list and filter questions
- view questions by question bank
- view questions by author within a question bank context
- open read-only question details
- create and edit multiple-choice and true/false questions
- delete questions
- support question correction workflows and duplicate unlinking support through related APIs
### 2.2 Main End-to-End Calls
| Operation | Template | Thymeleaf Route | Backend Route | Service Entry Point |
|---|---|---|---|---|
| Filter/list questions | `question-list.html` | `/questions/...` | `POST /api/questions/filter` | `QuestionService` filter/list methods |
| View question | `question-view.html` | `GET /questions/{id}` | `GET /api/questions/{id}` | `QuestionService.getQuestionById(...)` |
| View by question bank | `question-list.html` | question-bank-scoped route | `GET /api/questions/question-banks/{questionBankId}` | `QuestionService.getQuestionsByQuestionBankId(...)` |
| Create question | editor templates | `POST /questions` | `POST /api/questions` | `QuestionService.createQuestion(...)` |
| Update question | correction/editor routes | `POST /questions/{id}/edit` | `PUT /api/questions/{id}` | `QuestionService.updateQuestion(...)` |
| Delete question | `question-list.html` | `POST /questions/{id}/delete` | `DELETE /api/questions/{id}` | `QuestionService.deleteQuestion(...)` |
## 3. Architecture
### 3.1 Main Components
- `QuestionController`
- `QuestionService`
- `QuestionCorrectionService`
- `ThyQuestionController`
- question templates under `myquiz-thymeleaf/src/main/resources/templates/`
### 3.2 Domain Relationships
Questions belong to `QuestionBankAuthor` and therefore inherit context from:
- author
- question bank
- course through the question bank
Questions may also have related errors, duplicates, and answer-reference data.
## 4. Data Model and DTOs
Primary question contracts include:
- `QuestionDto`
- `QuestionCorrectionDto`
- `QuestionFilterRequestDto`
- `QuestionFilterResponseDto`
- `DuplicateUnlinkRequestDto`
## 5. Flows
### 5.1 Filter and List
1. UI submits question filters.
2. Thymeleaf controller builds `QuestionFilterRequestDto`.
3. Backend applies question specifications and returns `QuestionFilterResponseDto`.
4. UI renders the current page and selected context.
### 5.2 View by Question Bank / Author Scope
1. User navigates from a question bank or author context.
2. Backend resolves question-bank and author metadata.
3. Questions are returned already scoped for that context.
### 5.3 Create and Update
- editor templates differ by `QuestionType`
- backend create/update routes use `QuestionDto`
- correction workflows can use specialized DTOs/services while still ending in persisted question changes
### 5.4 Delete
1. User posts a delete action from list/detail UI.
2. Thymeleaf forwards authorized delete call.
3. Backend deletes the question and returns `204` or `404`.
## 6. Permissions and Security
- question pages require valid session state in Thymeleaf
- backend authorization controls final access to create/update/delete/correction actions
- question actions may surface additional menu/button visibility based on permissions derived from JWT claims
## 7. UI, API, and Service Responsibilities
### 7.1 Thymeleaf Layer
- render list, editor, view, and correction pages
- preserve filter and back-navigation context
- translate form posts into authorized REST requests
### 7.2 API Layer
- expose CRUD/filter endpoints under `/api/questions`
- shape DTO responses for both generic and scoped question views
- translate not-found and validation outcomes into stable HTTP responses
### 7.3 Service Layer
- implement CRUD operations
- apply question specifications for filtering
- coordinate correction-related logic
- handle duplicate unlinking and other question-adjacent business rules via related services
## 8. Validation and Error Handling
- null or malformed request DTOs should fail fast as bad requests
- missing questions should return `404`
- Thymeleaf flows should redirect or repaint with user-facing messages rather than exposing low-level errors
- filter defaults should be normalized before hitting repositories
## 9. Key Decisions
- keep question filtering DTO-driven for extensibility
- keep question-bank and author context available in filter responses to simplify UI composition
- separate correction logic from generic CRUD where specialized processing is needed
- keep question editing split by question type at the UI layer while sharing backend model and service logic
## 10. Implementation Notes
- Backend controller:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/controller/QuestionController.java`
- Backend services:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/services/QuestionService.java`
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/services/QuestionCorrectionService.java`
- Thymeleaf controller:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyQuestionController.java`
- Main templates:
  - `myquiz-thymeleaf/src/main/resources/templates/question-list.html`
  - `myquiz-thymeleaf/src/main/resources/templates/question-view.html`
  - `myquiz-thymeleaf/src/main/resources/templates/question-editor-mc.html`
  - `myquiz-thymeleaf/src/main/resources/templates/question-editor-tf.html`
  - `myquiz-thymeleaf/src/main/resources/templates/question-correction.html`
Related docs:
- `prompt/question-bank-sd.md`
- `prompt/duplicate-sd.md`
- `prompt/author-error-sd.md`
