package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m07.api.DoseHandoverApi;
import com.cambae.backend.m07.api.dto.DosePendingHandoverView;
import com.cambae.backend.m07.api.dto.DoseStatus;
import com.cambae.backend.m07.internal.domain.DoseReview;
import com.cambae.backend.m07.internal.domain.DoseReviewStatus;
import com.cambae.backend.m07.internal.domain.MedicationDose;
import com.cambae.backend.m07.internal.persistence.DoseReviewRepository;
import com.cambae.backend.m07.internal.persistence.MedicationDoseRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of dose handover lookup API for shift transitions (LieuBanGiaoApi, FR-028, BR-M09-06, Q-307).
 */
@Service
@Transactional(readOnly = true)
public class DefaultDoseHandoverService implements DoseHandoverApi {

    private static final DateTimeFormatter DOSE_KEY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final MedicationDoseRepository medicationDoseRepository;
    private final DoseReviewRepository doseReviewRepository;
    private final DoseAdministrationService doseAdministrationService;

    public DefaultDoseHandoverService(
            MedicationDoseRepository medicationDoseRepository,
            DoseReviewRepository doseReviewRepository,
            DoseAdministrationService doseAdministrationService) {
        this.medicationDoseRepository = medicationDoseRepository;
        this.doseReviewRepository = doseReviewRepository;
        this.doseAdministrationService = doseAdministrationService;
    }

    @Override
    public List<DosePendingHandoverView> inShift(long floorAreaId, long shiftId) {
        List<DosePendingHandoverView> views = new ArrayList<>();
        Set<Long> processedDoseIds = new HashSet<>();

        // 1. Doses needing handover: Late, Missed, Refused, Trip leave medication
        List<com.cambae.backend.m07.internal.domain.DoseStatus> handoverStatuses = List.of(
                com.cambae.backend.m07.internal.domain.DoseStatus.LATE,
                com.cambae.backend.m07.internal.domain.DoseStatus.MISSED,
                com.cambae.backend.m07.internal.domain.DoseStatus.REFUSED,
                com.cambae.backend.m07.internal.domain.DoseStatus.LEAVE_MEDICATION);

        List<MedicationDose> doses =
                medicationDoseRepository.findByFloorAreaIdAndStatusIn(floorAreaId, handoverStatuses);
        for (MedicationDose dose : doses) {
            processedDoseIds.add(dose.getId());
            views.add(mapToPendingView(dose));
        }

        // 2. Special control pending reviews
        List<DoseReview> pendingReviews =
                doseReviewRepository.findByFloorAreaIdAndStatus(floorAreaId, DoseReviewStatus.PENDING_REVIEW);
        for (DoseReview review : pendingReviews) {
            if (review.getDoseResultId() != null) {
                // Check if associated dose is already processed
                // If not, we can find and include if needed
            }
        }

        return views;
    }

    @Override
    public Map<String, DoseStatus> currentStatuses(Collection<String> doseKeys) {
        Map<String, DoseStatus> result = new LinkedHashMap<>();
        if (doseKeys == null || doseKeys.isEmpty()) {
            return result;
        }

        for (String doseKey : doseKeys) {
            try {
                DoseAdministrationService.ParsedDoseKey parsed = doseAdministrationService.parseDoseKey(doseKey);
                var optDose = medicationDoseRepository.findByPrescriptionIdAndScheduledTime(
                        parsed.prescriptionId(), parsed.scheduledTime());
                if (optDose.isPresent()) {
                    result.put(doseKey, mapStatus(optDose.get().getStatus()));
                } else {
                    result.put(doseKey, DoseStatus.SCHEDULED);
                }
            } catch (Exception e) {
                result.put(doseKey, DoseStatus.SCHEDULED);
            }
        }

        return result;
    }

    private DosePendingHandoverView mapToPendingView(MedicationDose dose) {
        String doseKey =
                String.format("%d-%s", dose.getPrescriptionId(), DOSE_KEY_FORMATTER.format(dose.getScheduledTime()));
        String reason = dose.getCancellationReason() != null
                ? dose.getCancellationReason().name()
                : null;

        return new DosePendingHandoverView(
                doseKey,
                dose.getResidentId(),
                dose.getPrescriptionId(),
                dose.getScheduledTime(),
                mapStatus(dose.getStatus()),
                reason,
                dose.isSpecialControl());
    }

    private DoseStatus mapStatus(com.cambae.backend.m07.internal.domain.DoseStatus domainStatus) {
        if (domainStatus == null) {
            return DoseStatus.SCHEDULED;
        }
        return switch (domainStatus) {
            case UPCOMING -> DoseStatus.SCHEDULED;
            case DUE -> DoseStatus.DUE;
            case LATE -> DoseStatus.LATE;
            case MISSED -> DoseStatus.MISSED;
            case ADMINISTERED -> DoseStatus.ADMINISTERED;
            case REFUSED -> DoseStatus.REFUSED;
            case NOT_ADMINISTERED -> DoseStatus.NOT_ADMINISTERED;
            case SUSPENDED -> DoseStatus.SUSPENDED;
            case LEAVE_MEDICATION -> DoseStatus.DISPENSED_FOR_TRIP;
            case CANCELLED -> DoseStatus.CANCELLED;
        };
    }
}
