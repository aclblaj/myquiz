# Style Software Design

## 1. Overview

This document describes the standardized UI/UX styling system for the MyQuiz application. All templates use consistent CSS classes, colors, and layouts defined in `styles.css`.

## 2. Core Design Principles

### 2.1 Separation of Concerns
- **All styles in styles.css** - No inline styles in templates (except rare exceptions)
- **General CSS class names** - Not template-specific (e.g., `.card`, not `.author-card`)
- **Reusable components** - Same classes across all templates

### 2.2 Color Themes

#### Blue Theme (General Application)
- **Primary:** Blue gradient (`#3b82f6` to `#2563eb`)
- **Background:** Light blue gradient (`#eff6ff` to `#dbeafe`)
- **Borders:** `#3b82f6` (2px solid)
- **Focus:** Blue shadow with `#3b82f6` border
- **Table:** `#1e3a8a` (header), `#3b82f6` (hover), `#eff6ff` (even rows), `#dbeafe` (odd rows)

#### Peach/Orange Theme (Authentication)
- **Primary:** Orange/peach tones
- **Background:** `#fff8f0`
- **Borders:** `#ffd6a5`
- **Form Inputs:** `#fffdf8` (background), orange borders
- **Used Only:** login.html, register.html

## 3. Standard Container Structure

### 3.1 Main Container
**All templates use:**
```html
<div class="container mt-3">
    <!-- Content here -->
</div>
```

**Exceptions:** login.html and register.html use `.login-container` and `.register-container`

### 3.2 Page Title
```html
<h2 class="card-header">Page Title</h2>
```

### 3.3 Card Sections
```html
<div class="card mt-4">
    <div class="card-header">Section Title</div>
    <div class="card-body">
        <!-- Section content -->
    </div>
</div>
```

## 4. Table Formatting

### 4.1 Standard Table
**Class:** `.styled-table`

**Properties:**
- Width: 100% (occupies full container)
- Border collapse: collapse
- Blue theme
- Responsive

**Structure:**
```html
<table class="styled-table">
    <thead>
        <tr>
            <th>Column 1</th>
            <th>Column 2</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Data 1</td>
            <td>Data 2</td>
        </tr>
    </tbody>
</table>
```

**Styling:**
- **Header:** Dark blue background (`#1e3a8a`), white text, bold
- **Even rows:** Light blue (`#eff6ff`)
- **Odd rows:** Lighter blue (`#dbeafe`)
- **Hover:** Blue highlight (`#3b82f6`, white text)
- **Borders:** 1px solid `#ddd`
- **Padding:** 12px

### 4.2 Detail Tables
**Use For:** author-details, quiz-details, course-details, question-view

**Alignment:**
- **Left column (labels):** Right-aligned, bold, background `#f0f9ff`
- **Right column (values):** Left-aligned

**Structure:**
```html
<table class="styled-table">
    <tbody>
        <tr>
            <td style="text-align: right; font-weight: bold;">Label:</td>
            <td style="text-align: left;">Value</td>
        </tr>
    </tbody>
</table>
```

## 5. Form Formatting

### 5.1 Form Container
**Class:** `.form-container`

**Properties:**
- Background: Blue gradient
- Border: 2px solid `#3b82f6`
- Border radius: 8px
- Padding: 30px
- Shadow: 0 4px 6px rgba(0,0,0,0.1)
- Max width: 900px (centered)

### 5.2 Form Layouts

#### Vertical Layout (Single Column)
**Class:** `.form-grid`
```html
<form class="form-container">
    <div class="form-grid">
        <div class="form-group">
            <label class="form-label">Field:</label>
            <input type="text" class="form-input" />
        </div>
    </div>
</form>
```

#### Two-Column Layout
**Class:** `.form-grid-2col`
```html
<div class="form-grid-2col">
    <div class="form-group">
        <label class="form-label">Field 1:</label>
        <input type="text" class="form-input" />
    </div>
    <div class="form-group">
        <label class="form-label">Field 2:</label>
        <input type="text" class="form-input" />
    </div>
</div>
```

### 5.3 Form Components

#### Input Fields
**Class:** `.form-input`
- Background: White
- Border: 1px solid `#93c5fd`
- Padding: 10px
- Border radius: 4px
- Focus: Blue shadow, `#3b82f6` border

#### Textareas
**Class:** `.form-textarea`
- Same as `.form-input`
- Min height: 100px
- Resize: vertical

#### Select Dropdowns
**Class:** `.form-select`
- Same as `.form-input`

#### Labels
**Class:** `.form-label`
- Font weight: bold
- Color: `#1e3a8a`
- Margin bottom: 5px

#### Fieldsets
**Class:** `.form-fieldset`
- Border: 2px solid `#3b82f6`
- Border radius: 6px
- Padding: 20px
- Margin top: 15px

**Legend:**
**Class:** `.form-legend`
- Font weight: bold
- Color: `#1e3a8a`
- Background: White
- Padding: 0 10px

### 5.4 Form Actions
**Class:** `.form-actions`

**Structure:**
```html
<div class="form-actions">
    <button type="submit" class="btn-save">Save</button>
    <a href="..." class="btn-cancel">Cancel</a>
</div>
```

**Properties:**
- Display: flex
- Justify content: flex-end
- Gap: 10px
- Margin top: 20px

### 5.5 Special Form Layouts

#### Answer/Weight Rows (Question Editor)
**Class:** `.answer-weight-row`
```html
<div class="answer-weight-row">
    <input type="text" placeholder="Answer" />
    <input type="number" placeholder="Weight" />
</div>
```

## 6. Button Standards

### 6.1 Button Color by Action

| Action | Class | Color | Gradient | Usage |
|--------|-------|-------|----------|-------|
| Save/Submit | `.btn-save` | Blue | `#3b82f6` to `#2563eb` | Primary actions (save, update, create) |
| Cancel/Back | `.btn-cancel` | Gray | `#6b7280` to `#4b5563` | Cancel, back navigation |
| New/Add | `.btn-new` | Green | `#10b981` to `#059669` | Create new item |
| Delete | `.btn-delete` | Red | `#ef4444` to `#dc2626` | Delete operations |
| Edit | `.btn-edit` | Orange | `#f97316` to `#ea580c` | Edit operations |
| Info/Details | `.btn-info` | Orange | `#f97316` to `#ea580c` | View details |
| Filter/Search | `.btn-filter` | Green | `#10b981` to `#059669` | Filter, search actions |

### 6.2 Button Base Properties
- Padding: 10px 20px
- Border radius: 6px
- Font weight: bold
- Color: White
- Border: none
- Cursor: pointer
- Transition: all 0.3s ease
- Text decoration: none

### 6.3 Button Hover Effect
- Transform: translateY(-2px)
- Box shadow: 0 4px 8px rgba(0,0,0,0.2)

### 6.4 Icon Buttons
Use emoji icons for visual appeal:
- 📤 Upload
- 📊 Statistics
- ✏️ Edit
- 🗑️ Delete
- 👁️ View
- ✅ Save
- ❌ Cancel

## 7. Alert and Message Formatting

### 7.1 Alert Classes
**Class:** `.alert`

**Types:**
- `.alert-success` - Green background, success messages
- `.alert-error` - Red background, error messages
- `.alert-info` - Blue background, information messages
- `.alert-warning` - Yellow background, warning messages

**Structure:**
```html
<div class="alert alert-success">
    Operation successful!
</div>
```

**Properties:**
- Padding: 15px
- Border radius: 6px
- Margin bottom: 20px
- Border left: 4px solid (darker shade)

### 7.2 Empty State Messages
```html
<div class="alert alert-info">
    No data found.
</div>
```

## 8. Badge and Status Indicators

### 8.1 Badge Classes

| Class | Color | Usage |
|-------|-------|-------|
| `.badge .badge-error` | Red | Error/open states |
| `.badge .badge-success` | Green | Success/resolved states |
| `.badge .badge-warn` | Yellow | Warning states |
| `.badge .badge-info` | Blue | Information states |

**Structure:**
```html
<span class="badge badge-error">Open</span>
<span class="badge badge-success">Resolved</span>
```

**Properties:**
- Display: inline-block
- Padding: 5px 10px
- Border radius: 12px
- Font size: 12px
- Font weight: bold

## 9. Filter Section Formatting

### 9.1 Filter Container
**Class:** `.searchfilter`

**Structure:**
```html
<div class="searchfilter">
    <h3>Filter Options</h3>
    <form method="get">
        <div class="form-grid-2col">
            <!-- Filter fields -->
        </div>
        <div class="form-actions">
            <button type="submit" class="btn-filter">🔍 Filter</button>
            <a href="..." class="btn-cancel">Clear</a>
        </div>
    </form>
</div>
```

**Properties:**
- Background: `#f0f9ff`
- Border: 1px solid `#3b82f6`
- Border radius: 8px
- Padding: 20px
- Margin bottom: 20px

## 10. Pagination Controls

### 10.1 Pagination Structure
```html
<div class="pagination">
    <a href="..." class="page-link">First</a>
    <a href="..." class="page-link">Previous</a>
    <span class="page-info">Page 2 of 5</span>
    <a href="..." class="page-link">Next</a>
    <a href="..." class="page-link">Last</a>
</div>
```

**Properties:**
- Display: flex
- Justify content: center
- Align items: center
- Gap: 10px
- Margin top: 20px

### 10.2 Page Link
**Class:** `.page-link`
- Padding: 8px 16px
- Background: `#3b82f6`
- Color: White
- Border radius: 4px
- Text decoration: none

### 10.3 Disabled Links
Use conditional styling:
```html
<a th:classappend="${currentPage == 0} ? 'disabled' : ''" 
   class="page-link">Previous</a>
```

**Disabled style:**
- Background: `#d1d5db`
- Cursor: not-allowed
- Pointer events: none

## 11. Template-Specific Rules

### 11.1 Login and Register Forms
**Classes:** `.login-container`, `.register-container`

**Theme:** Peach/orange (separate from general blue theme)

**Do NOT change:** These forms maintain their unique peach styling

### 11.2 Detail Views
Templates: author-details.html, quiz-details.html, course-details.html, question-view.html

**Use:**
- `.container mt-3` wrapper
- `.card` for sections
- `.styled-table` for information display
- Left-align content, right-align labels

### 11.3 List Views
Templates: author-list.html, quiz-list.html, question-list.html, error-list.html

**Use:**
- `.searchfilter` for filter section
- `.styled-table` for results
- `.pagination` for navigation

### 11.4 Editor Forms
Templates: question-editor-mc.html, question-editor-tf.html, quiz-editor.html

**Use:**
- `.form-container`
- `.form-fieldset` for sections
- `.answer-weight-row` for answer/weight pairs
- `.btn-save` for submit

### 11.5 Upload Forms
Templates: upload-excel.html, upload-archive.html

**Use:**
- `.form-container`
- `.form-grid` or `.form-grid-2col`
- File input with accept attribute

## 12. CSS Class Naming Conventions

### 12.1 Rules
1. **Use general names** - Not template-specific
2. **Lowercase with hyphens** - e.g., `.form-container`, not `.formContainer`
3. **Descriptive** - `.btn-save`, not `.btn-blue`
4. **Reusable** - Same class across templates
5. **No inline styles** - All in styles.css

### 12.2 Forbidden Practices
❌ Template-specific classes (e.g., `.author-header`, `.quiz-card`)
❌ Inline styles in templates (except rare cases)
❌ Color names in class names (e.g., `.blue-button`)
❌ Action-specific containers (e.g., `.edit-form-container`)
❌ Duplicate styles across templates

### 12.3 When to Create New CSS
✅ New reusable component needed across multiple templates
✅ New general pattern emerges
✅ Improving accessibility or responsiveness

❌ One-off styling for single element
❌ Template-specific customization
❌ Overriding existing working styles

## 13. Responsive Design

### 13.1 Breakpoints
- **Mobile:** < 768px
- **Tablet:** 768px - 1024px
- **Desktop:** > 1024px

### 13.2 Mobile Adjustments
```css
@media (max-width: 768px) {
    .form-grid-2col {
        grid-template-columns: 1fr; /* Stack on mobile */
    }
    
    .styled-table {
        font-size: 14px; /* Smaller text */
    }
    
    .form-actions {
        flex-direction: column; /* Stack buttons */
    }
}
```

## 14. Accessibility Requirements

### 14.1 Color Contrast
- Text on background: minimum 4.5:1 ratio
- Large text (18pt+): minimum 3:1 ratio
- All buttons and links meet contrast requirements

### 14.2 Focus Indicators
- All interactive elements have visible focus states
- Blue outline on tab navigation
- Enhanced for keyboard users

### 14.3 Labels and Aria
- All form inputs have associated labels
- Use `aria-label` for icon-only buttons
- Semantic HTML elements (nav, main, section)

## 15. Implementation Checklist

When creating or updating a template:

- [ ] Uses `.container mt-3` wrapper (except login/register)
- [ ] All tables use `.styled-table` class
- [ ] Forms use `.form-container` and grid layouts
- [ ] Buttons use correct color by action
- [ ] No inline styles (except rare exceptions)
- [ ] No template-specific CSS classes
- [ ] All new styles added to styles.css
- [ ] Responsive design considered
- [ ] Accessibility requirements met
- [ ] Tested on mobile, tablet, desktop
- [ ] Consistent with existing templates

## 16. Style Migration Summary

### 16.1 Standardization Changes

**What Changed:**
- Consolidated all styles into styles.css
- Removed template-specific CSS classes
- Standardized button colors by action
- Made tables occupy full container width (100%)
- Applied blue theme consistently
- Created reusable form layouts

**Templates Updated:**
- author-details.html
- author-edit.html
- course-edit.html
- question-editor-mc.html
- question-editor-tf.html
- quiz-editor.html
- upload-excel.html
- upload-archive.html

**Lines of CSS Removed:** ~300+ (template-specific styles)
**Lines of CSS Added:** ~150 (general, reusable styles)

## 17. Related Documentation

- See `guidelines.md` Section 13 for UI/UX formatting standards
- See individual *-sd.md files for template-specific implementation
- See `README.md` for quick reference

---

**Status:** ✅ Production Ready

Style system is fully standardized across all templates.

## Author Operations

### Create / Update
- Author-facing create/update flows (authors, questions, quizzes, uploads) must use the shared form and button styles defined here (e.g., `.form-container`, `.btn-save`, `.btn-cancel`).

### View / List
- Author-centric list and detail pages (author-list, question-list, quiz-list, author-details) must apply the standardized layout (`.container mt-3`, `.card`, `.styled-table`) so that author workflows look and behave consistently.

### Delete / Archive
- Destructive actions available to authors (delete question/quiz, etc.) must use the `.btn-delete` style and clear visual cues from this document.

### Permissions & Roles
- Styling must visually distinguish disabled or unauthorized actions for authors (e.g., disabled buttons, muted text) but all permission logic itself is defined in `auth-sd.md` and related backend services.
