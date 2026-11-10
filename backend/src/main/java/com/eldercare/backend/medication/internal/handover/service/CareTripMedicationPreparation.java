package com.eldercare.backend.medication.internal.handover.service;

import com.eldercare.backend.care.api.dto.TripInterval;
import com.eldercare.backend.care.api.dto.TripMedicationChangeType;
import com.eldercare.backend.care.api.dto.TripParticipantSources.CarriedItem;
import com.eldercare.backend.care.api.dto.TripParticipantSources.CarriedMedication;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripDose;
import com.eldercare.backend.care.api.spi.TripMedicationPreparationPort;
import com.eldercare.backend.medication.api.MedicationScheduleApi;
import com.eldercare.backend.medication.api.TripMedicationApi;
import com.eldercare.backend.medication.api.dto.TimeInterval;
import com.eldercare.backend.medication.api.dto.TripChangeType;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandover;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.handover.persistence.NoMedicationOrderRepository;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@code care.api.spi.TripMedicationPreparationPort} của 014 bằng {@link TripMedicationApi} (hướng API Phase 6 của
 * 014, chủ dự án duyệt 2026-11-05): care khai, medication cài, không tạo phụ thuộc care → medication.
 */
@Component
public class CareTripMedicationPreparation implements TripMedicationPreparationPort {

    private final TripMedicationApi trips;
    private final MedicationScheduleApi schedules;
    private final MedicationHandoverRepository handovers;
    private final NoMedicationOrderRepository orders;
    private final PrescriptionRepository prescriptions;
    private final MedicationRepository medications;

    public CareTripMedicationPreparation(
            TripMedicationApi trips,
            MedicationScheduleApi schedules,
            MedicationHandoverRepository handovers,
            NoMedicationOrderRepository orders,
            PrescriptionRepository prescriptions,
            MedicationRepository medications) {
        this.trips = trips;
        this.schedules = schedules;
        this.handovers = handovers;
        this.orders = orders;
        this.prescriptions = prescriptions;
        this.medications = medications;
    }

    /** 014 TripDetail (FR-036; chủ dự án duyệt 2026-11-10): liều theo lịch có thời điểm dự kiến trong [from, to). */
    @Override
    @Transactional(readOnly = true)
    public List<TripDose> dosesDuring(long residentId, Instant from, Instant to) {
        return schedules.byResident(residentId, from, to).stream()
                .map(d -> new TripDose(
                        d.doseKey(),
                        d.prescriptionId(),
                        d.medication().map(m -> m.name()).orElse(null),
                        d.medication().map(m -> m.strength()).orElse(null),
                        d.scheduledTime(),
                        d.status() == null ? null : d.status().name()))
                .toList();
    }

    /**
     * 014 TripDetail: lần giao thuốc mang theo gần nhất của người ở chuyến (thời điểm, người nhận, các dòng), hoặc quyết
     * định không mang thuốc kèm lý do; không suy từ {@link #isPrepared}.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<CarriedMedication> carried(long residentId, long tripId) {
        var handover = handovers.findByTripId(tripId).stream()
                .filter(h -> h.getResidentId() == residentId)
                .max(Comparator.comparing(MedicationHandover::getHandoverTime));
        if (handover.isPresent()) {
            var h = handover.get();
            var items = h.getItems().stream()
                    .map(item -> {
                        var medication = prescriptions
                                .findById(item.getPrescriptionId())
                                .flatMap(p -> medications.findById(p.getMedicationId()));
                        return new CarriedItem(
                                medication.map(m -> m.getName()).orElse(null),
                                medication.map(m -> m.getConcentration()).orElse(null),
                                item.getQuantity(),
                                medication.map(m -> m.getUsageUnit()).orElse(null));
                    })
                    .toList();
            return Optional.of(new CarriedMedication(h.getHandoverTime(), h.getRecipientName(), items, null));
        }
        return orders.findByResidentIdAndTripId(residentId, tripId)
                .map(o -> new CarriedMedication(null, null, List.of(), o.getReason()));
    }

    @Override
    public boolean isPrepared(long residentId, long tripId) {
        return trips.isPrepared(residentId, tripId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onTripChanged(long tripId, long residentId, TripMedicationChangeType changeType) {
        trips.onTripChanged(tripId, residentId, TripChangeType.valueOf(changeType.name()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recalculateInterval(long tripId, long residentId, TripInterval newInterval) {
        trips.recalculateInterval(tripId, residentId, new TimeInterval(newInterval.from(), newInterval.to()));
    }
}
