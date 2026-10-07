package com.example.packageinspection.service;

import com.example.packageinspection.exception.FileStorageException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private final FileStorageService service = new FileStorageService();

    @Test
    void testStoreFile_Success() {
        service.init();
        MockMultipartFile file = new MockMultipartFile("file", "test_dent.jpg", "image/jpeg", "fake image bytes".getBytes());
        String relativePath = service.storeFile(file);
        assertNotNull(relativePath);
        assertTrue(relativePath.startsWith("uploads/"));
    }

    @Test
    void testStoreFile_InvalidExtension() {
        service.init();
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "fake pdf".getBytes());
        assertThrows(FileStorageException.class, () -> service.storeFile(file));
    }
}
