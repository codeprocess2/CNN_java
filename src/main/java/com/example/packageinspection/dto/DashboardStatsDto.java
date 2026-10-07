package com.example.packageinspection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    private long totalInspections;
    private long openIncidents;
    private long inProgressIncidents;
    private long escalatedIncidents;
    private long resolvedIncidents;
    private long highSeverityIncidents;
    private Double averageConfidence;
    private Double resolutionRatePercentage;
    private Map<String, Long> damageDistribution;
}
