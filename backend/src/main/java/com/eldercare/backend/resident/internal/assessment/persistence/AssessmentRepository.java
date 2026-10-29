package com.eldercare.backend.resident.internal.assessment.persistence;

import com.eldercare.backend.resident.internal.assessment.domain.Assessment;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByResidentIdOrderByIdDesc(long residentId);

    Optional<Assessment> findFirstByResidentIdAndStatus(long residentId, AssessmentStatus status);

    boolean existsByResidentIdAndTypeAndStatus(long residentId, AssessmentType type, AssessmentStatus status);

    /** Lần Đã xác nhận gần nhất: thời điểm xác nhận, hòa thì lần lập sau (có tối đa một Nháp nên lập sau là xác nhận sau). */
    Optional<Assessment> findFirstByResidentIdAndStatusOrderByConfirmedAtDescIdDesc(
            long residentId, AssessmentStatus status);

    @Query("select a from Assessment a where a.residentId in :residentIds and a.status = :status"
            + " order by a.confirmedAt desc, a.id desc")
    List<Assessment> findByResidentIdInAndStatus(
            @Param("residentIds") Collection<Long> residentIds, @Param("status") AssessmentStatus status);
}
