# Upload Software Design
## 1. Overview
This document defines the current upload architecture for MyQuiz.
The upload subsystem supports:
- single Excel upload
- single archive upload
- archive-folder upload (multiple ZIP files)
- XML import
Uploads create or enrich question-bank structures and drive downstream validation, duplicate detection, and author/question creation workflows.
## 2. Functional Scope
### 2.1 Main Features
- render upload forms with course/template/study-year data
- forward multipart requests from Thymeleaf to backend
- validate upload requests at API boundary
- delegate processing to dedicated upload application services
### 2.2 Main End-to-End Calls
| Operation | Template | Thymeleaf Route | Backend Route | Main Backend Entry Point |
|---|---|---|---|---|
| Excel form | `upload-excel.html` | `GET /uploads/excel-form` | `GET /api/courses` for form data | course lookup only |
| Excel upload | `upload-excel.html` | `POST /uploads/excel-file` | `POST /api/upload/excel` | `UploadApplicationService.processExcelUpload(...)` |
| Archive form | `upload-archive.html` | `GET /uploads/archive-form` | `GET /api/courses` for form data | course lookup only |
| Archive upload | `upload-archive.html` | `POST /uploads/archive-file` | `POST /api/upload/archive` | `UploadApplicationService.processArchiveUpload(...)` |
| Archive-folder form | `upload-archive-folder.html` | `GET /uploads/archive-folder-form` | form data fetches only | — |
| Archive-folder upload | `upload-archive-folder.html` | `POST /uploads/archive-folder` | `POST /api/upload/archive-folder` | `UploadApplicationService.processArchiveFolderUpload(...)` |
| XML form | `upload-xml.html` | `GET /uploads/xml-form` | form data fetches only | — |
| XML upload | `upload-xml.html` | `POST /uploads/xml-file` | `POST /api/upload/xml` | `UploadApplicationService.processXmlUpload(...)` |
## 3. Architecture
### 3.1 Main Components
- `ThyUploadController` in `myquiz-thymeleaf`
- `UploadController` in `myquiz-app`
- `UploadApplicationService` in `myquiz-app`
- upload helper/support classes such as request validator, template resolver, and response factory
### 3.2 Layering
1. user submits form in Thymeleaf
2. Thymeleaf validates session and forwards multipart request
3. backend validates payload shape and required fields
4. upload application service performs parsing/import orchestration
5. success or error details are returned to the UI
## 4. Data Model and Contracts
### 4.1 Main Request Fields
Current upload flows use combinations of:
- `file`
- `archive`
- `archives[]`
- `xml`
- `username`
- `courseId`
- `name` or `questionBankName`
- `template`
- `studyYear`
### 4.2 Main Response Contracts
- string status messages for Excel, archive, and XML uploads
- `ArchiveFolderUploadResultDto` for folder processing summary and per-file results
## 5. Flows
### 5.1 Form Rendering
`ThyUploadController` loads courses, templates, and study-year options where needed before rendering the upload forms.
### 5.2 Excel Upload
1. User submits one Excel file with author/course/question-bank/template metadata.
2. Thymeleaf builds multipart request with authorization.
3. Backend validates file and template.
4. `UploadApplicationService.processExcelUpload(...)` processes the import.
### 5.3 Archive Upload
1. User uploads one ZIP archive with course/question-bank/study-year context.
2. Backend validates archive presence and required text values.
3. Archive is processed into imported question-bank content.
### 5.4 Archive-Folder Upload
1. User selects multiple ZIP archives from a folder.
2. Backend validates non-empty archive set and study year.
3. Service processes each archive and returns aggregate result counts.
### 5.5 XML Upload
1. User uploads XML with course/question-bank/study-year context.
2. Backend validates XML and target question-bank name.
3. Service imports compatible questions and reports outcome.
## 6. Permissions and Security
- upload pages require a valid session in Thymeleaf
- backend authorization remains enforced on upload endpoints
- multipart requests are constructed with the JWT authorization header via `SessionService`
## 7. UI, API, and Service Responsibilities
### 7.1 Thymeleaf Layer
- render upload forms
- gather dropdown data
- forward multipart uploads with authorization
- render success or error messages
### 7.2 API Layer
- validate required request parts and parameters
- convert validation failures into `400` responses
- delegate all business parsing/import logic to application services
### 7.3 Upload Application Layer
- orchestrate parsing and import workflow for each upload type
- coordinate question-bank, question, author, and archive-import side effects
## 8. Validation and Error Handling
- empty files are rejected immediately
- invalid template values are rejected before service execution
- missing question-bank names or study-year values are rejected where required
- Thymeleaf upload pages surface backend validation messages back to the user
## 9. Key Decisions
- keep multipart construction centralized in `SessionService`
- keep controller validation lightweight and delegate business logic to `UploadApplicationService`
- support both one-file and batch archive import flows
- keep XML import as a first-class upload path rather than a side utility
## 10. Implementation Notes
- API contract:
  - `myquiz-api/src/main/java/com/unitbv/myquiz/api/interfaces/UploadApi.java`
- Thymeleaf controller:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyUploadController.java`
- Backend controller:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/upload/api/UploadController.java`
- Backend application service:
  - `myquiz-app/src/main/java/com/unitbv/myquiz/app/upload/application/UploadApplicationService.java`
- Templates:
  - `myquiz-thymeleaf/src/main/resources/templates/upload-excel.html`
  - `myquiz-thymeleaf/src/main/resources/templates/upload-archive.html`
  - `myquiz-thymeleaf/src/main/resources/templates/upload-archive-folder.html`
  - `myquiz-thymeleaf/src/main/resources/templates/upload-xml.html`
Related docs:
- `prompt/question-bank-sd.md`
- `prompt/question-sd.md`
- `prompt/author-sd.md`
