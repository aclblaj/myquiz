# Admin Interface System Design

## Overview
Complete administration interface for managing users, roles, and permissions in MyQuiz application.

---

## Architecture

### System Flow
```
User Request (Browser)
    ↓
ThyMenuController → Checks permissions in JWT → Shows/hides admin menu
    ↓
ThyUserManagementController (myquiz-thymeleaf)
    ↓
HTTP REST Call → UserManagementController (myquiz-auth)
    ↓
HTTP REST Call → IAM Controllers (myquiz-iam)
    ↓
Database Operations
    ↓
Response → Back through layers → HTML Template Rendering
```

**Key Principle:** Thymeleaf controllers communicate ONLY with myquiz-auth, which then communicates with myquiz-iam. This maintains proper separation of concerns.

---

## Implementation Components

### myquiz-auth Module (6 files)

#### DTOs
1. **`UserManagementDto.java`** - DTO for user management operations
   - Fields: id, username, email, enabled, createdAt, updatedAt, roleNames

2. **`RoleManagementDto.java`** - DTO for role management operations
   - Fields: id, name, description, createdAt, permissionNames

3. **`PermissionManagementDto.java`** - DTO for permission operations
   - Fields: id, name, description, resource, action

#### REST Controllers (Communicate with myquiz-iam)
4. **`UserManagementController.java`** - `/api/admin/users`
5. **`RoleManagementController.java`** - `/api/admin/roles`
6. **`PermissionManagementController.java`** - `/api/admin/permissions`

### myquiz-thymeleaf Module (9 files)

#### Thymeleaf Controllers (Communicate with myquiz-auth)
7. **`ThyUserManagementController.java`** - `/admin/users`
8. **`ThyRoleManagementController.java`** - `/admin/roles`
9. **`ThyPermissionManagementController.java`** - `/admin/permissions`

#### HTML Templates
10. **`admin/user-list.html`** - User management list page
11. **`admin/user-edit.html`** - User edit page
12. **`admin/role-list.html`** - Role management list page
13. **`admin/role-add.html`** - Create new role page
14. **`admin/role-edit.html`** - Role edit page
15. **`admin/permission-list.html`** - Permission list page

---

## Features

### User Management
✅ List all users with their roles and status  
✅ Enable/disable user accounts  
✅ Assign multiple roles to users  
✅ Remove roles from users  
✅ Delete users  

### Role Management
✅ List all roles with permission counts  
✅ Create new custom roles  
✅ Edit role name and description  
✅ Assign multiple permissions to roles  
✅ Remove permissions from roles  
✅ Delete roles  

### Permission Management
✅ View all system permissions  
✅ Filter by resource and action  
✅ Read-only (system-defined permissions)  

---

## Security

### Menu Integration
Admin menu items are shown/hidden based on JWT permissions:
- `canManageUsers` - Shows User Management
- `canManageRoles` - Shows Role Management, Permission Management

### Access Control
- Session-based authentication via SessionService
- JWT authorization for all API calls
- SecurityConfig must include: `.requestMatchers("/api/admin/**").authenticated()`

---

## Troubleshooting

### 403 Forbidden Errors
1. Check SecurityConfig includes admin endpoint authentication
2. Restart myquiz-auth service
3. Clear browser cache and cookies
4. Login again to get fresh JWT token
5. Verify JWT contains required roles

---

## API Reference

### User Management API
```
GET    /api/admin/users                              - List all users
GET    /api/admin/users/{id}                         - Get user details
PUT    /api/admin/users/{id}/enabled                 - Enable/disable user
POST   /api/admin/users/{userId}/roles/{roleId}      - Assign role
DELETE /api/admin/users/{userId}/roles/{roleId}      - Remove role
DELETE /api/admin/users/{id}                         - Delete user
```

### Role Management API
```
GET    /api/admin/roles                              - List all roles
GET    /api/admin/roles/{id}                         - Get role details
POST   /api/admin/roles                              - Create role
PUT    /api/admin/roles/{id}                         - Update role
DELETE /api/admin/roles/{id}                         - Delete role
POST   /api/admin/roles/{roleId}/permissions/{permId} - Assign permission
DELETE /api/admin/roles/{roleId}/permissions/{permId} - Remove permission
```

### Permission Management API
```
GET    /api/admin/permissions                        - List all permissions
GET    /api/admin/permissions/{id}                   - Get permission details
```

---

## Deployment

### Service URLs
- myquiz-iam: http://localhost:8888
- myquiz-auth: http://localhost:8081
- myquiz-app: http://localhost:8080
- myquiz-thymeleaf: http://localhost:9090

### Admin Pages
- User Management: http://localhost:9090/admin/users
- Role Management: http://localhost:9090/admin/roles
- Permission Management: http://localhost:9090/admin/permissions

---

## Status
✅ **PRODUCTION READY**
