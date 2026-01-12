# QuestionServiceTest - Database Integration Tests

## Overview

The `QuestionServiceTest` class provides integration tests for the QuestionService, specifically testing the Excel file upload and parsing functionality as described in `upload-sd.md`.

## Prerequisites

Before running these tests, ensure the following requirements are met:

### 1. Docker PostgreSQL Database

The tests connect to the PostgreSQL database defined in `docker-compose.yml`:

- **Host**: localhost
- **Port**: 5433 (mapped from container's 5432)
- **Database Name**: myquiz
- **Username**: myquiz_user
- **Password**: myquiz_password

### 2. Start PostgreSQL with Docker

Make sure the PostgreSQL container is running:

```powershell
# Start PostgreSQL container from docker-compose
docker-compose up postgres -d

# Check if container is running
docker ps | grep myquiz-postgres

# Check container logs if needed
docker logs myquiz-postgres

# Stop container when done (optional)
docker-compose down
```

Alternative: Start all services:
```powershell
docker-compose --profile dev up -d
```

### 3. Test Data (for parseExcelFilesFromFolder test)

The `parseExcelFilesFromFolder` test requires Excel files to be present in a specific directory:

- Default directory: `C:\work\_mi\2025-NA\inpQ1\`
- Alternative test configurations are provided in the test (commented out)
- Excel files must be in `.xlsx` format
- Files should follow the naming convention expected by the parser

## Test Configuration

The tests use a dedicated test configuration file located at:
```
myquiz-app/src/test/resources/application.properties
```

Key configuration properties:
- Database connection to localhost PostgreSQL
- Hibernate DDL auto-update enabled
- Test-specific logging levels
- Swagger/OpenAPI disabled for tests
- Transactional rollback enabled

## Running the Tests

### Option 1: Run All Tests (Except Disabled Ones)

```powershell
cd C:\work\cla22\myquiz\myquiz-app
mvn test -Dtest=QuestionServiceTest
```

### Option 2: Run Specific Test

```powershell
# Run database connectivity test
mvn test -Dtest=QuestionServiceTest#testDatabaseConnectivity

# Run server encoding test
mvn test -Dtest=QuestionServiceTest#getServerEncoding

# Run questions by author ID test
mvn test -Dtest=QuestionServiceTest#getQuestionsByAuthorId
```

### Option 3: Run Disabled Tests

The `parseExcelFilesFromFolder` test is disabled by default. To enable and run it:

1. Open `QuestionServiceTest.java`
2. Remove the `@Disabled` annotation from the `parseExcelFilesFromFolder` method
3. Configure the correct directory path in the `InputParameters` constructor
4. Run the test:
   ```powershell
   mvn test -Dtest=QuestionServiceTest#parseExcelFilesFromFolder
   ```

## Test Descriptions

### 1. testDatabaseConnectivity()

**Purpose**: Verify that the test configuration properly connects to the local database.

**Test Flow**:
1. Create a test course
2. Verify course is persisted to database
3. Retrieve course from database
4. Verify course data matches

**Status**: Always enabled

### 2. parseExcelFilesFromFolder()

**Purpose**: Integration test for parsing Excel files from a folder structure. This validates the archive upload functionality described in `upload-sd.md` Section 2.4.

**Test Flow** (as per upload-sd.md):
1. Create/verify course exists (CourseService)
2. Check server encoding
3. Verify folder exists
4. Create quiz (QuizService)
5. Set template type for parsing
6. Parse all Excel files recursively (QuestionService.parseExcelFilesFromFolder)
7. Validate questions for duplicates (QuestionValidationService)

**Status**: Disabled by default (requires test data)

**Configuration**:
- Update the directory path in `InputParameters` to match your local test data
- Alternative configurations are provided for different courses/quizzes

### 3. getServerEncoding()

**Purpose**: Verify server encoding configuration.

**Test Flow**:
1. Get server encoding
2. Log the encoding
3. Verify encoding is not null

**Status**: Always enabled

### 4. getQuestionsByAuthorId()

**Purpose**: Test retrieving questions by author ID.

**Test Flow**:
1. Create quiz with questions
2. Retrieve questions by author ID
3. Log and verify questions

**Status**: Always enabled

### 5. getQuestionsByAuthorName()

**Purpose**: Test retrieving questions by author name.

**Test Flow**:
1. Create quiz with questions
2. Retrieve questions by author name
3. Log and verify questions

**Status**: Always enabled

### 6. delete()

**Purpose**: Clean up test data by deleting all entities.

**Test Flow**:
1. Delete all quiz authors
2. Delete all quizzes
3. Delete all questions
4. Delete all authors

**Status**: Always enabled

## Transactional Behavior

All tests are annotated with `@Transactional`, which means:
- Each test runs in its own transaction
- Changes are automatically rolled back after test completion
- Database state is restored after each test
- Tests do not interfere with each other

## Troubleshooting

### Database Connection Refused

**Error**: `Connection to localhost:5433 refused`

**Solution**:
1. Start the PostgreSQL Docker container: `docker-compose up postgres -d`
2. Check if container is running: `docker ps | grep myquiz-postgres`
3. Verify database exists: `myquiz`
4. Verify credentials: myquiz_user/myquiz_password
5. Check PostgreSQL is listening on port 5433: `netstat -an | findstr 5433`
6. Check container health: `docker inspect myquiz-postgres | findstr Health`

### Test Data Not Found

**Error**: `Test data folder should exist: C:\work\_mi\2025-NA\inpQ1\`

**Solution**:
1. Create the directory structure
2. Place valid Excel files in the directory
3. Or update the directory path in the test

### Authentication Failed

**Error**: `password authentication failed for user "myquiz_user"`

**Solution**:
1. Verify Docker credentials in `docker-compose.yml` match test configuration
2. Restart the PostgreSQL container: `docker-compose restart postgres`
3. Check container environment variables: `docker exec myquiz-postgres env | grep POSTGRES`
4. If needed, recreate the container: `docker-compose down && docker-compose up postgres -d`

### Encoding Issues

**Error**: Server encoding check fails

**Solution**:
1. Check server locale settings
2. Verify UTF-8 encoding is supported
3. Review EncodingService configuration

## Related Documentation

- `prompt/upload-sd.md` - Upload operations design document
- `prompt/author-sd.md` - Author operations design document (similar structure)
- `README.md` - Main project documentation

## Best Practices

1. **Always run database-dependent tests with a local database instance**
2. **Do not commit sensitive database credentials**
3. **Use @Disabled for tests requiring external resources (files, specific data)**
4. **Document test prerequisites clearly**
5. **Clean up test data after tests (handled by @Transactional)**
6. **Keep test data separate from production data**
7. **Use meaningful test data that represents real scenarios**

## Example Test Run Output

```
[INFO] Running com.unitbv.myquiz.app.services.QuestionServiceTest
2025-12-12T08:53:17.161+02:00  INFO 33604 --- Starting QuestionServiceTest
2025-12-12T08:53:22.029+02:00  INFO 33604 --- HikariPool-1 - Starting...
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 15.5 s
[INFO] BUILD SUCCESS
```

## Support

For issues or questions about these tests:
1. Check the error logs in the console output
2. Review the upload-sd.md design document
3. Verify database connectivity
4. Check test data availability

