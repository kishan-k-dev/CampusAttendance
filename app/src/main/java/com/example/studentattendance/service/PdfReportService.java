package com.example.studentattendance.service;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import com.example.studentattendance.model.AbsenceRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public final class PdfReportService {
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int LEFT = 42;
    private static final int RIGHT = 553;

    public File create(Context context, String institution, String date, String course,
                       int total, int present, int absent, List<AbsenceRecord> records) throws Exception {
        File directory = new File(context.getCacheDir(), "reports");
        if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Could not create report folder.");
        String safeDate = date.replaceAll("[^0-9-]", "");
        File output = new File(directory, "attendance_" + safeDate + ".pdf");
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int pageNumber = 1;
        PdfDocument.Page page = startPage(document, pageNumber);
        Canvas canvas = page.getCanvas();
        int y = 56;

        paint.setColor(0xFF183B68);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setTextSize(20);
        canvas.drawText("STUDENT ATTENDANCE REPORT", LEFT, y, paint);
        y += 26;
        paint.setColor(0xFF52657D);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(11);
        canvas.drawText(institution == null || institution.trim().isEmpty() ? "Institution name not configured" : institution, LEFT, y, paint);
        y += 20;
        canvas.drawText("Attendance date: " + date + "     Course: " + course, LEFT, y, paint);
        y += 26;

        paint.setColor(0xFFEAF1FA);
        canvas.drawRoundRect(LEFT, y - 16, RIGHT, y + 35, 10, 10, paint);
        paint.setColor(0xFF183B68);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(12);
        canvas.drawText("Total students: " + total, LEFT + 14, y + 3, paint);
        canvas.drawText("Present: " + present, LEFT + 185, y + 3, paint);
        canvas.drawText("Absent: " + absent, LEFT + 335, y + 3, paint);
        y += 58;

        paint.setColor(0xFF183B68);
        canvas.drawRect(LEFT, y - 15, RIGHT, y + 9, paint);
        paint.setColor(0xFFFFFFFF);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(9);
        canvas.drawText("NO.", LEFT + 8, y, paint);
        canvas.drawText("STUDENT NAME", LEFT + 42, y, paint);
        canvas.drawText("ROLL NUMBER", LEFT + 245, y, paint);
        canvas.drawText("COURSE", LEFT + 365, y, paint);
        y += 28;

        paint.setTextSize(9);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        if (records.isEmpty()) {
            paint.setColor(0xFF16845B);
            canvas.drawText("No absentees recorded for this date and course.", LEFT + 8, y + 5, paint);
        } else {
            int serial = 1;
            for (AbsenceRecord record : records) {
                if (y > PAGE_HEIGHT - 55) {
                    document.finishPage(page);
                    pageNumber++;
                    page = startPage(document, pageNumber);
                    canvas = page.getCanvas();
                    y = 54;
                }
                paint.setColor(serial % 2 == 0 ? 0xFFF4F7FB : 0xFFFFFFFF);
                canvas.drawRect(LEFT, y - 13, RIGHT, y + 12, paint);
                paint.setColor(0xFF23364D);
                canvas.drawText(String.valueOf(serial), LEFT + 8, y + 3, paint);
                canvas.drawText(clip(record.studentName, 31), LEFT + 42, y + 3, paint);
                canvas.drawText(clip(record.rollNumber, 17), LEFT + 245, y + 3, paint);
                canvas.drawText(clip(record.courseName, 22), LEFT + 365, y + 3, paint);
                y += 25;
                serial++;
            }
        }
        y = Math.min(y + 30, PAGE_HEIGHT - 35);
        paint.setColor(0xFF718096);
        paint.setTextSize(8);
        canvas.drawText("Generated offline by Campus Attendance", LEFT, y, paint);
        document.finishPage(page);

        try (FileOutputStream stream = new FileOutputStream(output)) {
            document.writeTo(stream);
        } finally {
            document.close();
        }
        return output;
    }

    private PdfDocument.Page startPage(PdfDocument document, int number) {
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create();
        PdfDocument.Page page = document.startPage(info);
        page.getCanvas().drawColor(0xFFFFFFFF);
        return page;
    }

    private String clip(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }
}
