# Admin Interface Software Design
## 1. Overview
This document defines the current administration interface for managing users, roles, permissions, and admin-only navigation entry points in MyQuiz.
The admin interface spans three layers:
- `myquiz-thymeleaf` for server-rendered admin pages
- `myquiz-auth` as the only admin API facade used by the UI
- `myquiz-iam` for persistence and identity/authorization data
## 2. Functional Scope
### 2.1 Main Features
- User listing, user edit, enable/disable, role assignment, and delete
- Role listing, create, edit, permission assignment/removal, and delete
- Permission listing and inspection
- Menu-level visibility of admin features based on current JWT permissions
### 2.2 Main End-to-End Calls
| Operation | Thymeleaf Route | Auth Route | Downstream IAM Responsibility |
|---|---|---|---|
| List users | `GET /admin/users` | `GET /api/admin/users` | fetch users and their roles |
| Edit user | `GET /admin/users/{id}/edit` | `GET /api/admin/users/{id}` + `GET /api/admin/roles` | fetch user and role candidates |
| Enable/disable user | `POST /admin/users/{id}/enabled` | `PUT /api/admin/users/{id}/enabled` | toggle user active state |
| Assign/remove role to user | `POST /admin/users/{userId}/roles/...` | `POST` / `DELETE /api/admin/users/{userId}/roles/{roleId}` | bind/unbind user role |
| Delete user | `POST /admin/users/{id}/delete` | `DELETE /api/admin/users/{id}` | remove user |
| List roles | `GET /admin/roles` | `GET /api/admin/roles` | fetch roles and permissions |
| Edit/create role | `/admin/roles/...` | `/api/admin/roles...` | create/update/delete role |
| Manage role permissions | `/admin/roles/...` | `/api/admin/roles/{roleId}/permissions/...` | bind/unbind permissions |
| List permissions | `GET /admin/permissions` | `GET /api/admin/permissions` | fetch permission catalog |
## 3. Architecture
### 3.1 Layering Rule
The admin UI must not call `myquiz-iam` directly.
Canonical request path:
1. browser → `myquiz-thymeleaf`
2. `myquiz-thymeleaf` → `myquiz-auth`
3. `myquiz-auth` → `myquiz-iam`
This keeps session/JWT concerns in the UI layer and keeps IAM behind the auth service boundary.
### 3.2 Main Components
- `myquiz-thymeleaf`
  - `ThyUserManagementController`
  - `ThyRoleManagementController`
  - `ThyPermissionManagementController`
  - admin templates under `templates/admin/`
- `myquiz-auth`
  - `UserManagementController`
  - `RoleManagementController`
  - `PermissionManagementController`
- `myquiz-iam`
  - `UsersController`
  - `RoleController`
  - `PermissionController`
## 4. Data Model and DTOs
Typical admin payloads include:
- user DTOs with `id`, `username`, `email`, `enabled`, timestamps, and assigned role names
- role DTOs with `id`, `name`, `description`, permission lists/counts
- permission DTOs with permission name and descriptive metadata
The Thymeleaf layer often consumes map-based payloads from `myquiz-auth`, while `myquiz-auth` adapts IAM responses into admin-facing DTOs.
## 5. Flows
### 5.1 User Management Flow
1. `ThyUserManagementController` validates session and permissions.
2. Controller rewrites `AUTH_API_URL` into `/api/admin/...` endpoints.
3. `myquiz-auth` fetches and enriches IAM user data with role names.
4. Thymeleaf renders `admin/user-list.html` or `admin/user-edit.html`.
### 5.2 Role Management Flow
1. `ThyRoleManagementController` validates admin access.
2. Role operations are forwarded to `myquiz-auth`.
3. `myquiz-auth` coordinates with IAM role endpoints.
4. Templates render role list/add/edit screens and permission selection data.
### 5.3 Permission Management Flow
1. `ThyPermissionManagementController` checks session and role-management permissions.
2. Read-focused permission requests are sent to `myquiz-auth`.
3. Permission data is rendered in `admin/permission-list.html`.
## 6. Permissions and Security
- Admin menu visibility is driven by JWT-derived menu flags in `ThyMenuController`.
- Current admin routes rely on both:
  - admin role checks (`hasAdminRole()`)
  - specific permission checks such as `MODIFY_USER` and `MODIFY_ROLE`
- Session/JWT state is managed centrally through `SessionService`.
Operational rule:
- `MODIFY_USER` gates user management routes.
- `MODIFY_ROLE` gates role management routes.
- permission management visibility is tied to role-management capability.
## 7. UI, API, and Service Responsibilities
### 7.1 Thymeleaf Responsibilities
- enforce session presence before calling downstream services
- present admin templates and redirect with user-facing error messages
- translate page actions into authorized REST calls to `myquiz-auth`
### 7.2 Auth-Service Responsibilities
- provide stable admin-facing endpoints under `/api/admin/**`
- adapt IAM payloads into UI-friendly structures
- keep direct IAM communication out of the UI tier
### 7.3 IAM Responsibilities
- own persistence for users, roles, and permissions
- enforce actual identity and authorization state changes
## 8. Validation and Error Handling
- missing session or expired token redirects to login
- missing permission redirects away from admin screens with access-denied semantics
- downstream failures are surfaced as list/edit page errors or redirect query messages
- not found responses are translated into safe redirects rather than blank pages where possible
## 9. Key Decisions
- keep `myquiz-auth` as the single admin API boundary
- keep permission logic explicit in Thymeleaf controllers for page-level gating
- keep menu visibility derived from token claims rather than separate API round-trips
- prefer controller-specific admin templates under `templates/admin/`
## 10. Implementation Notes
- Thymeleaf controllers:
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyUserManagementController.java`
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyRoleManagementController.java`
  - `myquiz-thymeleaf/src/main/java/com/unitbv/myquiz/thy/controller/ThyPermissionManagementController.java`
- Auth controllers:
  - `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/controller/UserManagementController.java`
  - `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/controller/RoleManagementController.java`
  - `myquiz-auth/src/main/java/com/unitbv/myquiz/auth/controller/PermissionManagementController.java`
- IAM controllers:
  - `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/controller/UsersController.java`
  - `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/controller/RoleController.java`
  - `myquiz-iam/src/main/java/com/unitbv/myquiz/iam/controller/PermissionController.java`
- Templates:
  - `myquiz-thymeleaf/src/main/resources/templates/admin/user-list.html`
  - `myquiz-thymeleaf/src/main/resources/templates/admin/user-edit.html`
  - `myquiz-thymeleaf/src/main/resources/templates/admin/role-list.html`
  - `myquiz-thymeleaf/src/main/resources/templates/admin/role-add.html`
  - `myquiz-thymeleaf/src/main/resources/templates/admin/role-edit.html`
  - `myquiz-thymeleaf/src/main/resources/templates/admin/permission-list.html`
Related docs:
- `prompt/auth-sd.md`
- `prompt/core-sd.md`
- `prompt/data-cleanup-sd.md`
