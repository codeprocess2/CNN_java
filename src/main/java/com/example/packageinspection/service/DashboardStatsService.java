package com.example.packageinspection.service;

import com.example.packageinspection.dto.DashboardStatsDto;
import com.example.packageinspection.entity.DamageType;
import com.example.packageinspection.entity.InspectionStatus;
import com.example.packageinspection.entity.Severity;
import com.example.packageinspection.repository.InspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final InspectionRepository inspectionRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long total = inspectionRepository.count();
        long open = inspectionRepository.countByStatus(InspectionStatus.OPEN);
        long inProgress = inspectionRepository.countByStatus(InspectionStatus.IN_PROGRESS) + inspectionRepository.countByStatus(InspectionStatus.ASSIGNED);
        long escalated = inspectionRepository.countByStatus(InspectionStatus.ESCALATED);
        long resolved = inspectionRepository.countByStatus(InspectionStatus.RESOLVED);
        long highSeverity = inspectionRepository.countBySeverity(Severity.HIGH);

        Double avgConf = inspectionRepository.findAverageConfidence();
        avgConf = avgConf != null ? Math.round(avgConf * 100.0) / 100.0 : 0.0;
        double resolutionRate = total > 0 ? Math.round(((double) resolved / total * 100.0) * 100.0) / 100.0 : 0.0;

        Map<String, Long> damageDistribution = new HashMap<>();
        for (DamageType type : DamageType.values()) {
            damageDistribution.put(type.name(), inspectionRepository.countByPredictedClass(type));
        }

        return DashboardStatsDto.builder()
                .totalInspections(total)
                .openIncidents(open)
                .inProgressIncidents(inProgress)
                .escalatedIncidents(escalated)
                .resolvedIncidents(resolved)
                .highSeverityIncidents(highSeverity)
                .averageConfidence(avgConf)
                .resolutionRatePercentage(resolutionRate)
                .damageDistribution(damageDistribution)
                .build();
    }
}
