package com.eldercare.backend.medication.internal.dose.persistence;

import com.eldercare.backend.medication.internal.dose.domain.DoseReview;
import com.eldercare.backend.medication.internal.dose.domain.DoseReviewStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho {@link DoseReview} (Group 2).
 */
@Repository
public interface DoseReviewRepository extends JpaRepository<DoseReview, Long> {

    Optional<DoseReview> findByDoseResultId(Long doseResultId);

    List<DoseReview> findByDoseResultIdIn(Collection<Long> doseResultIds);

    List<DoseReview> findByStatus(DoseReviewStatus status);

    List<DoseReview> findByFloorAreaIdAndStatus(Long floorAreaId, DoseReviewStatus status);
}
