package com.example.packageinspection.dto;

import com.example.packageinspection.entity.DamageType;
import com.example.packageinspection.entity.InspectionStatus;
import com.example.packageinspection.entity.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionResponseDto {
    private Long inspectionId;
    private String imagePath;
    private DamageType predictedClass;
    private Double confidence;
    private Severity severity;
    private InspectionStatus status;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UserResponseDto assignedTo;
    private OffsetDateTime resolvedAt;
}
