package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.medication.internal.domain.DoseResult;
import com.eldercare.backend.medication.internal.domain.DoseReview;
import com.eldercare.backend.medication.internal.domain.DoseReviewStatus;
import com.eldercare.backend.medication.internal.domain.Medication;
import com.eldercare.backend.medication.internal.domain.MedicationDose;
import com.eldercare.backend.medication.internal.domain.PrnAdministration;
import com.eldercare.backend.medication.internal.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.persistence.PrnAdministrationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for dose reviews (FR-029, SC-012; research D-11).
 */
@Service
public class DoseReviewService {

    /**
     * Bản ghi xem lại kèm đủ để nhận ra liều hoặc lần dùng PRN và người ghi (api-006 {@code DoseReview}); người xem lại
     * phải khác người ghi (FR-029).
     */
    public record ReviewView(
            DoseReview review,
            String doseKey,
            Long residentId,
            String medicationName,
            Instant administeredAt,
            String recordedBy,
            String reviewedBy) {}

    private final DoseReviewRepository doseReviewRepository;
    private final DoseResultRepository doseResultRepository;
    private final MedicationDoseRepository doseRepository;
    private final PrnAdministrationRepository prnRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicationRepository medicationRepository;
    private final ActorNames names;

    public DoseReviewService(
            DoseReviewRepository doseReviewRepository,
            DoseResultRepository doseResultRepository,
            MedicationDoseRepository doseRepository,
            PrnAdministrationRepository prnRepository,
            PrescriptionRepository prescriptionRepository,
            MedicationRepository medicationRepository,
            ActorNames names) {
        this.doseReviewRepository = doseReviewRepository;
        this.doseResultRepository = doseResultRepository;
        this.doseRepository = doseRepository;
        this.prnRepository = prnRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicationRepository = medicationRepository;
        this.names = names;
    }

    @Transactional(readOnly = true)
    public List<DoseReview> listPendingReviews(Long floorAreaId) {
        if (floorAreaId != null) {
            return doseReviewRepository.findByFloorAreaIdAndStatus(floorAreaId, DoseReviewStatus.PENDING_REVIEW);
        }
        return doseReviewRepository.findByStatus(DoseReviewStatus.PENDING_REVIEW);
    }

    /** Bản ghi xem lại theo trạng thái (api-006 tham số {@code status}); không truyền thì Chờ xem lại (FR-029). */
    @Transactional(readOnly = true)
    public List<ReviewView> listReviews(DoseReviewStatus status) {
        return doseReviewRepository.findByStatus(status != null ? status : DoseReviewStatus.PENDING_REVIEW).stream()
                .map(this::view)
                .toList();
    }

    private ReviewView view(DoseReview r) {
        String reviewedBy = names.of(r.getReviewerId());
        if (r.getDoseResultId() != null) {
            Optional<DoseResult> result = doseResultRepository.findById(r.getDoseResultId());
            Optional<MedicationDose> dose = result.flatMap(x -> doseRepository.findById(x.getDoseId()));
            return new ReviewView(
                    r,
                    dose.map(d -> MedicationScheduleCalculator.doseKey(d.getPrescriptionId(), d.getScheduledTime()))
                            .orElse(null),
                    result.map(DoseResult::getResidentId).orElse(null),
                    dose.flatMap(d -> medicationName(d.getMedicationId())).orElse(null),
                    result.map(DoseResult::getAdministeredAt).orElse(null),
                    result.map(x -> names.of(x.getRecorderId())).orElse(null),
                    reviewedBy);
        }
        Optional<PrnAdministration> prn =
                r.getPrnUsageId() == null ? Optional.empty() : prnRepository.findById(r.getPrnUsageId());
        return new ReviewView(
                r,
                null,
                prn.map(PrnAdministration::getResidentId).orElse(null),
                prn.flatMap(p -> prescriptionRepository.findById(p.getPrescriptionId()))
                        .flatMap(p -> medicationName(p.getMedicationId()))
                        .orElse(null),
                prn.map(PrnAdministration::getAdministeredAt).orElse(null),
                prn.map(p -> names.of(p.getAdministeredById())).orElse(null),
                reviewedBy);
    }

    private Optional<String> medicationName(Long medicationId) {
        return medicationId == null
                ? Optional.empty()
                : medicationRepository.findById(medicationId).map(Medication::getName);
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
                        "SELF_REVIEW_NOT_ALLOWED",
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
