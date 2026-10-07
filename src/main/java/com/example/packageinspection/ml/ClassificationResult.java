package com.example.packageinspection.ml;

import com.example.packageinspection.entity.DamageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassificationResult {
    private DamageType predictedClass;
    private double confidence;
    private boolean isMockModel;
    private String details;
}
