# Question Duplication Detection and Validation

## 1. Overview

This document defines how duplicate detection works in MyQuiz for questions inside the same course.

The current behavior compares uploaded or edited questions against existing questions already present in that course and records duplicate links and duplicate-related errors.

## 2. Functional Scope

- Duplicate checks run for questions that belong to a single course.
- The comparison target is always the course dataset, not global data across all courses.
- The service supports both MULTICHOICE and TRUEFALSE questions.
- Duplicate recomputation clears previous duplicate links and duplicate-related errors before rebuilding them.

## 3. Matching Rules

### 3.1 Core Rule

For every current question, candidate questions are selected from the same course and same question type.

### 3.2 Self-Exclusion Rule

- A question is never compared with itself.
- The same question identifier cannot be used as both source and candidate.

### 3.3 Title Rule

A title is treated as duplicate when at least one of these conditions is true:

- current title contains candidate title
- candidate title contains current title
- configured similarity metric classifies both titles as similar

### 3.4 Answer Rule

For multichoice answers, each normalized answer from the current question is compared against candidate answers using:

- exact match
- bidirectional substring inclusion (A contains B or B contains A)
- configured similarity metric

### 3.5 Text Rule

For true/false questions, text content follows the same semantics as title comparison:

- bidirectional substring inclusion
- configurable similarity metric

### 3.6 Missing Answer Rule

- Multichoice requires all answer slots.
- True/false requires a valid response value.
- Missing required answer data creates missing-answer errors and skips duplicate matching for that question.

## 4. Similarity Strategy Architecture

### 4.1 Base Class

Duplicate matching uses one shared base strategy class:

- `AbstractQuestionSimilarityStrategy`

This base class centralizes:

- algorithm naming
- threshold handling
- generic `isSimilar` decision

### 4.2 Implementations

Two implementations are available:

- `LevenshteinQuestionSimilarityStrategy`
- `JaroWinklerQuestionSimilarityStrategy`

### 4.3 Selection

- The default algorithm is configurable with `myquiz.duplicates.similarity.algorithm`.
- Recompute can explicitly request `levenshtein` or `jaro-winkler`.
- If an unknown algorithm name is requested, the service falls back to `levenshtein`.

## 5. Service Responsibilities

Primary service:

- `QuestionDuplicationService`

Main responsibilities:

- normalize text fields used for matching
- compare current questions against same-course candidates
- avoid self-comparison and duplicate pair reinsertion
- persist duplicate links
- create duplicate error records when matching rules are met
- clear previous duplicate state during full recompute

## 6. Data and Persistence

### 6.1 Duplicate Links

- Stored in `QuestionDuplicate`.
- Links are canonicalized as lower-id to higher-id pairs to prevent mirrored duplicates.

### 6.2 Error Records

- Stored in `QuestionError`.
- Duplicate prefixes are reused for title and answer duplicate messaging.

### 6.3 Recompute Cleanup

Before full recomputation for a course:

- remove duplicate links for course question ids
- remove duplicate-related question errors for course question ids
- run a full duplicate rebuild by question type

## 7. Integration Flows

### 7.1 Upload Integration

After upload parsing, duplicate detection runs for the affected course and question bank.

### 7.2 Course Recompute Integration

Course-level duplicate recomputation is used for maintenance and consistency checks. It can execute with the default similarity algorithm or an explicitly requested algorithm.

## 8. Validation and Error Handling

- Empty author list or blank course input results in safe no-op behavior and warning logs.
- Service failures do not leave partially recomputed duplicate links when transaction boundaries are respected.
- Duplicate pair persistence ignores already existing links.

## 9. Performance Notes

- Candidate normalization is cached lazily per recompute run.
- Answer matching uses a fast exact-intersection path before similarity checks.
- Bidirectional substring checks are retained because they are required by business rules.
- Similarity metric checks are applied after fast paths to reduce overhead.

## 10. Test Coverage Expectations

The duplicate module should be covered with:

- unit tests for matching behavior and self-exclusion
- recompute tests that validate cleanup + rebuild behavior
- integration test using existing database data to compare Levenshtein and Jaro-Winkler outcomes for one course (example `BD`)

## 11. Author Operations

### Create / Update

Authors create or update questions through upload and editor flows. Duplicate checks run against existing course questions.

### View / List

Authors and admins can inspect duplicate-related errors and duplicate links in question detail/error flows.

### Delete / Archive

When duplicates are removed or recomputed, obsolete duplicate links and duplicate-related errors are cleaned automatically.

### Permissions and Roles

Duplicate management follows course and question management permissions from the auth and core security design.

## 12. Related Documentation

- `prompt/upload-sd.md`
- `prompt/question-sd.md`
- `prompt/author-error-sd.md`
- `prompt/data-cleanup-sd.md`



