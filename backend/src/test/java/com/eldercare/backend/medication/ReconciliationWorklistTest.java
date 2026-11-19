package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.SuspensionReason;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationReason;
import com.eldercare.backend.medication.internal.reconciliation.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationWorklist;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Danh sách "Cần đối chiếu" (006 T076, T075): người cao tuổi trong phạm vi quyền xem đối chiếu thuốc có phiếu Mở, Chờ
 * xác nhận hoặc có đơn tạm dừng chờ đối chiếu; phiếu quá hạn trước, rồi theo hạn sớm nhất. Chỉ hiện số liệu đã có (Q-306).
 */
class ReconciliationWorklistTest {

    private static final Instant NOW = Instant.parse("2026-11-19T03:00:00Z");

    private static void setId(Object entity, Long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MedicationReconciliation ticket(long id, long residentId, Instant deadline) {
        MedicationReconciliation t = new MedicationReconciliation(
                residentId, ReconciliationReason.HOSPITAL_RETURN, null, deadline, 10L, NOW, NOW);
        setId(t, id);
        return t;
    }

    private static Prescription awaiting(long id, long residentId) {
        Prescription p = mock(Prescription.class);
        when(p.getId()).thenReturn(id);
        when(p.getResidentId()).thenReturn(residentId);
        when(p.getStatus()).thenReturn(PrescriptionStatus.SUSPENDED);
        when(p.getSuspensionReason()).thenReturn(SuspensionReason.RECONCILIATION_PENDING);
        return p;
    }

    @Test
    @DisplayName(
            "T076: lọc theo phạm vi, đếm đơn chờ đối chiếu, phiếu quá hạn trước rồi theo hạn, chưa lập phiếu sau cùng")
    void list_scopedAndOrdered() {
        MedicationReconciliationRepository tickets = mock(MedicationReconciliationRepository.class);
        PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
        PermissionApi permissions = mock(PermissionApi.class);
        CurrentUser user = mock(CurrentUser.class);

        MedicationReconciliation later = ticket(1L, 1L, NOW.plusSeconds(3 * 3600));
        MedicationReconciliation overdue = ticket(2L, 2L, NOW.minusSeconds(600));
        MedicationReconciliation outOfScope = ticket(4L, 4L, NOW.minusSeconds(600));
        MedicationReconciliation professional = new MedicationReconciliation(
                5L, ReconciliationReason.PROFESSIONAL_REQUEST, "Đổi phác đồ", null, 10L, NOW, NOW);
        setId(professional, 5L);
        professional.submit(10L, NOW);
        when(tickets.findByStatusIn(anyCollection())).thenReturn(List.of(later, overdue, outOfScope, professional));
        List<Prescription> suspended =
                List.of(awaiting(71L, 1L), awaiting(72L, 1L), awaiting(73L, 3L), awaiting(74L, 4L));
        when(prescriptions.findByStatus(PrescriptionStatus.SUSPENDED)).thenReturn(suspended);
        when(permissions.hasPermission(any(), any(), any())).thenAnswer(inv -> {
            Target target = inv.getArgument(2);
            return target.residentId() == 4L
                    ? PermissionResult.denied(FailedLayer.SCOPE)
                    : PermissionResult.allowed(mock(PermissionBasis.class));
        });

        var worklist = new ReconciliationWorklist(
                tickets, prescriptions, permissions, user, new BusinessTime(Clock.fixed(NOW, ZoneOffset.UTC)));
        var entries = worklist.list();

        assertThat(entries).extracting(ReconciliationWorklist.Entry::residentId).containsExactly(2L, 1L, 5L, 3L);
        var overdueEntry = entries.get(0);
        assertThat(overdueEntry.overdue()).isTrue();
        assertThat(overdueEntry.reconciliationId()).isEqualTo(2L);
        assertThat(overdueEntry.status()).isEqualTo("OPEN");
        assertThat(entries.get(1).awaitingPrescriptionCount()).isEqualTo(2);
        assertThat(entries.get(2).status()).isEqualTo("PENDING_CONFIRMATION");
        var noTicket = entries.get(3);
        assertThat(noTicket.reconciliationId()).isNull();
        assertThat(noTicket.status()).isNull();
        assertThat(noTicket.awaitingPrescriptionCount()).isEqualTo(1);
    }
}
