package com.example.packageinspection.dto;

import com.example.packageinspection.entity.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponseDto {
    private Long id;
    private Long inspectionId;
    private AuditAction action;
    private String performedBy;
    private OffsetDateTime timestamp;
    private String description;
}
