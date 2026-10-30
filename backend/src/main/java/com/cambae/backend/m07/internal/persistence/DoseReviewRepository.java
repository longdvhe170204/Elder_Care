package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.DoseReview;
import com.cambae.backend.m07.internal.domain.DoseReviewStatus;
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

    List<DoseReview> findByStatus(DoseReviewStatus status);

    List<DoseReview> findByFloorAreaIdAndStatus(Long floorAreaId, DoseReviewStatus status);
}
