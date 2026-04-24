package com.emailauto.web;

import com.emailauto.service.CloudinaryExcelService;
import com.emailauto.service.Contact;
import com.emailauto.web.dto.CloudinaryExcelResponse;
import com.emailauto.web.dto.ExcelContactsResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CloudinaryExcelController {
    private final CloudinaryExcelService cloudinaryExcelService;

    public CloudinaryExcelController(CloudinaryExcelService cloudinaryExcelService) {
        this.cloudinaryExcelService = cloudinaryExcelService;
    }

    @PostMapping("/api/files/excel")
    public CloudinaryExcelResponse uploadExcel(@RequestParam MultipartFile file) throws IOException {
        return new CloudinaryExcelResponse(cloudinaryExcelService.uploadExcel(file));
    }

    @PostMapping("/api/files/excel/process")
    public ExcelContactsResponse processCloudinaryExcel(@RequestParam String fileUrl) throws IOException {
        List<Contact> contacts = cloudinaryExcelService.downloadAndProcess(fileUrl);
        return new ExcelContactsResponse(contacts.size(), contacts);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<EmailController.ErrorResponse> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new EmailController.ErrorResponse(ex.getMessage()));
    }
}
