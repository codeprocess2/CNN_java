package com.example.packageinspection.repository;

import com.example.packageinspection.entity.DamageType;
import com.example.packageinspection.entity.Inspection;
import com.example.packageinspection.entity.InspectionStatus;
import com.example.packageinspection.entity.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long>, JpaSpecificationExecutor<Inspection> {

    List<Inspection> findByStatus(InspectionStatus status);

    List<Inspection> findBySeverity(Severity severity);

    List<Inspection> findByPredictedClass(DamageType predictedClass);

    long countByStatus(InspectionStatus status);

    long countBySeverity(Severity severity);

    long countByPredictedClass(DamageType predictedClass);

    @Query("SELECT AVG(i.confidence) FROM Inspection i")
    Double findAverageConfidence();

    @Query("SELECT i.predictedClass, COUNT(i) FROM Inspection i GROUP BY i.predictedClass")
    List<Object[]> countGroupedByPredictedClass();
}
