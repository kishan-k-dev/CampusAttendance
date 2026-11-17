package com.example.studentattendance.model;

public class AbsenceRecord {
    public final long studentId;
    public final String studentName;
    public final String rollNumber;
    public final String courseName;
    public final String dateTime;

    public AbsenceRecord(long studentId, String studentName, String rollNumber,
                         String courseName, String dateTime) {
        this.studentId = studentId;
        this.studentName = studentName == null ? "" : studentName;
        this.rollNumber = rollNumber == null ? "" : rollNumber;
        this.courseName = courseName == null ? "" : courseName;
        this.dateTime = dateTime == null ? "" : dateTime;
    }
}
