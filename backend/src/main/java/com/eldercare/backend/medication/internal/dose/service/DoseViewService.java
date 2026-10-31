package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CurrentValue;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.medication.internal.dose.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.dose.domain.DoseDetailView;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.DoseTransitionTable;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatch;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatchStatus;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationPrescriptionRepository;
import com.eldercare.backend.medication.internal.handover.domain.HandoverStatus;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandoverDose;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverDoseRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liều theo api-006 {@code Dose} (FR-020, FR-028, FR-044, FR-048a, Edge Case qua đời): ghép liều tính từ đơn hoặc đã
 * vật hóa với lần giao, lô thuốc gửi dùng được, kết quả và người đóng. Chỉ đọc, không tạo bản ghi (Q-306).
 */
@Service
public class DoseViewService {

    /** Lô thuốc gia đình gửi Được sử dụng của đơn, chỉ để hiển thị; xác nhận liều không chọn lô (Q-393). */
    public record UsableBatch(long batchId, LocalDate expiryDate) {}

    /** Kết quả đã ghi của liều ({@code DoseResult} của api-006). */
    public record ResultView(
            Instant administeredAt,
            String basis,
            String reportedByType,
            Long reportedById,
            String reportedByName,
            String relationship,
            String adverseReaction,
            String reason,
            String note,
            boolean recordedAfterDisconnect) {}

    /** Liều đã ghép. */
    public record DoseView(
            DoseDetailView dose,
            String currentStatus,
            boolean materialized,
            Boolean pendingHandoverConfirmation,
            boolean awaitingRecordAfterReturn,
            boolean requiresOnline,
            List<UsableBatch> usableBatches,
            Long handoverId,
            ResultView result,
            String closedBy,
            boolean needsReview) {}

    private final DoseResultRepository results;
    private final MedicationHandoverDoseRepository handoverDoses;
    private final MedicationHandoverRepository handovers;
    private final FamilyMedicationBatchRepository batches;
    private final FamilyMedicationPrescriptionRepository batchLinks;
    private final AccountApi accounts;
    private final ResidentApi residents;
    private final BusinessTime businessTime;
    private final ResidentLocationApi locations;
    private final NursingShiftPort shifts;
    private final CorrectionApi corrections;

    public DoseViewService(
            DoseResultRepository results,
            MedicationHandoverDoseRepository handoverDoses,
            MedicationHandoverRepository handovers,
            FamilyMedicationBatchRepository batches,
            FamilyMedicationPrescriptionRepository batchLinks,
            AccountApi accounts,
            ResidentApi residents,
            BusinessTime businessTime,
            ResidentLocationApi locations,
            NursingShiftPort shifts,
            CorrectionApi corrections) {
        this.results = results;
        this.handoverDoses = handoverDoses;
        this.handovers = handovers;
        this.batches = batches;
        this.batchLinks = batchLinks;
        this.accounts = accounts;
        this.residents = residents;
        this.businessTime = businessTime;
        this.locations = locations;
        this.shifts = shifts;
        this.corrections = corrections;
    }

    @Transactional(readOnly = true)
    public DoseView view(DoseDetailView d) {
        Optional<DoseResult> result = d.doseId() == null ? Optional.empty() : results.findByDoseId(d.doseId());
        Optional<MedicationHandoverDose> link =
                d.doseId() == null ? Optional.empty() : handoverDoses.findByDoseId(d.doseId());
        Long handoverId = link.map(MedicationHandoverDose::getHandoverId).orElse(null);
        // Liều Mang theo mà người cao tuổi đã về (lần giao Chờ nhận lại), chưa ghi kết quả (FR-028, FR-048a)
        boolean awaitingRecord = d.status() == DoseStatus.LEAVE_MEDICATION
                && handoverId != null
                && handovers
                        .findById(handoverId)
                        .map(h -> h.getStatus() == HandoverStatus.WAITING_RETURN)
                        .orElse(false);
        return new DoseView(
                d,
                currentStatus(d, result.orElse(null)),
                d.doseId() != null,
                pendingHandoverConfirmation(d),
                awaitingRecord,
                d.specialControl(),
                usableBatches(d),
                handoverId,
                result.map(DoseViewService::resultView).orElse(null),
                result.flatMap(r -> Optional.ofNullable(r.getRecorderId()))
                        .flatMap(accounts::fullName)
                        .orElse(null),
                result.map(r -> afterDeath(d.residentId(), r.getAdministeredAt()))
                        .orElse(false));
    }

    private String currentStatus(DoseDetailView dose, DoseResult result) {
        String original = result != null && result.getResultType() != null
                ? result.getResultType().name()
                : dose.status() == null ? null : dose.status().name();
        if (result == null || result.getId() == null) {
            return original;
        }
        CurrentValue current = corrections
                .current(DoseResultOfflineHandler.RECORD_TYPE, List.of(result.getId()))
                .get(result.getId());
        if (current == null || !current.corrected() || current.voided()) {
            return original;
        }
        if (current.content() instanceof Map<?, ?> content && content.get("resultType") instanceof String value) {
            try {
                return com.eldercare.backend.medication.internal.dose.domain.DoseResultType.valueOf(value)
                        .name();
            } catch (IllegalArgumentException ignored) {
                return original;
            }
        }
        return original;
    }

    private boolean pendingHandoverConfirmation(DoseDetailView dose) {
        if (dose.status() == null
                || DoseTransitionTable.isTerminal(dose.status())
                || dose.residentId() == null
                || dose.scheduledTime() == null
                || dose.assignedNurseId() == null) {
            return false;
        }
        Instant now = businessTime.now();
        Long floorId = locations
                .at(dose.residentId(), dose.scheduledTime())
                .map(location -> location.floorId())
                .orElse(null);
        if (floorId == null) {
            return false;
        }
        return shifts.shiftAt(floorId, dose.scheduledTime())
                .filter(previousShift -> !now.isBefore(previousShift.endsAt()))
                .filter(previousShift -> !shifts.inProgressShiftMember(dose.assignedNurseId(), now))
                .map(previousShift -> !shifts.handoverConfirmed(previousShift.id()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<DoseView> views(List<DoseDetailView> doses) {
        return doses.stream().map(this::view).toList();
    }

    private List<UsableBatch> usableBatches(DoseDetailView d) {
        return d.prescriptionId() == null
                ? List.of()
                : usableBatches(d.residentId(), d.prescriptionId(), d.medicationSource());
    }

    /**
     * Lô Được sử dụng, chưa hết hạn, đang gắn với đơn (FR-044; không theo dõi số còn, Q-393); dùng cho liều và thuốc cần mang (FR-048a).
     */
    @Transactional(readOnly = true)
    public List<UsableBatch> usableBatches(Long residentId, long prescriptionId, MedicationSource source) {
        if (source != MedicationSource.FAMILY_SENT) {
            return List.of();
        }
        Set<Long> bound = batchLinks.findByPrescriptionIdAndBoundToIsNull(prescriptionId).stream()
                .map(l -> l.getBatchId())
                .collect(Collectors.toSet());
        LocalDate today = businessTime.today();
        return batches.findByResidentIdAndStatus(residentId, FamilyMedicationBatchStatus.IN_USE).stream()
                .filter(b -> bound.contains(b.getId()))
                .filter(b -> b.getExpiryDate() == null || !b.getExpiryDate().isBefore(today))
                .map(DoseViewService::usable)
                .toList();
    }

    private static UsableBatch usable(FamilyMedicationBatch b) {
        return new UsableBatch(b.getId(), b.getExpiryDate());
    }

    /** Thời điểm dùng sau thời điểm qua đời ghi trong quá khứ: liều chỉ mang dấu cần rà soát (Edge Case, Q-329). */
    private boolean afterDeath(Long residentId, Instant administeredAt) {
        if (residentId == null || administeredAt == null || residents.status(residentId) != ResidentStatus.DECEASED) {
            return false;
        }
        return residents
                .finalStatusAt(residentId)
                .map(died -> administeredAt.isAfter(died))
                .orElse(false);
    }

    private static ResultView resultView(DoseResult r) {
        String basis = r.getAuthorityBasis() == DoseAuthorityBasis.REPORTED_BY_OTHERS
                ? "REPORTED_BY_OTHERS"
                : r.getAuthorityBasis() == DoseAuthorityBasis.LEAVE_MEDICATION ? "LEAVE_MEDICATION" : "REGULAR";
        return new ResultView(
                r.getAdministeredAt(),
                basis,
                r.getReportedByType(),
                r.getReportedById(),
                r.getReportedByName(),
                r.getRelationship(),
                r.getAdverseReaction(),
                r.getReason(),
                r.getNote(),
                r.isOfflineFlag());
    }
}
