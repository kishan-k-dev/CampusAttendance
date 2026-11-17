# Assignment Requirements Traceability

This document maps the supplied Student Attendance Mobile Application brief to the native Android Java implementation in this repository.

> **Important technology deviation:** The supplied brief specifies Flutter/Dart. This project implements the requested product behavior using native Android Java and XML. The features below do not make it a Flutter project. Confirm that the company accepts this stack substitution before submission.

## Functional requirements

| Assignment requirement | Implementation location / approach |
|---|---|
| Import students from Excel | `XlsxImportService.java`; first worksheet of `.xlsx`; file picker; required-header and row validation; preview before import |
| Preserve roll numbers such as `001` | Roll number is persisted as SQLite `TEXT`; workbook values are handled as text |
| Avoid duplicate imports | Unique roll-number index; existing records are skipped and reported |
| Store students | `DatabaseHelper.java`, `Students` table |
| Student list and management | `MainActivity.java` student screen; search and course filtering |
| Present by default | Absence rows are stored only for absent students; no absence row means present for that date |
| Mark Present/Absent and advance | Attendance UI stages a choice; confirmation updates SQLite and advances to the next unprocessed student |
| Save absence records | `Absence` table with `student_id` foreign key and `date_time` |
| Change Absent to Present | Remove the matching student's absence row for the selected date |
| Avoid duplicate absence rows | Database trigger/index and transactional attendance update logic |
| Clear an individual status | Per-student attendance reset for the selected date |
| Clear attendance for a date | Date reset removes attendance records/progress without deleting Students |
| Attendance progress | Date-scoped processed IDs saved in `SharedPreferences` |
| Absentee report | Date/course-filtered data queried from SQLite |
| PDF export/print/share | `PdfReportService.java` and Android PDF/document/print interfaces |
| Institution name | Configurable in Settings |
| Offline operation | Local SQLite and local file parsing; network only needed for first Gradle/dependency download |

## Database schema

The project preserves the two requested tables:

- `Students`: `id`, `student_name`, `roll_number`, `course_name`
- `Absence`: `id`, `date_time`, `student_id` (foreign key to `Students.id`)

See `DATABASE_SCHEMA.sql` and `DatabaseHelper.java` for the executable schema and database setup.

## Report totals

For a selected date and course filter:

- Total students = matching student count.
- Total absent = matching absence records for the selected date.
- Total present = total students minus total absent.

The report date must match the attendance date to show the corresponding absence list.

## Known scope boundaries

- Native Android Java is used instead of the Flutter stack in the original brief.
- `.xlsx` is supported; legacy `.xls` is not.
- The importer reads the first worksheet and searches for the required headers near the top of that sheet.
- Attendance progress is kept in `SharedPreferences`, not a third persistent SQLite session table.
- No cloud account, backend server, or cross-device synchronization is provided.
