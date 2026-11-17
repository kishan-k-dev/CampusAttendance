package com.example.studentattendance.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.studentattendance.model.AbsenceRecord;
import com.example.studentattendance.model.Student;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Local database. Absence rows are the source of truth; no row means Present. */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "student_attendance.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS Students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "student_name TEXT NOT NULL," +
                "roll_number TEXT NOT NULL," +
                "course_name TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_students_roll_unique " +
                "ON Students(roll_number COLLATE NOCASE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_students_name ON Students(student_name COLLATE NOCASE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_students_course ON Students(course_name COLLATE NOCASE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS Absence (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date_time TEXT NOT NULL," +
                "student_id INTEGER NOT NULL," +
                "FOREIGN KEY(student_id) REFERENCES Students(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_student_date ON Absence(student_id, date_time)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_date ON Absence(date_time)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_one_absence_per_student_day " +
                "BEFORE INSERT ON Absence " +
                "WHEN EXISTS (SELECT 1 FROM Absence a WHERE a.student_id=NEW.student_id " +
                "AND substr(a.date_time,1,10)=substr(NEW.date_time,1,10)) " +
                "BEGIN SELECT RAISE(ABORT, 'Duplicate absence for student/date'); END");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Keep existing data. Future schema changes should use explicit migrations.
    }

    public static final class ImportCounts {
        public final int inserted;
        public final int existing;
        public ImportCounts(int inserted, int existing) {
            this.inserted = inserted;
            this.existing = existing;
        }
    }

    public ImportCounts insertStudents(List<Student> students) {
        SQLiteDatabase db = getWritableDatabase();
        int inserted = 0;
        int existing = 0;
        db.beginTransaction();
        try {
            for (Student student : students) {
                if (student.name.trim().isEmpty() || student.rollNumber.trim().isEmpty()
                        || student.courseName.trim().isEmpty()) {
                    existing++;
                    continue;
                }
                ContentValues values = new ContentValues();
                values.put("student_name", student.name.trim());
                values.put("roll_number", student.rollNumber.trim());
                values.put("course_name", student.courseName.trim());
                long id = db.insertWithOnConflict("Students", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (id == -1) existing++; else inserted++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return new ImportCounts(inserted, existing);
    }

    public List<Student> getStudents(String search, String course) {
        List<Student> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder selection = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            selection.append(" AND (student_name LIKE ? OR roll_number LIKE ?)");
            String q = "%" + search.trim() + "%";
            args.add(q); args.add(q);
        }
        if (course != null && !course.equals("All courses")) {
            selection.append(" AND course_name = ?");
            args.add(course);
        }
        try (Cursor c = db.query("Students", new String[]{"id", "student_name", "roll_number", "course_name"},
                selection.toString(), args.toArray(new String[0]), null, null,
                "student_name COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                result.add(new Student(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)));
            }
        }
        return result;
    }

    public List<Student> getAllStudents() { return getStudents("", "All courses"); }

    public List<String> getCourses() {
        List<String> courses = new ArrayList<>();
        courses.add("All courses");
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT course_name FROM Students ORDER BY course_name COLLATE NOCASE", null)) {
            while (c.moveToNext()) courses.add(c.getString(0));
        }
        return courses;
    }

    public int getStudentCount() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM Students", null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public List<Long> getAbsentStudentIds(String date) {
        List<Long> ids = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT student_id FROM Absence WHERE substr(date_time,1,10)=?", new String[]{date})) {
            while (c.moveToNext()) ids.add(c.getLong(0));
        }
        return ids;
    }

    public boolean isAbsent(long studentId, String date) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM Absence WHERE student_id=? AND substr(date_time,1,10)=? LIMIT 1",
                new String[]{String.valueOf(studentId), date})) {
            return c.moveToFirst();
        }
    }

    public void setAttendanceStatus(long studentId, String date, boolean absent) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("Absence", "student_id=? AND substr(date_time,1,10)=?",
                    new String[]{String.valueOf(studentId), date});
            if (absent) {
                String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                ContentValues values = new ContentValues();
                values.put("date_time", date + "T" + time);
                values.put("student_id", studentId);
                db.insertOrThrow("Absence", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void clearAttendanceDate(String date) {
        getWritableDatabase().delete("Absence", "substr(date_time,1,10)=?", new String[]{date});
    }

    public void clearStudentAttendance(long studentId, String date) {
        getWritableDatabase().delete("Absence", "student_id=? AND substr(date_time,1,10)=?",
                new String[]{String.valueOf(studentId), date});
    }

    public List<AbsenceRecord> getAbsenceReport(String date, String course) {
        List<AbsenceRecord> result = new ArrayList<>();
        String sql = "SELECT s.id, s.student_name, s.roll_number, s.course_name, a.date_time " +
                "FROM Absence a INNER JOIN Students s ON s.id=a.student_id " +
                "WHERE substr(a.date_time,1,10)=?";
        List<String> args = new ArrayList<>();
        args.add(date);
        if (course != null && !course.equals("All courses")) {
            sql += " AND s.course_name=?";
            args.add(course);
        }
        sql += " ORDER BY s.student_name COLLATE NOCASE";
        try (Cursor c = getReadableDatabase().rawQuery(sql, args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                result.add(new AbsenceRecord(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)));
            }
        }
        return result;
    }
}
