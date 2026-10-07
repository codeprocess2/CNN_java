package com.example.packageinspection.service;

import com.example.packageinspection.dto.AuditLogResponseDto;
import com.example.packageinspection.entity.AuditAction;
import com.example.packageinspection.entity.AuditLog;
import com.example.packageinspection.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog logAction(Long inspectionId, AuditAction action, String performedBy, String description) {
        AuditLog auditLog = AuditLog.builder()
                .inspectionId(inspectionId)
                .action(action)
                .performedBy(performedBy != null ? performedBy : "System")
                .description(description)
                .build();
        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getHistoryForInspection(Long inspectionId) {
        return auditLogRepository.findByInspectionIdOrderByTimestampDesc(inspectionId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AuditLogResponseDto mapToDto(AuditLog auditLog) {
        return AuditLogResponseDto.builder()
                .id(auditLog.getId())
                .inspectionId(auditLog.getInspectionId())
                .action(auditLog.getAction())
                .performedBy(auditLog.getPerformedBy())
                .timestamp(auditLog.getTimestamp())
                .description(auditLog.getDescription())
                .build();
    }
}
