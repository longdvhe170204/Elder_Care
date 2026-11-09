package com.eldercare.backend.medication.internal.handover.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.TripMedicationApi;
import com.eldercare.backend.medication.api.dto.TimeInterval;
import com.eldercare.backend.medication.api.dto.TripChangeType;
import com.eldercare.backend.medication.api.events.CarriedMedicationReturnNeeded;
import com.eldercare.backend.medication.api.spi.TripMedicationPort;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.handover.domain.HandoverStatus;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandover;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationAbsenceIntervalRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.handover.persistence.NoMedicationOrderRepository;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import com.eldercare.backend.medication.internal.schedule.service.ScheduleContext;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thuốc mang theo chuyến đi cho 014 (T041, research D-15; FR-048b, Q-58, Q-171): đã chuẩn bị khi có lần giao cho chuyến,
 * lệnh "Không mang thuốc" hoặc không có liều nào trong khoảng đi đã duyệt; chuyến đổi thì nhắc Nhận lại; đính chính giờ
 * rời, về thì dời khoảng vắng của chuyến.
 */
@Service
public class DefaultTripMedicationApi implements TripMedicationApi {

    private final MedicationHandoverRepository handovers;
    private final NoMedicationOrderRepository orders;
    private final MedicationAbsenceIntervalRepository absences;
    private final PrescriptionRepository prescriptions;
    private final MedicationRepository medications;
    private final MedicationScheduleCalculator calculator;
    private final ObjectProvider<TripMedicationPort> trips;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public DefaultTripMedicationApi(
            MedicationHandoverRepository handovers,
            NoMedicationOrderRepository orders,
            MedicationAbsenceIntervalRepository absences,
            PrescriptionRepository prescriptions,
            MedicationRepository medications,
            MedicationScheduleCalculator calculator,
            ObjectProvider<TripMedicationPort> trips,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.handovers = handovers;
        this.orders = orders;
        this.absences = absences;
        this.prescriptions = prescriptions;
        this.medications = medications;
        this.calculator = calculator;
        this.trips = trips;
        this.events = events;
        this.time = time;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPrepared(long residentId, long tripId) {
        if (handovers.findByTripId(tripId).stream().anyMatch(h -> h.getResidentId() == residentId)
                || orders.findByResidentIdAndTripId(residentId, tripId).isPresent()) {
            return true;
        }
        TripMedicationPort port = trips.getIfAvailable();
        Optional<TimeInterval> interval =
                port == null ? Optional.empty() : port.approvedTripInterval(residentId, tripId);
        if (interval.isEmpty()) {
            return false;
        }
        for (Prescription p :
                prescriptions.findByResidentIdAndStatusIn(residentId, List.of(PrescriptionStatus.ACTIVE))) {
            if (p.getPrescriptionType() != PrescriptionType.PERIODIC) {
                continue;
            }
            boolean hasDose = calculator
                    .calculate(
                            p,
                            medications.findById(p.getMedicationId()).orElse(null),
                            interval.get().from(),
                            interval.get().to(),
                            time.now(),
                            ScheduleContext.empty(residentId))
                    .stream()
                    .anyMatch(slot -> slot.status() != DoseStatus.CANCELLED);
            if (hasDose) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional
    public void onTripChanged(long tripId, long residentId, TripChangeType changeType) {
        for (MedicationHandover handover : handovers.findByTripId(tripId)) {
            if (handover.getResidentId() == residentId && handover.getStatus() == HandoverStatus.HANDED_OVER) {
                events.publishEvent(
                        new CarriedMedicationReturnNeeded(handover.getId(), residentId, tripId, time.now()));
            }
        }
    }

    @Override
    @Transactional
    public void recalculateInterval(long tripId, long residentId, TimeInterval newInterval) {
        absences.findByTripId(tripId)
                .filter(interval -> interval.getResidentId() == residentId)
                .ifPresent(interval -> {
                    interval.reschedule(newInterval.from(), newInterval.to());
                    absences.save(interval);
                });
    }
}
