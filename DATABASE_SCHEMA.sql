PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS Students (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_name TEXT NOT NULL,
    roll_number TEXT NOT NULL,
    course_name TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_students_roll_unique
ON Students(roll_number COLLATE NOCASE);

CREATE INDEX IF NOT EXISTS idx_students_name
ON Students(student_name COLLATE NOCASE);

CREATE INDEX IF NOT EXISTS idx_students_course
ON Students(course_name COLLATE NOCASE);

CREATE TABLE IF NOT EXISTS Absence (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date_time TEXT NOT NULL,
    student_id INTEGER NOT NULL,
    FOREIGN KEY(student_id) REFERENCES Students(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_absence_student_date
ON Absence(student_id, date_time);

CREATE INDEX IF NOT EXISTS idx_absence_date
ON Absence(date_time);

CREATE TRIGGER IF NOT EXISTS trg_one_absence_per_student_day
BEFORE INSERT ON Absence
WHEN EXISTS (
    SELECT 1 FROM Absence a
    WHERE a.student_id = NEW.student_id
      AND substr(a.date_time, 1, 10) = substr(NEW.date_time, 1, 10)
)
BEGIN
    SELECT RAISE(ABORT, 'Duplicate absence for student/date');
END;
