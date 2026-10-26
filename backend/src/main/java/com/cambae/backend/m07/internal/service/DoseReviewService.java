package com.cambae.backend.m07.internal.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m07.internal.domain.DoseResult;
import com.cambae.backend.m07.internal.domain.DoseReview;
import com.cambae.backend.m07.internal.domain.DoseReviewStatus;
import com.cambae.backend.m07.internal.persistence.DoseResultRepository;
import com.cambae.backend.m07.internal.persistence.DoseReviewRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for dose reviews (FR-029, SC-012; research D-11).
 */
@Service
public class DoseReviewService {

    private final DoseReviewRepository doseReviewRepository;
    private final DoseResultRepository doseResultRepository;

    public DoseReviewService(DoseReviewRepository doseReviewRepository, DoseResultRepository doseResultRepository) {
        this.doseReviewRepository = doseReviewRepository;
        this.doseResultRepository = doseResultRepository;
    }

    @Transactional(readOnly = true)
    public List<DoseReview> listPendingReviews(Long floorAreaId) {
        if (floorAreaId != null) {
            return doseReviewRepository.findByFloorAreaIdAndStatus(floorAreaId, DoseReviewStatus.PENDING_REVIEW);
        }
        return doseReviewRepository.findByStatus(DoseReviewStatus.PENDING_REVIEW);
    }

    @Transactional
    public DoseReview reviewDose(Long reviewId, boolean accept, String note, Long reviewerId, Instant now) {
        DoseReview review = doseReviewRepository.findById(reviewId).orElseThrow(NotFoundException::new);

        if (review.getStatus() != DoseReviewStatus.PENDING_REVIEW) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "DA_XEM_LAI",
                    "Dose review record has already been processed (status: " + review.getStatus() + ")");
        }

        // Validate two-person rule (SC-012): reviewer cannot be the recorder of the dose result
        if (review.getDoseResultId() != null) {
            DoseResult doseResult =
                    doseResultRepository.findById(review.getDoseResultId()).orElse(null);
            if (doseResult != null && reviewerId != null && reviewerId.equals(doseResult.getRecorderId())) {
                throw new BusinessException(
                        HttpStatus.FORBIDDEN,
                        "TU_XEM_LAI_BI_CHAN",
                        "Reviewer cannot be the same person who recorded the dose result (SC-012, FR-029)");
            }
        }

        if (accept) {
            review.accept(reviewerId, note, now);
        } else {
            review.requireCorrection(reviewerId, note, now);
        }

        return doseReviewRepository.save(review);
    }
}
