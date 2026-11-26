package com.eldercare.backend.medication.internal.reconciliation.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.SuspensionReason;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.prescription.service.PrescriptionService;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationStatus;
import com.eldercare.backend.medication.internal.reconciliation.persistence.MedicationReconciliationRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách "Cần đối chiếu" (006 T075, T076): người cao tuổi trong phạm vi quyền xem đối chiếu thuốc của người dùng có
 * phiếu Mở, Chờ xác nhận hoặc có đơn đang tạm dừng chờ đối chiếu. Phiếu quá hạn trước, rồi theo hạn sớm nhất, chưa có
 * hạn, chưa lập phiếu sau cùng. Chỉ hiện số liệu đã có, không gợi ý (Q-306, Q-307).
 */
@Component
public class ReconciliationWorklist {

    private static final PermissionKey VIEW = new PermissionKey(Function.MEDICATION_RECONCILIATION, Operation.VIEW);
    private static final List<ReconciliationStatus> WORKING =
            List.of(ReconciliationStatus.OPEN, ReconciliationStatus.DRAFT, ReconciliationStatus.PENDING_CONFIRMATION);

    /** Một dòng của danh sách; phiếu để trống khi người đó chưa có phiếu đang làm. */
    public record Entry(
            long residentId,
            Long reconciliationId,
            String status,
            String reason,
            Instant deadlineAt,
            boolean overdue,
            int awaitingPrescriptionCount) {}

    private final MedicationReconciliationRepository tickets;
    private final PrescriptionRepository prescriptions;
    private final PermissionApi permissions;
    private final CurrentUser user;
    private final BusinessTime businessTime;

    public ReconciliationWorklist(
            MedicationReconciliationRepository tickets,
            PrescriptionRepository prescriptions,
            PermissionApi permissions,
            CurrentUser user,
            BusinessTime businessTime) {
        this.tickets = tickets;
        this.prescriptions = prescriptions;
        this.permissions = permissions;
        this.user = user;
        this.businessTime = businessTime;
    }

    @Transactional(readOnly = true)
    public List<Entry> list() {
        Instant now = businessTime.now();
        Map<Long, MedicationReconciliation> working = new HashMap<>();
        for (MedicationReconciliation t : tickets.findByStatusIn(WORKING)) {
            working.merge(t.getResidentId(), t, (a, b) -> a.getId() > b.getId() ? a : b);
        }
        Map<Long, Integer> awaiting = new HashMap<>();
        for (Prescription p : prescriptions.findByStatus(PrescriptionStatus.SUSPENDED)) {
            if (p.getSuspensionReason() == SuspensionReason.RECONCILIATION_PENDING) {
                awaiting.merge(p.getResidentId(), 1, Integer::sum);
            }
        }
        Set<Long> residents = new LinkedHashSet<>(working.keySet());
        residents.addAll(awaiting.keySet());

        return residents.stream()
                .filter(this::visible)
                .map(id -> entry(id, working.get(id), awaiting.getOrDefault(id, 0), now))
                .sorted(Comparator.comparing((Entry e) -> !e.overdue())
                        .thenComparing(e -> e.reconciliationId() == null)
                        .thenComparing(Entry::deadlineAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Entry::residentId))
                .toList();
    }

    private boolean visible(long residentId) {
        return permissions
                .hasPermission(user, VIEW, PrescriptionService.target(residentId))
                .allowed();
    }

    private static Entry entry(long residentId, MedicationReconciliation t, int awaiting, Instant now) {
        if (t == null) return new Entry(residentId, null, null, null, null, false, awaiting);
        boolean overdue = t.getOverdueAt() != null
                || (t.getDeadlineAt() != null && t.getDeadlineAt().isBefore(now));
        return new Entry(
                residentId,
                t.getId(),
                t.getStatus().name(),
                t.getReason().name(),
                t.getDeadlineAt(),
                overdue,
                awaiting);
    }
}
