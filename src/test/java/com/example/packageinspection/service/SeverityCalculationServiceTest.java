package com.example.packageinspection.service;

import com.example.packageinspection.entity.DamageType;
import com.example.packageinspection.entity.InspectionStatus;
import com.example.packageinspection.entity.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeverityCalculationServiceTest {

    private final SeverityCalculationService service = new SeverityCalculationService();

    @Test
    void testCalculateSeverity_PunctureHighConfidence() {
        Severity severity = service.calculateSeverity(DamageType.PUNCTURE, 0.95);
        assertEquals(Severity.HIGH, severity);
    }

    @Test
    void testCalculateSeverity_DentLowConfidence() {
        Severity severity = service.calculateSeverity(DamageType.DENT, 0.80);
        assertEquals(Severity.LOW, severity);
    }

    @Test
    void testCalculateSeverity_NoDamage() {
        Severity severity = service.calculateSeverity(DamageType.NO_DAMAGE, 0.99);
        assertEquals(Severity.LOW, severity);
    }

    @Test
    void testDetermineInitialStatus_HighConfidence() {
        InspectionStatus status = service.determineInitialStatus(0.90);
        assertEquals(InspectionStatus.OPEN, status);
    }

    @Test
    void testDetermineInitialStatus_LowConfidence() {
        InspectionStatus status = service.determineInitialStatus(0.70);
        assertEquals(InspectionStatus.NEEDS_REVIEW, status);
    }
}
