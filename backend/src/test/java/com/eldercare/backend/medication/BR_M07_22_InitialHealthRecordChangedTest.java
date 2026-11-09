package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.events.AdmissionMedicationChanged;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationReason;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationStatus;
import com.eldercare.backend.medication.internal.reconciliation.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.medication.internal.reconciliation.service.InitialHealthRecordListener;
import com.eldercare.backend.resident.api.events.InitialHealthRecordChanged;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * BR-M07-22, FR-034a (research D, quickstart Q25): hồ sơ sức khỏe ban đầu đổi sau khi lập phiếu đối chiếu "tiếp nhận"
 * thì báo người thực hiện (phiếu Mở), hoặc người thực hiện và người xác nhận (Chờ xác nhận, Đã xác nhận); phiếu không đổi.
 */
class BR_M07_22_InitialHealthRecordChangedTest {

    private static final Instant NOW = Instant.parse("2026-11-08T03:00:00Z");

    private final MedicationReconciliationRepository tickets = mock(MedicationReconciliationRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final InitialHealthRecordListener listener = new InitialHealthRecordListener(tickets, events, time);

    private MedicationReconciliation ticket(
            ReconciliationStatus status, Long performer, Long submitter, Long confirmer) {
        MedicationReconciliation ticket = mock(MedicationReconciliation.class);
        when(ticket.getId()).thenReturn(5L);
        when(ticket.getReason()).thenReturn(ReconciliationReason.ADMISSION);
        when(ticket.getStatus()).thenReturn(status);
        when(ticket.getCreatedById()).thenReturn(performer);
        when(ticket.getSubmittedById()).thenReturn(submitter);
        when(ticket.getConfirmedById()).thenReturn(confirmer);
        return ticket;
    }

    private List<Long> notifiedStaff() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        var event = (AdmissionMedicationChanged) captor.getValue();
        return ((HasNotificationRequests) event)
                .notificationRequests().getFirst().recipientGroups().stream()
                        .map(group -> ((RecipientGroup.Staff) group).staffId())
                        .toList();
    }

    @Test
    void BR_M07_22_openTicketNotifiesThePerformerOnly() {
        when(time.now()).thenReturn(NOW);
        var open = ticket(ReconciliationStatus.OPEN, 11L, null, null);
        when(tickets.findByResidentId(9L)).thenReturn(List.of(open));

        listener.onInitialHealthRecordChanged(new InitialHealthRecordChanged(9L));

        assertThat(notifiedStaff()).containsExactly(11L);
    }

    @Test
    void BR_M07_22_pendingAndConfirmedTicketsNotifyPerformerAndConfirmer() {
        when(time.now()).thenReturn(NOW);
        var confirmed = ticket(ReconciliationStatus.CONFIRMED, 11L, 12L, 13L);
        when(tickets.findByResidentId(9L)).thenReturn(List.of(confirmed));

        listener.onInitialHealthRecordChanged(new InitialHealthRecordChanged(9L));

        assertThat(notifiedStaff()).containsExactlyInAnyOrder(11L, 12L, 13L);
    }

    @Test
    void BR_M07_22_noAdmissionTicketMeansNoNotice() {
        when(tickets.findByResidentId(9L)).thenReturn(List.of());

        listener.onInitialHealthRecordChanged(new InitialHealthRecordChanged(9L));

        verify(events, never()).publishEvent(any(Object.class));
    }
}
