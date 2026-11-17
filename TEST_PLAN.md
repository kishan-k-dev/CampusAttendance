# Campus Attendance — Test Plan

Run these checks on a clean emulator/device and record the actual result and date before presenting the project to the company.

| # | Test | Expected result | Result |
|---|---|---|---|
| 1 | Launch app with an empty database | Dashboard opens; total students = 0; no crash | Verify locally |
| 2 | Import `sample_students.xlsx` | Preview shows 10 valid rows; confirmation inserts 10 students | Verify locally |
| 3 | Import the same workbook again | No new records; existing roll numbers are skipped; no overwrites | Verify locally |
| 4 | Import workbook missing a required header | Clear validation error; no database changes | Verify locally |
| 5 | Import rows with blank name/roll/course | Invalid rows are identified and skipped | Verify locally |
| 6 | Include duplicate roll numbers in one workbook | Later duplicate rows are flagged and skipped | Verify locally |
| 7 | Search by name/roll and filter by course | Results match selected query/filter | Verify locally |
| 8 | Mark a student Present | Progress increments and next unprocessed student is selected | Verify locally |
| 9 | Mark a student Absent | Absence row is saved; row shows Absent; next student is selected | Verify locally |
| 10 | Change an absent student to Present | Matching absence row for that date is removed | Verify locally |
| 11 | Toggle a student's staged choice | Choice changes visually; SQLite is updated only on confirmation | Verify locally |
| 12 | Clear one student's status | That student's absence row and processed marker are cleared for the date | Verify locally |
| 13 | Clear attendance for a date | Absences/progress for that date clear; Students remain unchanged | Verify locally |
| 14 | Complete attendance | Completion state and report navigation are shown | Verify locally |
| 15 | View report by date/course | Totals and absence list match the selected date and course | Verify locally |
| 16 | Generate PDF with absentees | Readable PDF contains selected report data | Verify locally |
| 17 | Generate PDF with no absentees | PDF clearly states that no absentees were recorded | Verify locally |
| 18 | Save/share/print PDF | Android document picker, share chooser and print UI open | Verify locally |
| 19 | Force-close and relaunch app | Students and absence rows persist | Verify locally |
| 20 | Retake attendance for same date | Absence row is updated without duplicate student/date entries | Verify locally |

## Developer environment note

The developer reports successful launch and manual use on a Pixel 8 Pro emulator running Android 17 / API 37, including the attendance and report screens. The full 20-case matrix above should still be run and recorded before formal evaluation; do not mark every test as passed based only on the app launching.

## Data notes

- Roll Number should be stored as text in Excel to preserve leading zeros such as `001`.
- `.xlsx` is supported; legacy `.xls` is not.
- Present is the default: total present equals total students minus saved absences for the selected date/course.
- Attendance progress is stored in `SharedPreferences`, not in an additional SQLite table.
