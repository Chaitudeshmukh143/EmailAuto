package com.emailauto.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExcelContactParser {
    private final DataFormatter formatter = new DataFormatter();
    private static final String REQUIRED_COLUMN = "email";

    public List<Contact> parse(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload a non-empty .xlsx file");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return parse(inputStream, file.getOriginalFilename());
        }
    }

    public List<Contact> parse(InputStream inputStream, String originalFilename) throws IOException {
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported");
        }
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> columns = headerColumns(header);
            requireColumn(columns, REQUIRED_COLUMN);
            List<Contact> contacts = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> placeholders = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> entry : columns.entrySet()) {
                    placeholders.put(entry.getKey(), value(row, entry.getValue()));
                }
                String name = placeholders.getOrDefault("name", "");
                String email = placeholders.getOrDefault("email", "");
                String company = placeholders.getOrDefault("company", "");
                if (!email.isBlank()) {
                    contacts.add(new Contact(name, email, company, placeholders));
                }
            }
            return contacts;
        }
    }

    public ExcelMetadata inspect(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload a non-empty .xlsx file");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return inspect(inputStream, file.getOriginalFilename());
        }
    }

    public ExcelMetadata inspect(InputStream inputStream, String originalFilename) throws IOException {
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported");
        }
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> columns = headerColumns(header);
            List<String> headers = new ArrayList<>(columns.keySet());
            List<String> placeholders = headers.stream().map(name -> "{{" + name + "}}").toList();
            return new ExcelMetadata(headers, placeholders, Math.max(0, sheet.getLastRowNum() - sheet.getFirstRowNum()));
        }
    }

    private Map<String, Integer> headerColumns(Row header) {
        if (header == null) {
            throw new IllegalArgumentException("The first row must contain at least an email header");
        }
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (Cell cell : header) {
            columns.put(formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT), cell.getColumnIndex());
        }
        return columns;
    }

    private void requireColumn(Map<String, Integer> columns, String name) {
        if (!columns.containsKey(name)) {
            throw new IllegalArgumentException("Missing required column: " + name);
        }
    }

    private String value(Row row, int column) {
        Cell cell = row.getCell(column);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    public record ExcelMetadata(List<String> headers, List<String> placeholders, int rowCount) {
        public ExcelMetadata {
            headers = Collections.unmodifiableList(headers);
            placeholders = Collections.unmodifiableList(placeholders);
        }
    }
}
