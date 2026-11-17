package com.example.studentattendance.service;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Xml;

import com.example.studentattendance.model.Student;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Minimal offline XLSX reader using Android's built-in ZIP and XML Pull APIs. */
public final class XlsxImportService {
    private static final int MAX_FILE_BYTES = 25 * 1024 * 1024;

    public static final class Preview {
        public final List<Student> validStudents = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
        public int skippedBlankRows;
        public int duplicateRows;
    }

    public Preview read(ContentResolver resolver, Uri uri) throws Exception {
        Map<String, byte[]> entries = readZip(resolver, uri);
        List<String> sharedStrings = parseSharedStrings(entries.get("xl/sharedStrings.xml"));
        String sheetPath = findFirstSheetPath(entries);
        byte[] sheetBytes = entries.get(sheetPath);
        if (sheetBytes == null) throw new IllegalArgumentException("The workbook does not contain its first worksheet.");
        List<Map<Integer, String>> rows = parseSheet(sheetBytes, sharedStrings);
        if (rows.isEmpty()) throw new IllegalArgumentException("The selected worksheet is empty.");

        int headerRowIndex = -1;
        int nameCol = -1, rollCol = -1, courseCol = -1;
        for (int i = 0; i < Math.min(rows.size(), 10); i++) {
            Map<Integer, String> row = rows.get(i);
            for (Map.Entry<Integer, String> cell : row.entrySet()) {
                String header = normalize(cell.getValue());
                if (header.equals("student name")) nameCol = cell.getKey();
                if (header.equals("roll number")) rollCol = cell.getKey();
                if (header.equals("course name")) courseCol = cell.getKey();
            }
            if (nameCol >= 0 && rollCol >= 0 && courseCol >= 0) {
                headerRowIndex = i;
                break;
            }
            nameCol = rollCol = courseCol = -1;
        }
        if (headerRowIndex < 0) {
            throw new IllegalArgumentException("Required columns not found. Include Student Name, Roll Number and Course Name in the first 10 rows.");
        }

        Preview preview = new Preview();
        Set<String> seenRolls = new HashSet<>();
        for (int i = headerRowIndex + 1; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            String name = clean(row.get(nameCol));
            String roll = clean(row.get(rollCol));
            String course = clean(row.get(courseCol));
            if (name.isEmpty() && roll.isEmpty() && course.isEmpty()) {
                preview.skippedBlankRows++;
                continue;
            }
            int displayRow = i + 1;
            if (name.isEmpty() || roll.isEmpty() || course.isEmpty()) {
                preview.errors.add("Row " + displayRow + ": Student Name, Roll Number and Course Name are all required.");
                continue;
            }
            String key = roll.toLowerCase(Locale.ROOT);
            if (!seenRolls.add(key)) {
                preview.duplicateRows++;
                preview.errors.add("Row " + displayRow + ": duplicate roll number '" + roll + "' within this workbook.");
                continue;
            }
            preview.validStudents.add(new Student(0, name, roll, course));
        }
        return preview;
    }

    private Map<String, byte[]> readZip(ContentResolver resolver, Uri uri) throws Exception {
        Map<String, byte[]> result = new HashMap<>();
        int total = 0;
        InputStream input = resolver.openInputStream(uri);
        if (input == null) throw new IllegalArgumentException("Could not open the selected file.");
        try (InputStream source = input; ZipInputStream zip = new ZipInputStream(source)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int count;
                    while ((count = zip.read(buffer)) != -1) {
                        total += count;
                        if (total > MAX_FILE_BYTES) throw new IllegalArgumentException("Workbook is too large (25 MB uncompressed limit).");
                        out.write(buffer, 0, count);
                    }
                    result.put(entry.getName(), out.toByteArray());
                }
                zip.closeEntry();
            }
        }
        if (!result.containsKey("xl/workbook.xml")) throw new IllegalArgumentException("This is not a valid .xlsx workbook.");
        return result;
    }

    private List<String> parseSharedStrings(byte[] bytes) throws Exception {
        List<String> strings = new ArrayList<>();
        if (bytes == null) return strings;
        XmlPullParser parser = newParser(bytes);
        boolean inSi = false, inText = false;
        StringBuilder current = new StringBuilder();
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                if ("si".equals(parser.getName())) { inSi = true; current.setLength(0); }
                else if (inSi && "t".equals(parser.getName())) inText = true;
            } else if (event == XmlPullParser.TEXT && inSi && inText) {
                current.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG) {
                if ("t".equals(parser.getName())) inText = false;
                if ("si".equals(parser.getName())) { strings.add(current.toString()); inSi = false; }
            }
        }
        return strings;
    }

    private String findFirstSheetPath(Map<String, byte[]> entries) throws Exception {
        byte[] workbook = entries.get("xl/workbook.xml");
        byte[] rels = entries.get("xl/_rels/workbook.xml.rels");
        String firstRelationshipId = null;
        XmlPullParser parser = newParser(workbook);
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "sheet".equals(parser.getName())) {
                firstRelationshipId = parser.getAttributeValue(
                        "http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id");
                if (firstRelationshipId == null) firstRelationshipId = parser.getAttributeValue(null, "r:id");
                break;
            }
        }
        if (rels != null && firstRelationshipId != null) {
            parser = newParser(rels);
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "Relationship".equals(parser.getName())) {
                    String id = parser.getAttributeValue(null, "Id");
                    if (firstRelationshipId.equals(id)) {
                        String target = parser.getAttributeValue(null, "Target");
                        if (target != null) {
                            target = target.replace("\\", "/");
                            if (target.startsWith("/")) return target.substring(1);
                            if (target.startsWith("xl/")) return target;
                            return "xl/" + target;
                        }
                    }
                }
            }
        }
        for (String name : entries.keySet()) {
            if (name.matches("xl/worksheets/sheet[0-9]+\\.xml")) return name;
        }
        throw new IllegalArgumentException("Could not locate a worksheet in this .xlsx file.");
    }

    private List<Map<Integer, String>> parseSheet(byte[] bytes, List<String> sharedStrings) throws Exception {
        List<Map<Integer, String>> rows = new ArrayList<>();
        XmlPullParser parser = newParser(bytes);
        Map<Integer, String> row = null;
        String cellRef = null, cellType = null;
        StringBuilder cellValue = new StringBuilder();
        boolean inCell = false, collect = false;
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("row".equals(tag)) row = new HashMap<>();
                else if ("c".equals(tag) && row != null) {
                    inCell = true;
                    cellRef = parser.getAttributeValue(null, "r");
                    cellType = parser.getAttributeValue(null, "t");
                    cellValue.setLength(0);
                } else if (inCell && ("v".equals(tag) || "t".equals(tag))) {
                    collect = true;
                }
            } else if (event == XmlPullParser.TEXT && inCell && collect) {
                cellValue.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG) {
                String tag = parser.getName();
                if ("v".equals(tag) || "t".equals(tag)) collect = false;
                if ("c".equals(tag) && inCell && row != null) {
                    int col = columnIndex(cellRef);
                    String value = cellValue.toString();
                    if ("s".equals(cellType)) {
                        try { value = sharedStrings.get(Integer.parseInt(value)); }
                        catch (Exception ignored) { value = ""; }
                    }
                    if (col >= 0) row.put(col, value);
                    inCell = false;
                }
                if ("row".equals(tag) && row != null) {
                    rows.add(row);
                    row = null;
                }
            }
        }
        return rows;
    }

    private XmlPullParser newParser(byte[] bytes) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new ByteArrayInputStream(bytes), "UTF-8");
        return parser;
    }

    private int columnIndex(String reference) {
        if (reference == null) return -1;
        int value = 0;
        for (int i = 0; i < reference.length(); i++) {
            char ch = reference.charAt(i);
            if (ch < 'A' || ch > 'Z') break;
            value = value * 26 + (ch - 'A' + 1);
        }
        return value - 1;
    }

    private String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
