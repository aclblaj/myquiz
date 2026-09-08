# Proposed Improvements — MyQuiz

**Author: Flavius-Stefan Ungureanu 08.09.2026**

> Companion document to the `feature/unit-tests-and-improvements` branch.
> It (1) summarizes the unit tests added to strengthen the automated test suite and
> (2) proposes **three concrete improvements** for the MyQuiz project, as requested.

---

## 📑 Table of Contents

- [Proposed Improvements — MyQuiz](#proposed-improvements--myquiz)
  - [📑 Table of Contents](#-table-of-contents)
  - [🎯 Context](#-context)
  - [🧪 Part 1 — Extended Unit Tests](#-part-1--extended-unit-tests)
  - [🚀 Part 2 — Three Proposed Improvements](#-part-2--three-proposed-improvements)
    - [Improvement 1 — Fill the missing academic year *(bug fix)*](#improvement-1--fill-the-missing-academic-year-bug-fix)
    - [Improvement 2 — Harden pagination input validation *(robustness)*](#improvement-2--harden-pagination-input-validation-robustness)
    - [Improvement 3 — Add a new question type *(feature)*](#improvement-3--add-a-new-question-type-feature)
  - [📊 Risk \& Validation Summary](#-risk--validation-summary)

---

## 🎯 Context

The task has three parts:

| # | Requirement | Status |
|---|-------------|--------|
| 1 | Extend the automated (unit) tests | ✅ Implemented — 5 new test classes |
| 2 | Propose 3 improvements (e.g. new question types) | 📝 Proposed below — each with a concrete change and a matching test plan |

Every proposal below is deliberately **additive and low-risk**, and each one is
**verifiable with a plain unit test** — no database, Docker, or running server required.
This keeps the change set safe to review and easy to validate in CI.

---

## 🧪 Part 1 — Extended Unit Tests

Five new **pure unit tests** were added (no Spring context, no PostgreSQL, no Docker),
so they run in milliseconds and are deterministic.

| Test class | Target under test | What it covers |
|------------|-------------------|----------------|
| `QuestionTypeTest` | `QuestionType` enum | `fromInteger` (valid / `null` / unmapped), `getValue`, `getAcronym`, `getTypeAsString`, `getTypeAsStringFromInteger`, `getAllTypesAsStringArray` |
| `StudyYearTest` | `StudyYear` enum | `getValue`, `fromValue` by display string, `fromValue` by name (case-insensitive), `null`/blank → `null`, invalid → exception |
| `LevenshteinQuestionSimilarityStrategyTest` | `LevenshteinQuestionSimilarityStrategy` | metadata, identical text, single-char difference above threshold, dissimilar text, `null`/blank guards |
| `JaroWinklerQuestionSimilarityStrategyTest` | `JaroWinklerQuestionSimilarityStrategy` | metadata, identical text, transposed near-match (`MARTHA`/`MARHTA`), unrelated text, `null`/blank guards |
| `MyUtilTest` | `MyUtil` | `isDuplicateValidationError` cases, `getPageable` (unpaged / unsorted / descending sort / invalid size) |

**Why these classes?** They hold real business logic (type resolution, text-similarity
scoring, pagination) yet have **no external dependencies**, making them the highest-value,
lowest-friction targets for reliable unit tests.

---

## 🚀 Part 2 — Three Proposed Improvements

### Improvement 1 — Fill the missing academic year *(bug fix)*

**Problem.** The `StudyYear` enum skips a year:

```java
Y2022_2023("2022-2023"),
// ⛔ 2023-2024 is missing
Y2024_2025("2024-2025"),
```

Any question bank belonging to the **2023–2024** academic year cannot be labeled or
filtered, because the value simply does not exist.

**Proposed change.** Add the missing constant in its correct chronological position:

```java
Y2022_2023("2022-2023"),
Y2023_2024("2023-2024"),
Y2024_2025("2024-2025"),
```

| Aspect | Detail |
|--------|--------|
| **Benefit** | Restores an unbroken academic-year sequence for labeling & filtering |
| **Risk** | 🟢 Minimal — the enum is a JSON value / dropdown / filter option; the change is purely additive |
| **Validation** | Extend `StudyYearTest` to assert `Y2023_2024` resolves both by value (`"2023-2024"`) and by name (`"y2023_2024"`) |

---

### Improvement 2 — Harden pagination input validation *(robustness)*

**Problem.** `MyUtil.getPageable(int pageNo, ...)` treats `-1` as the "unpaged" sentinel,
but **any other negative** page number silently flows through to
`PageRequest.of(pageNo - 1, ...)`, which throws a confusing low-level error such as
`Page index must not be less than zero`.

```java
if (pageNo == -1) {
    return Pageable.unpaged();
}
// pageNo == -2 → PageRequest.of(-3, ...) → cryptic exception
```

**Proposed change.** Validate the page number at the boundary and fail fast with a
clear, intentional message:

```java
if (pageNo == -1) {
    return Pageable.unpaged();
}
if (pageNo < 1) {
    throw new IllegalArgumentException("Page number must be >= 1 (or -1 for unpaged)");
}
```

| Aspect | Detail |
|--------|--------|
| **Benefit** | Clear, predictable error for invalid input; safer API boundary |
| **Risk** | 🟢 Minimal — only affects already-invalid input; every valid call behaves exactly as before |
| **Validation** | Extend `MyUtilTest` to assert `getPageable(-2, 10, null, null)` throws a clear `IllegalArgumentException` |

---

### Improvement 3 — Add a new question type *(feature)*

> This is the example the assignment explicitly suggested: *"adding other types of questions."*

**Problem.** `QuestionType` currently supports only `MULTICHOICE` and `TRUEFALSE`
(plus `UNKNOWN`). There is no way to represent an **open-ended / essay** question.

```java
public enum QuestionType {
    UNKNOWN(0, "UN"),
    MULTICHOICE(1, "MC"),
    TRUEFALSE(2, "TF");
}
```

**Proposed change (model/API level, intentionally scoped).**

1. Add the new value plus two convenience methods:

   ```java
   ESSAY(3, "ES");

   public static QuestionType fromAcronym(String acronym) { ... } // reverse of getAcronym()
   public boolean isKnown() { return this != UNKNOWN; }
   ```

2. Add an explicit `ESSAY` branch in `QuestionController#buildSampleQuestion(...)` so the
   sample renders correctly instead of falling through to the true/false branch.

**Why this is safe:**

- The duplicate-analysis `switch` already has a graceful `default ->` branch, so an
  unmapped type is skipped, not crashed.
- `getSampleQuestion` already wraps `QuestionType.valueOf(...)` in a `try/catch`.
- The change is **intentionally scoped to the model/API layer**. The Excel import / Moodle
  export pipeline is left untouched, keeping the proposal small, focused, and easy to review.

| Aspect | Detail |
|--------|--------|
| **Benefit** | Extensible type model; groundwork for open-ended questions; richer type API (`fromAcronym`, `isKnown`) |
| **Risk** | 🟡 Low — model/API only; graceful defaults already exist in the hot paths |
| **Validation** | Extend `QuestionTypeTest` for the new value, its acronym, `fromAcronym`, and `isKnown` |

---

## 📊 Risk & Validation Summary

| Improvement | Type | Files to change | Risk | Proposed validating test |
|-------------|------|-----------------|------|--------------------------|
| 1 — Missing academic year | Bug fix | `StudyYear` | 🟢 Minimal | `StudyYearTest` |
| 2 — Pagination validation | Robustness | `MyUtil` | 🟢 Minimal | `MyUtilTest` |
| 3 — New question type | Feature | `QuestionType`, `QuestionController` | 🟡 Low | `QuestionTypeTest` |

All three are additive, isolated, and provable with fast unit tests — a safe, focused way
to contribute value while keeping each change easy to review and verify in CI.

---