# Data Cleanup and Statistics System Design

## Overview
Data cleanup service and database statistics functionality for MyQuiz application.

---

## Data Cleanup Service

### Purpose
Provides functionality to delete all quiz-related data except users, roles, and permissions.

### Features
- Delete all questions, quizzes, quiz authors, authors, and courses
- Preserve user authentication data
- Permission-based access control
- Transaction management
- Detailed logging and timing

### API Endpoint
```
DELETE /api/data/delete-all
```

**Requirements:**
- Must be authenticated
- Must have `MODIFY_DATA` permission

### Implementation
**File:** `DataCleanupService.java` in myquiz-app module

**Key Methods:**
- `deleteAllDataExceptUsersRolesPermissions()` - Main cleanup method
- `canViewExtendedStatistics(Authentication)` - Permission check

### Exception Handling
**Custom Exception:** `DataCleanupException`
```java
public static class DataCleanupException extends RuntimeException {
    public DataCleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Code Quality
✅ Custom exceptions (no generic RuntimeException)  
✅ Appropriate logging levels (DEBUG for permission checks, INFO for operations)  
✅ Transaction management  
✅ Error context with timing information  

---

## Database Statistics Feature

### Purpose
Display record counts for all database tables accessible from Help menu.

### Features
- Two-column table display (Table Name | Record Count)
- Permission-based visibility (basic vs extended statistics)
- Refresh functionality
- Consistent UI with existing application style

### API Endpoint
```
GET /api/data/statistics
```

**Response:** `Map<String, Long>` containing table names and record counts

### Statistics Categories

#### Basic Statistics (always visible):
- questionErrors
- quizErrors
- questions
- quizAuthors
- quizzes
- authors
- courses

#### Extended Statistics (permission-based):
Requires `canViewExtendedStatistics` permission:
- users
- roles
- permissions

### UI Implementation

**Template:** `database-statistics.html` in myquiz-thymeleaf module

**URL:** `/statistics`

**Menu Integration:** Help menu dropdown
- About → `/help`
- Database Statistics → `/statistics`

### Permission Logic
```java
private boolean canViewExtendedStatistics(Authentication auth) {
    if (auth == null) return false;
    
    return auth.getAuthorities().stream()
        .anyMatch(authority -> {
            String authorityName = authority.getAuthority();
            return authorityName.equals("ADMIN") || 
                   authorityName.equals("MODIFY_DATA") ||
                   authorityName.equals("READ_EXTENDED_STATISTICS");
        });
}
```

---

## Architecture

### Data Flow for Statistics
```
User → Help Menu → Database Statistics
    ↓
ThyHomeController.getStatistics()
    ↓
HTTP GET /api/data/statistics
    ↓
DataCleanupController.getDatabaseStatistics()
    ↓
DataCleanupService (check permissions)
    ↓
Query database for counts
    ↓
Return Map<String, Long>
    ↓
Render database-statistics.html template
```

### Data Flow for Cleanup
```
Admin → API Call DELETE /api/data/delete-all
    ↓
DataCleanupController.deleteAllData()
    ↓
Permission Check (MODIFY_DATA required)
    ↓
DataCleanupService.deleteAllDataExceptUsersRolesPermissions()
    ↓
Start transaction
    ↓
Delete in order: Questions → QuizAuthors → Quizzes → Authors → Courses
    ↓
Commit transaction
    ↓
Return success message with timing
```

---

## Logging Strategy

### Log Levels
- **TRACE:** Authority details during permission checks
- **DEBUG:** Permission check results
- **INFO:** Operation start/end, timing, counts
- **WARN:** Permission denials
- **ERROR:** Exceptions and failures

### Example Logs
```
INFO  - Starting delete all data operation
DEBUG - [PERMISSION CHECK] User has authority: ADMIN
INFO  - Successfully deleted all data except users/roles/permissions in 1234ms
WARN  - [PERMISSION DENIED] User guest attempted to view extended statistics
ERROR - Error during data cleanup after 567ms: Connection timeout
```

---

## Security

### Access Control
- Cleanup requires `MODIFY_DATA` permission
- Extended statistics require `READ_EXTENDED_STATISTICS`, `MODIFY_DATA`, or `ADMIN`
- Basic statistics available to all authenticated users
- Session validation for all endpoints

### JWT Integration
- Permissions extracted from JWT token
- Authorities checked against required permissions
- Proper handling of missing/invalid authentication

---

## Error Handling

### Cleanup Errors
- Wrap exceptions in `DataCleanupException`
- Include timing information in error logs
- Roll back transaction on failure
- Return meaningful error messages

### Statistics Errors
- Log permission denials at WARN level
- Return empty map on errors (graceful degradation)
- Display error message in UI
- Maintain session validity

---

## Testing

### Cleanup Testing
1. Test with MODIFY_DATA permission (should succeed)
2. Test without permission (should deny)
3. Test transaction rollback on error
4. Verify timing logs
5. Confirm users/roles/permissions preserved

### Statistics Testing
1. Test basic statistics for regular users
2. Test extended statistics for admin users
3. Test permission denial for guests
4. Verify table counts accuracy
5. Test refresh functionality

---

## Code Reviews and Fixes

#### Issues Fixed
1. ✅ **Generic RuntimeException** → Custom `DataCleanupException`
2. ✅ **Verbose INFO logging** → DEBUG level for permission checks
3. ✅ **Missing timing in errors** → Added elapsed time to error logs

#### Quality Improvements
- Passes SonarQube quality gates
- Reduced production log noise
- Better exception hierarchy
- Improved error context

---

## Deployment

### Files Modified
- `DataCleanupService.java` - Main service class
- `DataCleanupController.java` - REST API controller
- `ThyHomeController.java` - Statistics page controller
- `database-statistics.html` - Statistics template
- `fragments.html` - Menu integration

### Configuration
No additional configuration required. Uses existing:
- Database connection
- JWT authentication
- Session management
- Permission system

---

## Status
✅ **PRODUCTION READY**
