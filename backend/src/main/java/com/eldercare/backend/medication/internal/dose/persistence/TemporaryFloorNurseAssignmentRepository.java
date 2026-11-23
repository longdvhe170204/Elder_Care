package com.eldercare.backend.medication.internal.dose.persistence;

import com.eldercare.backend.medication.internal.dose.domain.TemporaryFloorNurseAssignment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemporaryFloorNurseAssignmentRepository extends JpaRepository<TemporaryFloorNurseAssignment, Long> {

    @Query(
            "SELECT a FROM TemporaryFloorNurseAssignment a WHERE a.floorAreaId = :floorAreaId AND a.revokedAt IS NULL AND a.expiresAt > :now ORDER BY a.assignedAt")
    List<TemporaryFloorNurseAssignment> findActive(@Param("floorAreaId") Long floorAreaId, @Param("now") Instant now);
}
