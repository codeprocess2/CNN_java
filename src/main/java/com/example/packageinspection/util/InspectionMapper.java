package com.example.packageinspection.util;

import com.example.packageinspection.dto.InspectionResponseDto;
import com.example.packageinspection.entity.Inspection;
import com.example.packageinspection.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InspectionMapper {

    private final UserService userService;

    public InspectionResponseDto toDto(Inspection inspection) {
        if (inspection == null) return null;
        return InspectionResponseDto.builder()
                .inspectionId(inspection.getId())
                .imagePath(inspection.getImagePath())
                .predictedClass(inspection.getPredictedClass())
                .confidence(inspection.getConfidence())
                .severity(inspection.getSeverity())
                .status(inspection.getStatus())
                .description(inspection.getDescription())
                .createdAt(inspection.getCreatedAt())
                .updatedAt(inspection.getUpdatedAt())
                .assignedTo(userService.mapToDto(inspection.getAssignedTo()))
                .resolvedAt(inspection.getResolvedAt())
                .build();
    }
}
