## myquiz
Import of multiple choice questions from xlsx files into postgresql database and export them to moodle xml format.
The import can be made by adapting and running the unit tests.

The first sheet of the input file is read and its header contains the next structure:

| No | Title | Text | PR1 | Response 1 | PR2 | Response 2 | PR3 | Response 3 | PR4 | Response 4 |


The application is written in Java 21 and uses Spring Boot 3.3.5 and Maven 3.8.2.

## Remarks
- Download with: git clone https://github.com/aclblaj/myquiz.git
- Open in IntelliJ or Eclipse, in IntelliJ, access project structure and set jdk to java 17
- Source files should be available into a subfolder: "inpQ1\John Doe_123\fis.xlsx"
- Create an empty database myQuiz
- Set the database encoding to UTF-8
``` sql
SET client_encoding = 'UTF8';
UPDATE pg_database SET datcollate='en_US.UTF-8', datctype='en_US.UTF-8' WHERE datname='postgres';
UPDATE pg_database SET encoding = pg_char_to_encoding('UTF8') WHERE datname = 'myQuiz' ;
```
- Check  that the database encoding is UTF-8
``` sql
SELECT datname, pg_encoding_to_char(encoding) AS encoding FROM pg_database WHERE datname = 'myQuiz'
```
- Adapt the connection string inside the application.properties file (e.g switch to: spring.datasource.url=jdbc:postgresql://localhost:5432/myQuiz)
- Change path to files and run the unit test parseExcelFilesFromFolder from the QuestionServiceTest test class

## GUI 
- start the spring boot application and access http://localhost:8080