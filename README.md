# Campus Attendance — Native Android (Java)

Offline-first Android application for managing student records, taking daily attendance, and generating absentee reports.

> **Technology-stack notice:** The original company brief requested Flutter. This repository contains a native Android implementation using **Java, XML layouts, and SQLite**. This is a technology substitution, not a Flutter implementation. Obtain the evaluator's approval before submitting if Flutter is mandatory.

## Features

- Import student records from `.xlsx` workbooks using the Android file picker.
- Validate required headers (`Student Name`, `Roll Number`, `Course Name`), blank values, and duplicate roll numbers.
- Preview valid/invalid rows before confirming import.
- Store students and absence records locally in SQLite.
- Search students by name or roll number and filter by course.
- Take attendance by date with Present as the default status.
- Stage a Present/Absent choice, confirm it, and advance to the next student.
- Change an absent student to present by removing the matching absence record for that date.
- Clear an individual student's status or reset attendance for a date without deleting student records.
- Generate date/course-filtered absentee reports from SQLite.
- Create, save, share, and print PDF reports using Android's document and print interfaces.
- Configure the institution name in Settings.
- Operate offline after the initial Android Gradle dependencies have been downloaded.

## Technology

| Area | Implementation |
|---|---|
| Language | Java |
| Android UI | XML layouts + Material Components |
| Local database | Android SQLite via `SQLiteOpenHelper` |
| Excel import | Offline `.xlsx` ZIP/XML parser |
| PDF | Android `PdfDocument` |
| Printing/sharing | Android Print Framework and file sharing |
| Minimum Android | API 23 |
| Compile SDK | API 36 |
| Android Gradle Plugin | 8.12.3 |
| Gradle | 8.13 |

## Open and run

1. Extract the repository/project ZIP.
2. In Android Studio, choose **File → Open**.
3. Select the `CampusAttendance` root directory—the one containing `settings.gradle`, `build.gradle`, `gradlew.bat`, and `app/`.
4. Use Android Studio's bundled JDK 17 or newer and install Android SDK Platform 36 if prompted.
5. Allow the first Gradle sync to complete. Internet is needed for the initial Gradle/dependency downloads.
6. Select an Android emulator or connected device and run the `app` configuration.

On Windows PowerShell, from the project root:

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK is generated at:

```
app\build\outputs\apk\debug\app-debug.apk
```

To install on a connected emulator/device:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

## SQLite database

No separate SQLite installation or manual database creation is required. On first app launch, `DatabaseHelper` creates `student_attendance.db` in the app's private Android storage.

### `Students`

- `id INTEGER PRIMARY KEY AUTOINCREMENT`
- `student_name TEXT NOT NULL`
- `roll_number TEXT NOT NULL`
- `course_name TEXT NOT NULL`

### `Absence`

- `id INTEGER PRIMARY KEY AUTOINCREMENT`
- `date_time TEXT NOT NULL`
- `student_id INTEGER NOT NULL REFERENCES Students(id)`

The database enables foreign keys, adds indexes for student/report lookups, and enforces one absence per student per calendar date. Students without an absence record on the selected date are treated as Present. Attendance writes use transactions. Processed-student progress is stored locally in `SharedPreferences`; no extra session table is used.

To inspect the database while the app is running, open **View → Tool Windows → App Inspection → Database Inspector**, select the app process, then open `student_attendance.db`.

## Import sample data

The project includes `sample_students.xlsx` with 10 sample records. Copy it to an emulator's Downloads folder from the project root:

```powershell
adb push .\sample_students.xlsx /sdcard/Download/sample_students.xlsx
```

In the app, open the import workflow, choose the workbook, review the preview, and confirm. The importer reads the first worksheet and expects the three required headers within the first 10 rows. `.xlsx` is supported; legacy `.xls` is not. Roll numbers should be formatted as text in Excel to preserve leading zeroes such as `001`.

Existing roll numbers are skipped rather than overwritten or deleted. Blank rows are ignored; invalid and duplicate workbook rows are reported in the preview.

## Project structure

```text
CampusAttendance/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/studentattendance/
│       │   ├── data/DatabaseHelper.java
│       │   ├── model/AbsenceRecord.java
│       │   ├── model/Student.java
│       │   ├── service/PdfReportService.java
│       │   ├── service/XlsxImportService.java
│       │   └── ui/MainActivity.java
│       └── res/
├── gradle/wrapper/
├── tools/GradleWrapperMain.java
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── DATABASE_SCHEMA.sql
├── ASSIGNMENT_REQUIREMENTS.md
├── TEST_PLAN.md
└── sample_students.xlsx
```

## Data and privacy notes

- Student and attendance data stays in the app's private local database; no server, login, Firebase, or cloud sync is used.
- Closing and reopening the app preserves records.
- Uninstalling the app or clearing its storage/data removes its private database. Back up important information before doing either.
- `local.properties`, build outputs, and IDE-specific files are excluded from version control by `.gitignore`.

## Verification note

The developer reports that the app was launched on a Pixel 8 Pro emulator running Android 17 / API 37 and that the attendance and report screens were exercised. The screenshots and interactive testing were performed in the developer's local Android Studio environment. Re-run the full checklist in `TEST_PLAN.md` on the evaluator's target device before final assessment; this repository does not include a CI build artifact or independently generated test report.

## Assignment mapping

See [`ASSIGNMENT_REQUIREMENTS.md`](ASSIGNMENT_REQUIREMENTS.md) for a feature-by-feature mapping to the supplied assignment and for the native-Java technology deviation.
