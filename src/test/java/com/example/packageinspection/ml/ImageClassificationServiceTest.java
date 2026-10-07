package com.example.packageinspection.ml;

import com.example.packageinspection.entity.DamageType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class ImageClassificationServiceTest {

    private final MockImageClassificationService mockService = new MockImageClassificationService();
    private final DJLImageClassificationService djlService = new DJLImageClassificationService(mockService);

    @Test
    void testMockClassification_Dent() {
        MockMultipartFile file = new MockMultipartFile("file", "box_dent_sample.jpg", "image/jpeg", "image bytes".getBytes());
        ClassificationResult result = mockService.classify(file);
        assertNotNull(result);
        assertEquals(DamageType.DENT, result.getPredictedClass());
        assertTrue(result.getConfidence() >= 0.85);
        assertTrue(result.isMockModel());
    }

    @Test
    void testDJLClassification_FallbackToMock() {
        djlService.init(); // Model file doesn't exist, will fallback
        MockMultipartFile file = new MockMultipartFile("file", "puncture_test.jpg", "image/jpeg", "image bytes".getBytes());
        ClassificationResult result = djlService.classify(file);
        assertNotNull(result);
        assertEquals(DamageType.PUNCTURE, result.getPredictedClass());
    }
}
