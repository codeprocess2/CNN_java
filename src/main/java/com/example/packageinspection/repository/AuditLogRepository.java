package com.example.packageinspection.repository;

import com.example.packageinspection.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByInspectionIdOrderByTimestampDesc(Long inspectionId);
}
