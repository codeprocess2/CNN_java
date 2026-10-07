package com.example.packageinspection.service;

import com.example.packageinspection.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir = "uploads/";

    private Path uploadPath;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory at: " + this.uploadPath, e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        validateImageFile(file, originalFilename);

        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? ".jpg" : "." + extension);

        try {
            Path targetLocation = this.uploadPath.resolve(uniqueFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            // Relative path for database storage and client access
            return "uploads/" + uniqueFilename;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + uniqueFilename, e);
        }
    }

    public Path getFilePath(String relativePath) {
        String filename = Paths.get(relativePath).getFileName().toString();
        return this.uploadPath.resolve(filename);
    }

    private void validateImageFile(MultipartFile file, String filename) {
        String ext = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new FileStorageException("Invalid file type: ." + ext + ". Only JPG, JPEG, PNG, and WEBP are supported.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException("Invalid Content-Type: " + contentType + ". Must be an image file.");
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
