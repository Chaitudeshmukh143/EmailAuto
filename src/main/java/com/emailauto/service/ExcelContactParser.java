package com.emailauto.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
            requireColumn(columns, "name");
            requireColumn(columns, "email");
            requireColumn(columns, "company");
            List<Contact> contacts = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String name = value(row, columns.get("name"));
                String email = value(row, columns.get("email"));
                String company = value(row, columns.get("company"));
                if (!email.isBlank()) {
                    contacts.add(new Contact(name, email, company));
                }
            }
            return contacts;
        }
    }

    private Map<String, Integer> headerColumns(Row header) {
        if (header == null) {
            throw new IllegalArgumentException("The first row must contain name, email, company headers");
        }
        Map<String, Integer> columns = new HashMap<>();
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
}
