package com.example.packageinspection.controller;

import com.example.packageinspection.dto.AuditLogResponseDto;
import com.example.packageinspection.dto.InspectionResponseDto;
import com.example.packageinspection.entity.DamageType;
import com.example.packageinspection.entity.InspectionStatus;
import com.example.packageinspection.entity.Severity;
import com.example.packageinspection.service.AuditLogService;
import com.example.packageinspection.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;
    private final AuditLogService auditLogService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InspectionResponseDto> createInspection(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        InspectionResponseDto dto = inspectionService.createInspection(file, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionResponseDto> getInspectionById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(inspectionService.getInspectionById(id));
    }

    @GetMapping
    public ResponseEntity<List<InspectionResponseDto>> getAllInspections(
            @RequestParam(value = "status", required = false) InspectionStatus status,
            @RequestParam(value = "severity", required = false) Severity severity,
            @RequestParam(value = "predictedClass", required = false) DamageType predictedClass) {
        return ResponseEntity.ok(inspectionService.getAllInspections(status, severity, predictedClass));
    }

    @PutMapping("/{id}/assign/{userId}")
    public ResponseEntity<InspectionResponseDto> assignInspection(
            @PathVariable("id") Long id,
            @PathVariable("userId") Long userId,
            @RequestParam(value = "performedBy", required = false) String performedBy) {
        return ResponseEntity.ok(inspectionService.assignInspection(id, userId, performedBy));
    }

    @PutMapping("/{id}/escalate")
    public ResponseEntity<InspectionResponseDto> escalateInspection(
            @PathVariable("id") Long id,
            @RequestParam(value = "performedBy", required = false) String performedBy) {
        return ResponseEntity.ok(inspectionService.escalateInspection(id, performedBy));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<InspectionResponseDto> resolveInspection(
            @PathVariable("id") Long id,
            @RequestParam(value = "performedBy", required = false) String performedBy) {
        return ResponseEntity.ok(inspectionService.resolveInspection(id, performedBy));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<AuditLogResponseDto>> getInspectionHistory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(auditLogService.getHistoryForInspection(id));
    }
}
