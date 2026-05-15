# Markdown Consolidation Guide

This guide defines how MyQuiz documentation is consolidated so that the active documents stay aligned with the current codebase and historical markdown files are archived instead of competing with the canonical sources.

## Primary Objective

The main purpose of consolidation is to keep the documentation in sync with the code.

That means:

- active docs must describe the implementation that actually exists in the repository
- overlapping documents must be merged into a single source of truth
- historical reports, fix notes, and delivery summaries must be moved out of the root once their useful content is absorbed
- the roles of `README.md`, `guidelines.md`, and `prompt/*-sd.md` must stay clearly separated

## Canonical Documentation Set

After consolidation, the active documentation should be centered on these files:

### `README.md`

Purpose:

- explain the project scope
- list the minimal components of the ecosystem
- provide the shortest reliable setup and run steps
- help a new developer understand what MyQuiz is and how to start it

`README.md` should not become a long historical log or a dumping ground for every implementation detail.

### `guidelines.md`

Purpose:

- define the general rules for implementing and maintaining the ecosystem
- capture cross-cutting conventions used across modules
- document architectural guardrails, coding rules, integration rules, and shared practices

`guidelines.md` is the ecosystem rulebook, not a feature-specific design document.

### `prompt/*-sd.md`

Purpose:

- document architecture and software design for major functionalities
- capture feature boundaries, responsibilities, flows, decisions, and implementation constraints
- keep a similar structure across all feature design files for consistency

These are the main functional design documents and should remain the canonical place for feature-oriented architecture.

## Consolidation Scope

The main consolidation scope is:

1. consolidate root-level introductory and explanatory markdown content into `README.md`
2. consolidate ecosystem-wide implementation rules into `guidelines.md`
3. consolidate feature-specific architecture/design content into the relevant `prompt/*-sd.md` files
4. move non-canonical root markdown documents to archive once their useful content has been absorbed

In practice, this means the project should converge toward:

- `README.md` for project overview and run instructions
- `guidelines.md` for general implementation rules
- `prompt/*-sd.md` for major functionality design
- `prompt/archive/` for superseded, historical, temporary, delivery, repair, and status documents

- critical operational and implementation content should be absorbed into canonical documents
- processed root-level reports should move to `prompt/archive/`
- archive material may remain available for reference, but it must stop competing with active documentation

## What Belongs Where

### Put content in `README.md` when it is about:

- what the project does
- which services/modules exist
- minimum prerequisites
- minimal startup flow
- key repository structure at a high level
- links to deeper documentation

### Put content in `guidelines.md` when it is about:

- coding and architectural rules
- ecosystem-wide communication patterns
- shared implementation constraints
- naming, layering, DTO usage, security, persistence, testing, or integration conventions
- rules that apply across multiple modules or features

### Put content in `prompt/*-sd.md` when it is about:

- one major functional area
- architecture for that functional area
- responsibilities of controllers/services/repositories/templates in that area
- data flow, integration flow, validation flow, or permissions for that area
- design decisions tied to that functionality

### Move content to archive when it is:

- a completed fix report
- a one-time migration note
- a delivery summary
- a duplicate explanation of content now covered elsewhere
- a temporary implementation note that is no longer canonical

## Root Folder Archiving Policy

After consolidation, root-level markdown files should be kept minimal and intentional.

### Keep active in the root

- `README.md`
- `guidelines.md`
- any truly top-level project files that are still needed as active entry points

### Move to `prompt/archive/`

- completed reports
- implementation summaries
- fix notes
- review notes
- historical deliverables
- temporary analysis files
- redundant root markdown files whose content has already been merged into canonical docs

Rule: if a root markdown file does not define project entry information, ecosystem-wide rules, or an active top-level reference purpose, it should usually be archived after consolidation.

- keep the set of canonical documents small and clear
- centralize key information by document purpose
- preserve historical materials in archive instead of deleting them
- do not maintain discoverability through links to the archived documents

## Standard Consolidation Workflow

1. Inventory markdown files
   - List root `.md` files and all `prompt/*-sd.md` documents.

2. Classify each document
   - Decide whether the file is canonical, mergeable, or archive-only.

3. Extract unique content
   - Keep only information that adds value beyond what is already in `README.md`, `guidelines.md`, or a relevant `*-sd.md` file.

4. Merge into canonical targets
   - Merge project overview/startup information into `README.md`.
   - Merge ecosystem rules into `guidelines.md`.
   - Merge feature architecture into the relevant `prompt/*-sd.md` document.
   - Prefer merging reusable final-state knowledge, not status prose like "completed", "fixed", or "delivered".

5. Normalize structure
   - Make sure the destination document uses the correct scope and section style.
   - Remove completion-report wording, milestone language, and one-time execution framing.

6. Verify against code
   - Check module names, ports, paths, commands, flows, and ownership boundaries against the actual repository.

7. Remove time-bound wording
   - Rewrite dated or time-sensitive statements into stable documentation language.
   - Prefer neutral wording such as "current implementation" instead of calendar-based phrasing.
   - Use git history for chronology instead of embedding new dates in active markdown.

8. Archive the source files
   - Move obsolete root docs to `prompt/archive/` only after their useful content has been captured.

9. Leave references behind only when needed
   - If a file is archived, the canonical doc may not keep nor a brief historical reference, nor duplicate the archived content.

### Preserve these patterns

- keep `README.md` as the entry point for project overview and minimal run guidance
- keep `guidelines.md` as the cross-cutting implementation rulebook
- keep `prompt/*-sd.md` as the authoritative design layer for major features
- move processed reports, summaries, and refactoring notes to `prompt/archive/`

### Do not repeat these anti-patterns in new active docs

- do not copy archive summaries into active docs as historical narratives
- do not add "what was done" or "consolidation complete" sections to canonical docs
- do not preserve counts of moved files, dated milestones, or completion badges in active docs unless they are strictly needed in archive material

### Preferred migration style

- extract the lasting rule, pattern, architecture note, or operational instruction
- rewrite it in neutral, present-tense documentation
- place it in the single correct canonical destination
- archive the source report after the knowledge has been absorbed

## Conflict Resolution Rules

When two documents disagree, resolve conflicts in this order:

1. Code and current project structure win
   - Source code, actual modules, active configuration, and real paths take precedence over prose.

2. Canonical destination wins by scope
   - `README.md` wins for overview/run content.
   - `guidelines.md` wins for general implementation rules.
   - `prompt/*-sd.md` wins for feature-specific design.

3. Newer active documentation wins over older reports
   - Prefer active docs over archived summaries, status files, or implementation reports.

4. Specific evidence wins over vague wording
   - Prefer content backed by code references, endpoint paths, package names, module names, or command examples.

5. Archive unresolved historical nuance
   - If a detail is historical and not needed for current understanding, keep it in archive rather than polluting active docs.

6. Prefer timeless wording over dated consolidation notes
   - If a statement is only true because of a dated event or milestone, rewrite it as a stable rule or leave it in archive.

## Standard Structure for `*-sd.md` Files

All major functionality design files should use a similar structure so they are easier to navigate and maintain.

Recommended structure:

1. `Overview`
   - feature purpose and scope
2. `Business Context` or `Functional Scope`
   - what problem the feature solves
3. `Architecture`
   - involved modules and responsibilities
4. `Data Model / DTOs / Entities`
   - relevant structures and contracts
5. `Flows`
   - request flow, UI flow, validation flow, or integration flow
6. `Permissions and Security`
   - access rules and enforcement points
7. `UI / API / Service Responsibilities`
   - what belongs in controllers, services, repositories, templates, etc.
8. `Validation / Error Handling`
   - important validation rules and failure modes
9. `Key Decisions`
   - major architectural choices and rationale
10. `Implementation Notes`
   - practical constraints and important repository references

The exact headings may vary by feature, but the structure should remain recognizably similar across all `*-sd.md` files.

## Naming and Versioning

Use working files in `prompt/util/` while consolidating, then merge into canonical targets.

Recommended working names:

- `TOPIC-CONSOLIDATED-vX.Y.md`

Rules:

- use `v1.0` for the first clean consolidation draft
- bump minor versions for clarifications and additional merged content
- bump major versions for substantial restructuring
- avoid names like `final.md`, `final2.md`, or `new-final.md`
- do not add calendar dates to consolidation filenames created during the run

## No-Dates Rule for Consolidation Runs

During a consolidation run, do not add new dates into active markdown files.

This restriction applies to:

- document headers such as `Date:`, `Last Updated:`, or `Updated on`
- section titles containing dates
- newly created consolidation filenames with embedded dates
- dated milestone bullets added to active canonical documentation

Allowed handling:

- preserve dates only inside already archived historical documents when they are part of the original record
- remove or rewrite dated content when merging it into `README.md`, `guidelines.md`, or `prompt/*-sd.md`
- use git history, commit messages, tags, or archive location for chronology instead of adding dates into active docs

## Quality Checklist

- [ ] `README.md` only contains scope, minimal components, startup guidance, and links outward.
- [ ] `guidelines.md` contains ecosystem-wide rules rather than feature-specific duplication.
- [ ] each `prompt/*-sd.md` focuses on one major functionality.
- [ ] `*-sd.md` files follow a similar structural pattern.
- [ ] active docs match the real modules, paths, ports, and flows in the repository.
- [ ] duplicated historical content has been removed from active documents.
- [ ] obsolete root markdown files have been moved to `prompt/archive/`.
- [ ] archived files are no longer treated as canonical sources.
- [ ] no new dates were added to active markdown files during the consolidation run.
- [ ] merged content was rewritten from report-style language into timeless documentation language.

## Example Workflow (Windows PowerShell)

```powershell
$Root = "C:\work\cla22\myquiz"
Set-Location $Root

# 1) Review root markdown files
Get-ChildItem -Path $Root -Filter "*.md" | Select-Object Name

# 2) Review active software design files
Get-ChildItem -Path "$Root\prompt" -Filter "*-sd.md" | Select-Object Name

# 3) Create a working consolidation draft
$Out = "$Root\prompt\util\ROOT-DOCS-CONSOLIDATED-v1.0.md"
"# Root Documentation Consolidation Draft" | Set-Content -Path $Out -Encoding UTF8

# 4) Identify likely archive candidates in the root
Get-ChildItem -Path $Root -Filter "*.md" |
    Where-Object { $_.Name -notin @('README.md', 'guidelines.md') } |
    Select-Object Name

# 5) Check active docs for leftover placeholders
Select-String -Path "$Root\README.md", "$Root\guidelines.md", "$Root\prompt\*-sd.md" -Pattern "TODO|TBD|FIXME"

# 6) Check active docs for newly added date labels that should not appear after consolidation
Select-String -Path "$Root\README.md", "$Root\guidelines.md", "$Root\prompt\*-sd.md" -Pattern "Date:|Last Updated|Updated on|20[0-9][0-9]|19[0-9][0-9]"
```

## Source Mapping Template

Use this mapping during consolidation:

- Source file: `path/to/file.md`
- Destination: `README.md` | `guidelines.md` | `prompt/<feature>-sd.md` | `prompt/archive/`
- Reason: project overview | ecosystem rule | feature design | historical reference

## Notes

- Archive after consolidation, not before.
- Avoid copying the same content into `README.md`, `guidelines.md`, and a `*-sd.md` file.
- Prefer linking from one canonical document to another instead of duplicating sections.
- If a historical file still contains useful context but is not part of the active design, keep it in `prompt/archive/`.
- When using archived consolidation reports as inputs, extract reusable rules and outcomes rather than copying dated status text.

