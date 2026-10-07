package com.example.packageinspection.service;

import com.example.packageinspection.dto.InspectionResponseDto;
import com.example.packageinspection.entity.*;
import com.example.packageinspection.exception.InvalidStatusTransitionException;
import com.example.packageinspection.exception.ResourceNotFoundException;
import com.example.packageinspection.ml.ClassificationResult;
import com.example.packageinspection.ml.ImageClassificationService;
import com.example.packageinspection.repository.InspectionRepository;
import com.example.packageinspection.util.InspectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final ImageClassificationService imageClassificationService;
    private final FileStorageService fileStorageService;
    private final SeverityCalculationService severityCalculationService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final InspectionMapper mapper;

    @Transactional
    public InspectionResponseDto createInspection(MultipartFile image, String userDescription) {
        String path = fileStorageService.storeFile(image);
        ClassificationResult res = imageClassificationService.classify(image);

        Severity severity = severityCalculationService.calculateSeverity(res.getPredictedClass(), res.getConfidence());
        InspectionStatus status = severityCalculationService.determineInitialStatus(res.getConfidence());

        String desc = (userDescription != null && !userDescription.isBlank()) ? userDescription :
                "AI Prediction: " + res.getPredictedClass() + " (" + Math.round(res.getConfidence() * 100) + "% confidence). " + res.getDetails();

        Inspection entity = Inspection.builder()
                .imagePath(path)
                .predictedClass(res.getPredictedClass())
                .confidence(res.getConfidence())
                .severity(severity)
                .status(status)
                .description(desc)
                .build();

        Inspection saved = inspectionRepository.save(entity);
        auditLogService.logAction(saved.getId(), AuditAction.CREATED, "System", "Created with CNN class: " + res.getPredictedClass());

        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public InspectionResponseDto getInspectionById(Long id) {
        return mapper.toDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<InspectionResponseDto> getAllInspections(InspectionStatus status, Severity severity, DamageType predictedClass) {
        Specification<Inspection> spec = Specification.where(null);
        if (status != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (severity != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("severity"), severity));
        if (predictedClass != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("predictedClass"), predictedClass));

        return inspectionRepository.findAll(spec).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public InspectionResponseDto assignInspection(Long id, Long userId, String performedBy) {
        Inspection insp = findEntityById(id);
        User user = userService.getUserEntity(userId);
        if (insp.getStatus() == InspectionStatus.RESOLVED || insp.getStatus() == InspectionStatus.REJECTED) {
            throw new InvalidStatusTransitionException("Cannot assign closed inspection");
        }
        insp.setAssignedTo(user);
        if (insp.getStatus() == InspectionStatus.OPEN || insp.getStatus() == InspectionStatus.NEEDS_REVIEW) {
            insp.setStatus(InspectionStatus.ASSIGNED);
        }
        auditLogService.logAction(id, AuditAction.ASSIGNED, performedBy != null ? performedBy : "System", "Assigned to " + user.getName());
        return mapper.toDto(inspectionRepository.save(insp));
    }

    @Transactional
    public InspectionResponseDto escalateInspection(Long id, String performedBy) {
        Inspection insp = findEntityById(id);
        if (insp.getStatus() == InspectionStatus.RESOLVED || insp.getStatus() == InspectionStatus.REJECTED) {
            throw new InvalidStatusTransitionException("Cannot escalate closed inspection");
        }
        insp.setStatus(InspectionStatus.ESCALATED);
        insp.setSeverity(Severity.HIGH);
        auditLogService.logAction(id, AuditAction.ESCALATED, performedBy != null ? performedBy : "Supervisor", "Escalated to HIGH");
        return mapper.toDto(inspectionRepository.save(insp));
    }

    @Transactional
    public InspectionResponseDto resolveInspection(Long id, String performedBy) {
        Inspection insp = findEntityById(id);
        if (insp.getStatus() == InspectionStatus.RESOLVED) {
            throw new InvalidStatusTransitionException("Inspection already RESOLVED");
        }
        insp.setStatus(InspectionStatus.RESOLVED);
        insp.setResolvedAt(OffsetDateTime.now());
        auditLogService.logAction(id, AuditAction.RESOLVED, performedBy != null ? performedBy : "Inspector", "Inspection resolved");
        return mapper.toDto(inspectionRepository.save(insp));
    }

    public Inspection findEntityById(Long id) {
        return inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection with ID " + id + " does not exist"));
    }
}
