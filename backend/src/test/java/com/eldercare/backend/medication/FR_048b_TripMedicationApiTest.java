package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.TimeInterval;
import com.eldercare.backend.medication.api.dto.TripChangeType;
import com.eldercare.backend.medication.api.events.CarriedMedicationReturnNeeded;
import com.eldercare.backend.medication.api.spi.TripMedicationPort;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.handover.domain.AbsenceIntervalType;
import com.eldercare.backend.medication.internal.handover.domain.HandoverStatus;
import com.eldercare.backend.medication.internal.handover.domain.MedicationAbsenceInterval;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandover;
import com.eldercare.backend.medication.internal.handover.domain.NoMedicationOrderBody;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationAbsenceIntervalRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverDoseRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverItemRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationReturnRepository;
import com.eldercare.backend.medication.internal.handover.persistence.NoMedicationOrderRepository;
import com.eldercare.backend.medication.internal.handover.service.DefaultTripMedicationApi;
import com.eldercare.backend.medication.internal.handover.service.MedicationHandoverService;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.internal.schedule.service.CalculatedDoseSlot;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * FR-048b, Q-58, Q-171 (research D-15): 014 hỏi thuốc mang theo đã chuẩn bị chưa trước khi cho điểm danh rời viện, báo
 * khi chuyến đổi để nhắc Nhận lại, và lệnh "Không mang thuốc" chỉ Tạm dừng liều trong khoảng đi đã duyệt.
 */
class FR_048b_TripMedicationApiTest {

    private static final long RESIDENT = 9L;
    private static final long TRIP = 77L;
    private static final Instant NOW = Instant.parse("2026-11-08T01:00:00Z");
    private static final TimeInterval TRIP_INTERVAL =
            new TimeInterval(Instant.parse("2026-11-09T00:00:00Z"), Instant.parse("2026-11-09T06:00:00Z"));

    private final MedicationHandoverRepository handovers = mock(MedicationHandoverRepository.class);
    private final NoMedicationOrderRepository orders = mock(NoMedicationOrderRepository.class);
    private final MedicationAbsenceIntervalRepository absences = mock(MedicationAbsenceIntervalRepository.class);
    private final PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
    private final MedicationRepository medications = mock(MedicationRepository.class);
    private final MedicationScheduleCalculator calculator = mock(MedicationScheduleCalculator.class);
    private final TripMedicationPort port = mock(TripMedicationPort.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BusinessTime time = mock(BusinessTime.class);

    private static StaticListableBeanFactory beans(TripMedicationPort port) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("trips", port);
        return factory;
    }

    private DefaultTripMedicationApi api() {
        return new DefaultTripMedicationApi(
                handovers,
                orders,
                absences,
                prescriptions,
                medications,
                calculator,
                beans(port).getBeanProvider(TripMedicationPort.class),
                events,
                time);
    }

    private static CalculatedDoseSlot slot(DoseStatus status) {
        CalculatedDoseSlot slot = mock(CalculatedDoseSlot.class);
        when(slot.status()).thenReturn(status);
        return slot;
    }

    @Test
    void FR_048b_preparedWhenHandoverOrNoMedicationOrderOrNoDoseFallsInTheTrip() {
        when(time.now()).thenReturn(NOW);
        when(handovers.findByTripId(TRIP)).thenReturn(List.of());
        when(orders.findByResidentIdAndTripId(RESIDENT, TRIP)).thenReturn(Optional.empty());
        when(port.approvedTripInterval(RESIDENT, TRIP)).thenReturn(Optional.of(TRIP_INTERVAL));
        CalculatedDoseSlot upcoming = slot(DoseStatus.UPCOMING);
        CalculatedDoseSlot cancelled = slot(DoseStatus.CANCELLED);
        Prescription periodic = mock(Prescription.class);
        when(periodic.getPrescriptionType()).thenReturn(PrescriptionType.PERIODIC);
        when(prescriptions.findByResidentIdAndStatusIn(eq(RESIDENT), anyCollection()))
                .thenReturn(List.of(periodic));
        when(calculator.calculate(eq(periodic), any(), eq(TRIP_INTERVAL.from()), eq(TRIP_INTERVAL.to()), any(), any()))
                .thenReturn(List.of(upcoming));

        assertThat(api().isPrepared(RESIDENT, TRIP))
                .as("có liều, chưa giao, chưa có lệnh")
                .isFalse();

        when(calculator.calculate(eq(periodic), any(), any(), any(), any(), any()))
                .thenReturn(List.of(cancelled));
        assertThat(api().isPrepared(RESIDENT, TRIP))
                .as("không có liều trong khoảng đi")
                .isTrue();

        MedicationHandover given = mock(MedicationHandover.class);
        when(given.getResidentId()).thenReturn(RESIDENT);
        when(handovers.findByTripId(TRIP)).thenReturn(List.of(given));
        assertThat(api().isPrepared(RESIDENT, TRIP))
                .as("đã có lần giao cho chuyến")
                .isTrue();
    }

    @Test
    void Q_58_tripChangeRemindsReturnOfCarriedMedicationAndCorrectionMovesTheInterval() {
        when(time.now()).thenReturn(NOW);
        MedicationHandover carried = mock(MedicationHandover.class);
        when(carried.getId()).thenReturn(5L);
        when(carried.getResidentId()).thenReturn(RESIDENT);
        when(carried.getStatus()).thenReturn(HandoverStatus.HANDED_OVER);
        when(handovers.findByTripId(TRIP)).thenReturn(List.of(carried));

        api().onTripChanged(TRIP, RESIDENT, TripChangeType.CANCEL_TRIP);

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());
        assertThat(published.getValue()).isEqualTo(new CarriedMedicationReturnNeeded(5L, RESIDENT, TRIP, NOW));

        MedicationAbsenceInterval interval = new MedicationAbsenceInterval(
                RESIDENT, AbsenceIntervalType.TRIP_WITH_MED, NOW, TRIP_INTERVAL.to(), null, TRIP, NOW);
        when(absences.findByTripId(TRIP)).thenReturn(Optional.of(interval));
        api().recalculateInterval(TRIP, RESIDENT, TRIP_INTERVAL);
        assertThat(interval.getFromTime()).isEqualTo(TRIP_INTERVAL.from());
        verify(absences).save(interval);
    }

    @Test
    void Q_171_noMedicationOrderNeedsEligibilityAndCoversOnlyTheApprovedTrip() {
        when(time.now()).thenReturn(NOW);
        when(orders.findByResidentIdAndTripId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
        when(absences.findByTripId(TRIP)).thenReturn(Optional.empty());
        MedicationHandoverService service = new MedicationHandoverService(
                absences,
                handovers,
                mock(MedicationHandoverItemRepository.class),
                mock(MedicationHandoverDoseRepository.class),
                mock(MedicationReturnRepository.class),
                orders,
                prescriptions,
                medications,
                mock(MedicationDoseRepository.class),
                mock(ResidentMedicationLock.class),
                time,
                calculator,
                beans(port).getBeanProvider(TripMedicationPort.class),
                event -> {});
        NoMedicationOrderBody body = new NoMedicationOrderBody();
        body.setTripId(TRIP);
        body.setReason("Gia đình mang thuốc riêng");

        when(port.approvedTripInterval(RESIDENT, TRIP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.orderNoMedication(RESIDENT, body, 1L))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("CONDITION_NOT_MET"));
        verify(absences, never()).save(any());

        when(port.approvedTripInterval(RESIDENT, TRIP)).thenReturn(Optional.of(TRIP_INTERVAL));
        service.orderNoMedication(RESIDENT, body, 1L);
        ArgumentCaptor<MedicationAbsenceInterval> saved = ArgumentCaptor.forClass(MedicationAbsenceInterval.class);
        verify(absences).save(saved.capture());
        assertThat(saved.getValue().getAbsenceType()).isEqualTo(AbsenceIntervalType.TRIP_NO_MED);
        assertThat(saved.getValue().getFromTime()).isEqualTo(TRIP_INTERVAL.from());
        assertThat(saved.getValue().getExpectedToTime()).isEqualTo(TRIP_INTERVAL.to());
    }
}
