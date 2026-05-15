# Quiz Software Design Operations Handling

## 1. Create DTOs in `myquiz-api` Module
**Inputs:** Requirements for quiz filtering and pagination
**Outputs:**
- `QuizFilterInputDto` (fields: page, pageSize, course, quizId, authorId, etc.)
- `QuizFilterDto` (fields: quizzes, totalElements, totalPages, currentPage, pageSize)

## 2. Quiz Operations

| Section | Operation                | UI Template           | Thymeleaf Endpoint           | Backend Endpoint                | Service Action                    |
|---------|--------------------------|----------------------|------------------------------|----------------------------------|------------------------------------|
| 2.1.1   | List Quizzes             | quiz-list.html       | GET /quizzes                 | POST /api/quizzes/filter         | QuizService.filterQuizzes()        |
| 2.1.2   | Delete Quiz              | quiz-list.html       | POST /quizzes/{id}/delete    | DELETE /api/quizzes/{id}         | QuizService.deleteQuiz()           |
| 2.2.1   | View Quiz Details        | quiz-details.html    | GET /quizzes/{id}            | GET /api/quizzes/{id}            | QuizService.getQuizById()          |
| 2.2.2   | View Extended Details    | quiz-details.html    | GET /quizzes/{id}/details    | GET /api/quizzes/{id}/details    | QuizService.getQuizDetailsById()   |
| 2.3.1   | View Quiz Statistics     | quiz-statistics.html | GET /quizzes/{id}/statistics | GET /api/quizzes/{id}/statistics | QuizService.getQuizStatistics()    |
| 2.4     | Create Quiz              | quiz-create.html     | POST /quizzes                | POST /api/quizzes                | QuizService.createQuiz()           |
| 2.5     | Update Quiz              | quiz-edit.html       | POST /quizzes/{id}/edit      | PUT /api/quizzes/{id}            | QuizService.updateQuiz()           |

For each quiz operation, the flow cascades from UI template to Thymeleaf Controller, to backend endpoint, to service action. Each item below details steps, inputs, outputs, and possible errors.

### 2.1 Actions from quiz-list.html
#### 2.1.1 List Quizzes (with Filtering & Pagination)
- **Step 1: UI Template**
  - Template: `quiz-list.html`
  - Action: User selects filters/pagination and submits form
  - Input: filter fields (page, pageSize, course, quizId, authorId)
  - Output: Filtered/paginated quiz list
  - Errors: Invalid filter values, empty result
- **Step 2: Thymeleaf Controller Endpoint**
  - Endpoint: `GET /quizzes` (ThyQuizController)
  - Action: Receives filter params, calls backend via RestTemplate
  - Input: filter params
  - Output: QuizFilterDto
  - Errors: Backend error, invalid params
- **Step 3: Backend Endpoint**
  - Endpoint: `POST /api/quizzes/filter`
  - Action: Filters quizzes
  - Input: QuizFilterInputDto
  - Output: QuizFilterDto
  - Errors: Validation error, DB error
- **Step 4: Service Action**
  - Service: QuizService.filterQuizzes()
  - Input: QuizFilterInputDto
  - Output: QuizFilterDto
  - Errors: Data access error

#### 2.1.2 Delete Quiz

This operation performs a comprehensive cascade delete of a quiz and all its related data, including cleanup of orphaned authors.

- **Step 1: UI Template**
  - Template: `quiz-list.html` (delete button)
  - Action: User clicks delete button for a quiz
  - Input: quizId
  - Output: Quiz deleted confirmation
  - Errors: Quiz not found, deletion failed
  - **Guidelines:**
    - Show confirmation dialog before deletion
    - Display warning about cascade deletion of all related data
    - Redirect to quiz list after successful deletion
    - Show error message if deletion fails

- **Step 2: Thymeleaf Controller Endpoint**
  - Endpoint: `POST /quizzes/{id}/delete` (ThyQuizController)
  - Action: Calls backend to delete quiz
  - Input: quizId (Long)
  - Output: Redirect to quiz list with status message
  - Errors: Backend error, quiz not found
  - **Guidelines:**
    - Validate quizId is not null
    - Use RestTemplate to call DELETE endpoint
    - Handle exceptions gracefully
    - Add success/error message to redirect attributes
    - Log deletion attempts

- **Step 3: Backend Endpoint**
  - Endpoint: `DELETE /api/quizzes/{id}` (QuizController)
  - Action: Delegates to service to delete quiz with cascade
  - Input: quizId (Long, @PathVariable)
  - Output: ResponseEntity<Void> with 204 (No Content) on success, 404 on not found
  - Errors: 404 (quiz not found), 500 (internal server error)
  - **Guidelines:**
    - Document with Swagger/OpenAPI annotations
    - Return 204 No Content on successful deletion
    - Return 404 Not Found if quiz doesn't exist
    - Catch and log all exceptions
    - Use try-catch to handle service exceptions

- **Step 4: Service Action**
  - Service: QuizService.deleteQuizById(Long id)
  - Input: quizId (Long)
  - Output: void (throws exception on error)
  - Errors: EntityNotFoundException, DataAccessException
  - **Cascade Deletion Sequence:**
    1. **Find all QuizAuthor entries** for this quiz (with questions and errors loaded)
    2. **For each QuizAuthor:**
       - a. **Delete all Questions** associated with the QuizAuthor
         - Use QuestionRepository with QuestionSpecification to find questions
         - Call questionRepository.deleteAll(questions)
         - Log number of questions deleted per author
       - b. **Delete all QuizErrors** associated with the QuizAuthor
         - Use QuizErrorRepository.findByQuizAuthorId()
         - Call quizErrorRepository.deleteAll(errors)
         - Log number of errors deleted per author
    3. **Delete all QuizAuthor entries** for this quiz
       - Use QuizAuthorRepository.deleteAllByQuizId(id)
       - Log deletion of QuizAuthor entries
    4. **Delete the Quiz** itself
       - Use QuizRepository.deleteById(id)
       - Log quiz deletion
    5. **Clean up orphaned Authors** (authors with no remaining quiz contributions)
       - For each author from deleted QuizAuthor entries:
         - Check remaining contributions: quizAuthorRepository.countByAuthor(author)
         - If count == 0, delete the author: authorRepository.delete(author)
         - Log whether author was deleted or kept
  - **Guidelines:**
    - Use @Transactional annotation to ensure atomicity
    - Log each major step for debugging and audit trail
    - Handle each deletion step with proper error handling
    - Ensure all related data is deleted before deleting the quiz
    - Clean up orphaned authors to maintain database integrity
    - Use batch operations where possible for performance
  - **Transaction Management:**
    - All deletions must occur within a single transaction
    - If any step fails, entire transaction is rolled back
    - Ensures database consistency
  - **Performance Considerations:**
    - Eager load questions and errors with QuizAuthor to minimize queries
    - Use batch delete operations
    - Log execution time for large datasets
  - **Audit Trail:**
    - Log quiz ID being deleted
    - Log number of QuizAuthor entries found
    - Log number of questions deleted per author
    - Log number of errors deleted per author
    - Log authors deleted vs kept
    - Log completion of deletion process

### 2.2 Actions from quiz-details.html
#### 2.2.1 View Quiz Details
- **Step 1: UI Template**
  - Template: `quiz-details.html`
  - Action: User clicks on quiz to view details
  - Input: quizId
  - Output: Quiz details
  - Errors: Quiz not found
- **Step 2: Thymeleaf Controller Endpoint**
  - Endpoint: `GET /quizzes/{id}` (ThyQuizController)
  - Action: Calls backend for quiz details
  - Input: quizId
  - Output: QuizDto
  - Errors: Backend error, quiz not found
- **Step 3: Backend Endpoint**
  - Endpoint: `GET /api/quizzes/{id}`
  - Action: Fetches quiz details
  - Input: quizId
  - Output: QuizDto
  - Errors: Not found, DB error
- **Step 4: Service Action**
  - Service: QuizService.getQuizById()
  - Input: quizId
  - Output: QuizDto
  - Errors: Data access error

#### 2.2.2 View Extended Quiz Details
- **Step 1: UI Template**
  - Template: `quiz-details.html`
  - Action: User requests extended details
  - Input: quizId
  - Output: Extended quiz details
  - Errors: Quiz not found
- **Step 2: Thymeleaf Controller Endpoint**
  - Endpoint: `GET /quizzes/{id}/details` (ThyQuizController)
  - Action: Calls backend for extended details
  - Input: quizId
  - Output: QuizDetailsDto
  - Errors: Backend error, quiz not found
- **Step 3: Backend Endpoint**
  - Endpoint: `GET /api/quizzes/{id}/details`
  - Action: Fetches extended details
  - Input: quizId
  - Output: QuizDetailsDto
  - Errors: Not found, DB error
- **Step 4: Service Action**
  - Service: QuizService.getQuizDetailsById()
  - Input: quizId
  - Output: QuizDetailsDto
  - Errors: Data access error

### 2.3 Actions from quiz-statistics.html
#### 2.3.1 View Quiz Statistics
- **Step 1: UI Template**
  - Template: `quiz-statistics.html`
  - Action: User requests quiz statistics
  - Input: quizId
  - Output: Quiz statistics
  - Errors: Quiz not found, statistics unavailable
- **Step 2: Thymeleaf Controller Endpoint**
  - Endpoint: `GET /quizzes/{id}/statistics` (ThyQuizController)
  - Action: Calls backend for quiz statistics
  - Input: quizId
  - Output: QuizStatisticsDto
  - Errors: Backend error, statistics not found
- **Step 3: Backend Endpoint**
  - Endpoint: `GET /api/quizzes/{id}/statistics`
  - Action: Fetches quiz statistics
  - Input: quizId
  - Output: QuizStatisticsDto
  - Errors: Not found, DB error
- **Step 4: Service Action**
  - Service: QuizService.getQuizStatistics()
  - Input: quizId
  - Output: QuizStatisticsDto
  - Errors: Data access error

### 2.4 Create Quiz
- **Step 1: UI Template**
  - Template: `quiz-create.html`
  - Action: User submits new quiz form
  - Input: QuizDto fields
  - Output: Quiz created confirmation
  - Errors: Validation error
- **Step 2: Thymeleaf Controller Endpoint**
  - Endpoint: `POST /quizzes` (ThyQuizController)
  - Action: Calls backend to create quiz
  - Input: QuizDto
  - Output: Created QuizDto
  - Errors: Backend error, validation error
- **Step 3: Backend Endpoint**
  - Endpoint: `POST /api/quizzes`
  - Action: Creates quiz
  - Input: QuizDto
  - Output: QuizDto
  - Errors: Validation error, DB error
- **Step 4: Service Action**
  - Service: QuizService.createQuiz()
  - Input: QuizDto
  - Output: QuizDto
  - Errors: Data access error

### 2.5 Update Quiz
- **Step 1: UI Template**
  - Template: `quiz-edit.html`
  - Action: User submits edit form
  - Input: QuizDto fields
  - Output: Quiz updated confirmation
  - Errors: Validation error
- **Step 2: Thymeleaf Controller Endpoint**
  - Endpoint: `POST /quizzes/{id}/edit` (ThyQuizController)
  - Action: Calls backend to update quiz
  - Input: QuizDto
  - Output: Updated QuizDto
  - Errors: Backend error, validation error
- **Step 3: Backend Endpoint**
  - Endpoint: `PUT /api/quizzes/{id}`
  - Action: Updates quiz
  - Input: QuizDto
  - Output: QuizDto
  - Errors: Validation error, DB error
- **Step 4: Service Action**
  - Service: QuizService.updateQuiz()
  - Input: QuizDto
  - Output: QuizDto
  - Errors: Data access error

## 3. QuizAuthor Relationship Management

The QuizAuthor entity represents the many-to-many relationship between Quiz and Author, with additional context like source file and template type. This entity is central to quiz deletion and author lifecycle management.

### 3.1 QuizAuthor Entity Structure

**Relationships:**
- **ManyToOne** to Author (many QuizAuthors can reference one Author)
- **ManyToOne** to Quiz (many QuizAuthors can reference one Quiz)
- **OneToMany** to Question (one QuizAuthor has many Questions)
- **OneToMany** to QuizError (one QuizAuthor has many QuizErrors)

**Key Fields:**
- `id` (Long): Primary key
- `author` (Author): The author who contributed questions
- `quiz` (Quiz): The quiz this contribution belongs to
- `source` (String): Original source file path
- `templateType` (TemplateType): Template used for questions
- `questions` (Set<Question>): Questions contributed by this author to this quiz
- `quizErrors` (Set<QuizError>): Errors associated with this contribution

### 3.2 QuizAuthor Repository Methods

**Key Methods Used in Delete Operations:**
- `findWithQuestionsAndQuizErrorsByQuizId(Long quizId)`: Eager loads all QuizAuthor entries with their questions and errors for a quiz
- `deleteAllByQuizId(Long quizId)`: Deletes all QuizAuthor entries for a specific quiz
- `countByAuthor(Author author)`: Counts remaining quiz contributions for an author (used for orphan detection)

### 3.3 Orphaned Author Detection and Cleanup

**Definition:** An orphaned author is an author who has no remaining QuizAuthor entries (no quiz contributions).

**Detection Logic:**
```java
Long remainingContributions = quizAuthorRepository.countByAuthor(author);
if (remainingContributions == 0) {
    // Author is orphaned - safe to delete
}
```

**Cleanup Process:**
1. Collect all authors from QuizAuthor entries being deleted
2. After deleting QuizAuthor entries, check each author
3. If author has zero remaining contributions, delete the author
4. Log all deletion decisions for audit trail

**Benefits:**
- Maintains database integrity
- Prevents accumulation of unused author records
- Automatic cleanup reduces manual maintenance
- Preserves authors who contribute to other quizzes

## 4. Author Operations

### Create / Update
- Authors create and update quizzes through `quiz-create.html` and `quiz-edit.html`.
- Quiz creation associates quizzes with courses and authors; updates must preserve author attribution and related question links.

### View / List
- Authors can view and filter their quizzes via `quiz-list.html`, including pagination and filtering by course and author.
- Quiz details and statistics views (`quiz-details.html`, `quiz-statistics.html`) expose quiz performance and composition from the author’s perspective.

### Delete / Archive
- Authors can request quiz deletion from `quiz-list.html`; the system performs cascade deletion of related questions, quiz-author links, and errors according to business rules.
- Any future archive/soft-delete mechanism must maintain author history and avoid data loss for reporting.

### Permissions & Roles
- Only authenticated authors can manage their own quizzes.
- Administrators may manage quizzes for all authors, including deletion and statistics.

## 5. Ensure Consistency Across Calls
**Inputs:** All quiz list display calls
**Outputs:**
- All quiz list views use the same filtering and pagination logic
- Duplicate logic refactored to use DTOs and controller methods

## 6. Test and Validate the Flow
**Inputs:** UI actions, backend responses
**Outputs:**
- Pagination and filtering work as expected
- Backend returns correct paginated/filter data
- All quiz list views use unified flow
- Delete operations maintain referential integrity
- Orphaned authors are properly cleaned up

## 7. Quiz List UI Enhancements

### 7.1 Button Styling Improvements

Enhanced quiz-list.html with consistent button icons and styling:

**Added Icons:**
- 📝 Questions button
- 👁️ Details button
- ✏️ Edit button
- 🗑️ Delete button
- 📥 CSV MC button (green gradient)
- 📥 CSV TF button (green gradient)
- 👥 Authors button

**Button Classes:**
- `btn-edit` - Orange gradient for edit/view actions
- `btn-info` - Orange gradient for info actions
- `btn-delete` - Red gradient for delete actions
- `btn-new` - Green gradient for export/download actions

**CSS Standards:**
All buttons use gradient styling from `styles.css`:
```css
.btn-new {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: #ffffff;
    border: none;
    font-weight: 600;
}
```

### 7.2 Filter and Navigation

**Quiz List Filtering:**
- ✅ Course dropdown filter
- ✅ Page size selector (5, 10, 20, 50)
- ✅ Current page input
- ✅ Filter button with gradient styling

**Pagination Controls:**
- ⏮️ First page button
- ⬅️ Previous page button
- Page X of Y display
- ➡️ Next page button
- ⏭️ Last page button

**Disabled States:**
- First/Previous disabled on page 1
- Next/Last disabled on last page
- Visual feedback with reduced opacity

### 7.3 Quiz Details Page

Enhanced quiz-details.html with:
- Consistent button styling matching quiz-list
- All action buttons have appropriate icons
- Export buttons use green gradient (btn-new class)
- Navigation breadcrumbs
- Proper spacing and alignment

## 8. Related Documentation
- See `question-sd.md` for Question service details and QuestionSpecification usage
- See `author-sd.md` for Author operations and lifecycle management
- See `upload-sd.md` for how QuizAuthor entries are created during file upload
- See `style-sd.md` for UI/UX standards and CSS classes

---

**Note:** Follow project guidelines and best practices for DTOs, controller design, endpoint publishing, and template integration.
