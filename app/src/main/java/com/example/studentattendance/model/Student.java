package com.example.studentattendance.model;

public class Student {
    public final long id;
    public final String name;
    public final String rollNumber;
    public final String courseName;

    public Student(long id, String name, String rollNumber, String courseName) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.rollNumber = rollNumber == null ? "" : rollNumber;
        this.courseName = courseName == null ? "" : courseName;
    }
}
