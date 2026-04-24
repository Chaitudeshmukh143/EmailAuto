package com.emailauto.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.emailauto.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryExcelService {
    private final ObjectProvider<Cloudinary> cloudinaryProvider;
    private final ExcelContactParser excelContactParser;
    private final AppProperties properties;
    private final RestClient restClient;

    public CloudinaryExcelService(ObjectProvider<Cloudinary> cloudinaryProvider, ExcelContactParser excelContactParser, AppProperties properties) {
        this.cloudinaryProvider = cloudinaryProvider;
        this.excelContactParser = excelContactParser;
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(15000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public String uploadExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload a non-empty .xlsx file");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported");
        }
        Cloudinary cloudinary = configuredCloudinary();
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "raw",
                "folder", properties.getCloudinary().getFolder(),
                "use_filename", true,
                "unique_filename", true,
                "overwrite", false));
        Object secureUrl = result.get("secure_url");
        if (secureUrl == null) {
            throw new IllegalStateException("Cloudinary did not return a secure URL");
        }
        return secureUrl.toString();
    }

    public List<Contact> downloadAndProcess(String fileUrl) throws IOException {
        URI uri = validatedUri(fileUrl);
        Resource resource = restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .body(Resource.class);
        if (resource == null) {
            throw new IllegalStateException("Cloudinary file could not be downloaded");
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return excelContactParser.parse(inputStream, "contacts.xlsx");
        }
    }

    private Cloudinary configuredCloudinary() {
        Cloudinary cloudinary = cloudinaryProvider.getIfAvailable();
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET.");
        }
        return cloudinary;
    }

    private URI validatedUri(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            throw new IllegalArgumentException("fileUrl is required");
        }
        URI uri = URI.create(fileUrl);
        String allowedHost = properties.getCloudinary().getAllowedDownloadHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || !uri.getHost().endsWith(allowedHost)) {
            throw new IllegalArgumentException("Only HTTPS Cloudinary URLs are allowed");
        }
        return uri;
    }
}
