package com.cambae.backend.m07.internal.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.offline.SyncOutcome;
import com.cambae.backend.m07.api.dto.AdministerDoseRequest;
import com.cambae.backend.m07.api.dto.DoseDetailView;
import com.cambae.backend.m07.api.dto.RefuseDoseRequest;
import com.cambae.backend.m07.api.dto.SkipDoseRequest;
import com.cambae.backend.m07.api.events.MedicationAlertRequestedEvent;
import com.cambae.backend.m07.internal.domain.DoseResult;
import com.cambae.backend.m07.internal.domain.DoseResultType;
import com.cambae.backend.m07.internal.domain.DoseReview;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.MedicationDose;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.persistence.DoseResultRepository;
import com.cambae.backend.m07.internal.persistence.DoseReviewRepository;
import com.cambae.backend.m07.internal.persistence.MedicationDoseRepository;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.OfflineHandler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Offline synchronization handler for {@code M07.KET_QUA_LIEU} (FR-029, BR-M07-20; research D-11).
 */
@Component
public class DoseResultOfflineHandler implements OfflineHandler<DoseResultOfflineContent> {

    public static final String RECORD_TYPE = "M07.KET_QUA_LIEU";

    private final DoseAdministrationService doseAdministrationService;
    private final MedicationDoseRepository doseRepository;
    private final DoseResultRepository doseResultRepository;
    private final DoseReviewRepository doseReviewRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicationRepository medicationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DoseResultOfflineHandler(
            DoseAdministrationService doseAdministrationService,
            MedicationDoseRepository doseRepository,
            DoseResultRepository doseResultRepository,
            DoseReviewRepository doseReviewRepository,
            PrescriptionRepository prescriptionRepository,
            MedicationRepository medicationRepository,
            ApplicationEventPublisher eventPublisher) {
        this.doseAdministrationService = doseAdministrationService;
        this.doseRepository = doseRepository;
        this.doseResultRepository = doseResultRepository;
        this.doseReviewRepository = doseReviewRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicationRepository = medicationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String recordType() {
        return RECORD_TYPE;
    }

    @Override
    public PermissionKey permissionKey() {
        return new PermissionKey(Function.MEDICATION_ADMINISTRATION, Operation.PROFESSIONAL);
    }

    @Override
    public Target target(DoseResultOfflineContent content) {
        return new Target("MEDICATION_DOSE", content.doseKey(), content.residentId(), null);
    }

    @Override
    public void validate(DoseResultOfflineContent content) {
        DoseAdministrationService.ParsedDoseKey parsed = doseAdministrationService.parseDoseKey(content.doseKey());
        Prescription prescription =
                prescriptionRepository.findById(parsed.prescriptionId()).orElse(null);
        if (prescription != null) {
            Medication med = medicationRepository
                    .findById(prescription.getMedicationId())
                    .orElse(null);
            if (med != null && med.isSpecialControl()) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "BAT_BUOC_TRUC_TUYEN",
                        "Special control medication requires online recording, cannot sync offline (BR-M07-20)");
            }
        }
    }

    @Override
    public String apply(DoseResultOfflineContent content, SyncOutcome outcome, Instant effectiveAt) {
        DoseAdministrationService.ParsedDoseKey parsed = doseAdministrationService.parseDoseKey(content.doseKey());
        Optional<MedicationDose> optDose =
                doseRepository.findByPrescriptionIdAndScheduledTime(parsed.prescriptionId(), parsed.scheduledTime());

        // Check if dose is already closed (two offline devices recorded the same dose, BR-M07-20)
        if (optDose.isPresent()) {
            MedicationDose dose = optDose.get();
            Optional<DoseResult> existingResult = doseResultRepository.findByDoseId(dose.getId());
            if (existingResult.isPresent()) {
                // Route conflicting duplicate to dose_review
                DoseReview review = new DoseReview(
                        existingResult.get().getId(),
                        null,
                        dose.getFloorAreaId(),
                        null,
                        "Offline record rejected due to duplicate dose with pre-existing record (BR-M07-20)");
                doseReviewRepository.save(review);

                eventPublisher.publishEvent(new MedicationAlertRequestedEvent(
                        "NGHI_DUNG_TRUNG_LIEU",
                        "TRUNG_BINH",
                        content.residentId(),
                        "doseKey:" + content.doseKey(),
                        "dup:" + content.doseKey(),
                        effectiveAt,
                        List.of()));

                return "dose_review:" + review.getId();
            }
        }

        // Apply normal administration, refusal, or skip
        DoseResultType type = content.resultType() != null ? content.resultType() : DoseResultType.ADMINISTERED;
        DoseDetailView view = switch (type) {
            case ADMINISTERED ->
                doseAdministrationService.administerDose(
                        content.doseKey(),
                        new AdministerDoseRequest(
                                content.administeredAt() != null ? content.administeredAt() : effectiveAt,
                                effectiveAt,
                                null,
                                null,
                                null,
                                null,
                                content.adverseReaction(),
                                content.note(),
                                true,
                                content.familyMedicationLotId(),
                                content.leaveHandoverId()),
                        content.recorderId(),
                        null,
                        effectiveAt);
            case REFUSED ->
                doseAdministrationService.refuseDose(
                        content.doseKey(),
                        new RefuseDoseRequest(content.reason(), content.note(), effectiveAt, true),
                        content.recorderId(),
                        null,
                        effectiveAt);
            case NOT_ADMINISTERED, MISSED ->
                doseAdministrationService.skipDose(
                        content.doseKey(),
                        new SkipDoseRequest(content.reason(), content.note(), effectiveAt, true),
                        content.recorderId(),
                        null,
                        effectiveAt);
        };

        return "dose:" + view.doseId();
    }
}
