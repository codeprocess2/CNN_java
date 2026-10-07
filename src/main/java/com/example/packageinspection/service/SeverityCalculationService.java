package com.example.packageinspection.service;

import com.example.packageinspection.entity.DamageType;
import com.example.packageinspection.entity.InspectionStatus;
import com.example.packageinspection.entity.Severity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SeverityCalculationService {

    @Value("${cnn.confidence.threshold:0.85}")
    private double confidenceThreshold = 0.85;

    public Severity calculateSeverity(DamageType predictedClass, double confidence) {
        if (predictedClass == null) {
            return Severity.LOW;
        }

        switch (predictedClass) {
            case PUNCTURE:
            case CRUSHED:
                return confidence >= 0.75 ? Severity.HIGH : Severity.MEDIUM;
            case TEAR:
                return confidence >= 0.85 ? Severity.HIGH : Severity.MEDIUM;
            case DENT:
                return confidence >= 0.85 ? Severity.MEDIUM : Severity.LOW;
            case NO_DAMAGE:
            default:
                return Severity.LOW;
        }
    }

    public InspectionStatus determineInitialStatus(double confidence) {
        if (confidence < confidenceThreshold) {
            return InspectionStatus.NEEDS_REVIEW;
        }
        return InspectionStatus.OPEN;
    }
}
