# UI Style Software Design
## 1. Overview
This document defines the active UI design-system conventions for MyQuiz.
It focuses on reusable layout, table, form, button, navigation, and state classes centralized in the shared stylesheet.
## 2. Functional Scope
The style system supports all server-rendered templates in `myquiz-thymeleaf` and is intentionally cross-feature rather than feature-specific.
## 3. Architecture
### 3.1 Styling Ownership
- canonical stylesheet: `myquiz-thymeleaf/src/main/resources/static/css/styles.css`
- templates are expected to reuse shared classes rather than define page-specific inline styling
### 3.2 Design-System Rule
Shared class names should describe reusable UI patterns, not one-off pages.
## 4. Design Tokens and Visual Patterns
### 4.1 Main Themes
- primary application styling is blue-oriented for general pages
- authentication screens keep a distinct login/register visual treatment
- action colors communicate intent for create, edit, export, and destructive operations
### 4.2 Reusable UI Building Blocks
Common patterns include:
- `.container`
- `.card`, `.card-header`, `.card-body`
- `.styled-table`
- `.form-container`, `.form-actions`, `.form-grid-*`
- semantic button classes
- menu classes such as `.menu`, `.main-menu-item`, `.main-submenu`
## 5. Flows and Usage Patterns
### 5.1 Template Composition Pattern
1. Template includes shared stylesheet.
2. Page layout is built from shared containers/cards.
3. Forms and tables use standard classes.
4. Buttons express intent through shared semantic button classes.
### 5.2 Responsive Pattern
Responsive rules adapt:
- form grids
- action-cell button layout
- navigation/menu presentation
- compact table and action arrangements on smaller widths
## 6. Responsibilities
### 6.1 Templates
- use shared classes instead of copying styling logic inline
- prefer composition from existing layout primitives
### 6.2 Stylesheet
- centralize visual decisions
- keep classes generic and reusable
- support both page-level layout and component-level styling
## 7. Validation and Error Handling
UI-state classes should make user feedback explicit and visually distinct, including:
- error messages
- success messages
- disabled controls
- contextual resolved/selected states where applicable
## 8. Key Decisions
- keep styling centralized in one shared stylesheet
- keep class names generic and cross-feature
- keep destructive, export, and edit actions visually distinct
- keep responsive behavior in CSS rather than template-specific workarounds where possible
## 9. Implementation Notes
- Shared stylesheet:
  - `myquiz-thymeleaf/src/main/resources/static/css/styles.css`
- Representative templates:
  - `myquiz-thymeleaf/src/main/resources/templates/question-bank-list.html`
  - `myquiz-thymeleaf/src/main/resources/templates/question-list.html`
  - `myquiz-thymeleaf/src/main/resources/templates/admin/data-management.html`
  - `myquiz-thymeleaf/src/main/resources/templates/fragments/navigation.html`
Related docs:
- `prompt/core-sd.md`
- `prompt/admin-interface-sd.md`
