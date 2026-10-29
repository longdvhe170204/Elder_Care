package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.domain.DoseResult;
import com.eldercare.backend.medication.internal.domain.DoseStatus;
import com.eldercare.backend.medication.internal.domain.FamilyMedicationBatch;
import com.eldercare.backend.medication.internal.domain.FamilyMedicationBatchStatus;
import com.eldercare.backend.medication.internal.domain.HandoverStatus;
import com.eldercare.backend.medication.internal.domain.MedicationHandoverDose;
import com.eldercare.backend.medication.internal.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.persistence.FamilyMedicationPrescriptionRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationHandoverDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.service.response.DoseDetailView;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    /**
     * Liều đã ghép. {@code pendingHandoverConfirmation} cần bàn giao ca của 008 ({@code CaTrucApi} chưa có) nên là
     * {@code null}: chưa xác định, không phải {@code false}.
     */
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

    public DoseViewService(
            DoseResultRepository results,
            MedicationHandoverDoseRepository handoverDoses,
            MedicationHandoverRepository handovers,
            FamilyMedicationBatchRepository batches,
            FamilyMedicationPrescriptionRepository batchLinks,
            AccountApi accounts,
            ResidentApi residents,
            BusinessTime businessTime) {
        this.results = results;
        this.handoverDoses = handoverDoses;
        this.handovers = handovers;
        this.batches = batches;
        this.batchLinks = batchLinks;
        this.accounts = accounts;
        this.residents = residents;
        this.businessTime = businessTime;
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
                d.status() == null ? null : d.status().name(),
                d.doseId() != null,
                null,
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
