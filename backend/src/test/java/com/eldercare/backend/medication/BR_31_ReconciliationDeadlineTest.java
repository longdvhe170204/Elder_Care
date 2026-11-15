package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.events.ReconciliationNeeded;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyType;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.domain.SuspensionReason;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationReason;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationStatus;
import com.eldercare.backend.medication.internal.reconciliation.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ReconciliationDueStore;
import com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationDeadlines;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * BR-M07-09, FR-034, FR-034a, FR-035 (research D-13): Hoàn tất tiếp nhận hoặc Ghi nhận trở về từ bệnh viện tạm dừng đơn
 * chờ đối chiếu và đặt hạn CFG-M07-05; đã có phiếu tiếp nhận xác nhận thì chỉ cho đơn "chờ tiếp nhận" Hiệu lực; tới hạn
 * chưa đối chiếu thì yêu cầu cảnh báo một lần.
 */
class BR_31_ReconciliationDeadlineTest {

    private static final long RESIDENT = 9L;
    private static final Instant AT = Instant.parse("2026-11-08T02:00:00Z");

    private final PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
    private final MedicationDoseRepository doses = mock(MedicationDoseRepository.class);
    private final MedicationReconciliationRepository tickets = mock(MedicationReconciliationRepository.class);
    private final ReconciliationDueStore due = mock(ReconciliationDueStore.class);
    private final ParameterApi parameters = mock(ParameterApi.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final ReconciliationDeadlines deadlines =
            new ReconciliationDeadlines(prescriptions, doses, tickets, due, parameters, events, time);

    private static void setField(Object target, Class<?> type, String name, Object value) {
        try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Prescription prescription(long id, PrescriptionStatus status, boolean pendingAdmission) {
        Prescription p = new Prescription(
                RESIDENT,
                100L,
                new BigDecimal("1.00"),
                "viên",
                "uống",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\"]",
                null,
                null,
                null,
                null,
                null,
                AT.minus(10, ChronoUnit.DAYS),
                null,
                MedicationSource.FACILITY,
                PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                null,
                status,
                pendingAdmission,
                null,
                null,
                null,
                AT,
                Set.of(),
                AT);
        setField(p, Prescription.class, "id", id);
        return p;
    }

    private static MedicationReconciliation ticket(ReconciliationReason reason, ReconciliationStatus status) {
        MedicationReconciliation t = mock(MedicationReconciliation.class);
        when(t.getReason()).thenReturn(reason);
        when(t.getStatus()).thenReturn(status);
        return t;
    }

    @Test
    void BR_31_returnFromHospitalSuspendsPrescriptionsSetsDeadlineAndNotifies() {
        when(time.now()).thenReturn(AT);
        ParameterValue window = mock(ParameterValue.class);
        when(window.asDuration()).thenReturn(Duration.ofHours(24));
        when(parameters.read(ParameterCode.CFG_M07_05)).thenReturn(window);
        Prescription active = prescription(1L, PrescriptionStatus.ACTIVE, false);
        when(prescriptions.findByResidentIdAndStatusIn(eq(RESIDENT), anyCollection()))
                .thenReturn(List.of(active));
        MedicationDose upcoming = new MedicationDose(
                1L,
                RESIDENT,
                100L,
                AT.plus(3, ChronoUnit.HOURS),
                AT.plus(2, ChronoUnit.HOURS),
                AT.plus(4, ChronoUnit.HOURS),
                new BigDecimal("1.00"),
                "viên",
                MedicationSource.FACILITY,
                false,
                DoseStatus.UPCOMING,
                3L,
                AT);
        when(doses.findByPrescriptionIdAndStatusIn(eq(1L), anyCollection())).thenReturn(List.of(upcoming));
        MedicationReconciliation open = ticket(ReconciliationReason.HOSPITAL_RETURN, ReconciliationStatus.OPEN);
        when(tickets.findByResidentIdAndStatusIn(eq(RESIDENT), anyCollection())).thenReturn(List.of(open));

        deadlines.begin(RESIDENT, ReconciliationReason.HOSPITAL_RETURN, AT);

        assertThat(active.getStatus()).isEqualTo(PrescriptionStatus.SUSPENDED);
        assertThat(active.getSuspensionReason()).isEqualTo(SuspensionReason.RECONCILIATION_PENDING);
        assertThat(upcoming.getStatus()).isEqualTo(DoseStatus.SUSPENDED);
        verify(due).start(RESIDENT, AT.plus(24, ChronoUnit.HOURS), ReconciliationReason.HOSPITAL_RETURN.name());
        verify(open).assignDeadline(AT.plus(24, ChronoUnit.HOURS));
        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());
        assertThat(published.getValue()).isInstanceOf(ReconciliationNeeded.class);
    }

    @Test
    void FR_034a_noConfirmedAdmissionTicketBehavesLikeAHospitalReturn() {
        when(time.now()).thenReturn(AT);
        ParameterValue window = mock(ParameterValue.class);
        when(window.asDuration()).thenReturn(Duration.ofHours(24));
        when(parameters.read(ParameterCode.CFG_M07_05)).thenReturn(window);
        when(tickets.findByResidentId(RESIDENT)).thenReturn(List.of());
        Prescription waiting = prescription(2L, PrescriptionStatus.PENDING_EFFECTIVE, true);
        when(prescriptions.findByResidentIdAndStatusIn(eq(RESIDENT), anyCollection()))
                .thenReturn(List.of(waiting));

        deadlines.completeAdmission(RESIDENT, AT);

        assertThat(waiting.getStatus()).isEqualTo(PrescriptionStatus.SUSPENDED);
        verify(due).start(RESIDENT, AT.plus(24, ChronoUnit.HOURS), ReconciliationReason.ADMISSION.name());
    }

    @Test
    void FR_036_confirmedTicketClearsTheDeadlineAndResumesSuspendedDoses() {
        when(time.now()).thenReturn(AT);
        Prescription resumed = prescription(1L, PrescriptionStatus.ACTIVE, false);
        when(prescriptions.findByResidentIdAndStatusIn(eq(RESIDENT), anyList())).thenReturn(List.of(resumed));
        MedicationDose suspended = new MedicationDose(
                1L,
                RESIDENT,
                100L,
                AT.plus(3, ChronoUnit.HOURS),
                AT.plus(2, ChronoUnit.HOURS),
                AT.plus(4, ChronoUnit.HOURS),
                new BigDecimal("1.00"),
                "viên",
                MedicationSource.FACILITY,
                false,
                DoseStatus.SUSPENDED,
                3L,
                AT);
        when(doses.findByPrescriptionIdAndStatusIn(eq(1L), anyCollection())).thenReturn(List.of(suspended));

        deadlines.complete(RESIDENT);

        verify(due).clear(RESIDENT);
        verify(events)
                .publishEvent(new com.eldercare.backend.medication.api.events.ReconciliationCompleted(RESIDENT, AT));
        assertThat(suspended.getStatus()).isEqualTo(DoseStatus.UPCOMING);
    }
}
