package com.example.packageinspection.service;

import com.example.packageinspection.dto.InspectionResponseDto;
import com.example.packageinspection.entity.*;
import com.example.packageinspection.exception.InvalidStatusTransitionException;
import com.example.packageinspection.ml.ClassificationResult;
import com.example.packageinspection.ml.ImageClassificationService;
import com.example.packageinspection.repository.InspectionRepository;
import com.example.packageinspection.util.InspectionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private ImageClassificationService imageClassificationService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private SeverityCalculationService severityCalculationService;

    @Mock
    private UserService userService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private InspectionMapper mapper;

    @InjectMocks
    private InspectionService inspectionService;

    @Test
    void testCreateInspection() {
        MockMultipartFile file = new MockMultipartFile("file", "dent.jpg", "image/jpeg", "test".getBytes());
        when(fileStorageService.storeFile(file)).thenReturn("uploads/dent.jpg");
        when(imageClassificationService.classify(file)).thenReturn(
                ClassificationResult.builder().predictedClass(DamageType.DENT).confidence(0.95).isMockModel(true).details("Mock").build()
        );
        when(severityCalculationService.calculateSeverity(DamageType.DENT, 0.95)).thenReturn(Severity.MEDIUM);
        when(severityCalculationService.determineInitialStatus(0.95)).thenReturn(InspectionStatus.OPEN);

        Inspection saved = Inspection.builder().id(1L).predictedClass(DamageType.DENT).confidence(0.95).severity(Severity.MEDIUM).status(InspectionStatus.OPEN).build();
        when(inspectionRepository.save(any(Inspection.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(InspectionResponseDto.builder().inspectionId(1L).predictedClass(DamageType.DENT).status(InspectionStatus.OPEN).build());

        InspectionResponseDto result = inspectionService.createInspection(file, "Sample note");
        assertNotNull(result);
        assertEquals(1L, result.getInspectionId());
        assertEquals(DamageType.DENT, result.getPredictedClass());

        verify(auditLogService, times(1)).logAction(eq(1L), eq(AuditAction.CREATED), eq("System"), anyString());
    }

    @Test
    void testEscalateInspection() {
        Inspection inspection = Inspection.builder().id(10L).status(InspectionStatus.OPEN).severity(Severity.LOW).build();
        when(inspectionRepository.findById(10L)).thenReturn(Optional.of(inspection));
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any())).thenAnswer(inv -> {
            Inspection arg = inv.getArgument(0);
            return InspectionResponseDto.builder().inspectionId(arg.getId()).status(arg.getStatus()).severity(arg.getSeverity()).build();
        });

        InspectionResponseDto response = inspectionService.escalateInspection(10L, "Supervisor");
        assertEquals(InspectionStatus.ESCALATED, response.getStatus());
        assertEquals(Severity.HIGH, response.getSeverity());
    }

    @Test
    void testResolveAlreadyResolved_ThrowsException() {
        Inspection inspection = Inspection.builder().id(20L).status(InspectionStatus.RESOLVED).build();
        when(inspectionRepository.findById(20L)).thenReturn(Optional.of(inspection));

        assertThrows(InvalidStatusTransitionException.class, () -> inspectionService.resolveInspection(20L, "Inspector"));
    }
}
