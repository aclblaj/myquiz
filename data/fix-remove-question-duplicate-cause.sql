-- Removes obsolete cause column from question_duplicate.
-- Safe for repeated execution.
ALTER TABLE IF EXISTS question_duplicate
    DROP COLUMN IF EXISTS cause;

