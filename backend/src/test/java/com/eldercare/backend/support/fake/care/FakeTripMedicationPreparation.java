package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.TripMedicationPreparationPort;
import java.util.*;

/** Bản giả duy nhất của SPI thuốc do care khai; không có trong production. */
public class FakeTripMedicationPreparation implements TripMedicationPreparationPort {
    private final Set<String> prepared = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<String, Runnable> checks = new java.util.concurrent.ConcurrentHashMap<>();

    public void prepared(long resident, long trip) {
        prepared.add(resident + ":" + trip);
    }

    /** Điểm hẹn một lần để test xen kẽ phân công với kiểm điều kiện rời viện. */
    public void onPreparedCheck(long resident, long trip, Runnable check) {
        checks.put(resident + ":" + trip, check);
    }

    @Override
    public boolean isPrepared(long resident, long trip) {
        var check = checks.remove(resident + ":" + trip);
        if (check != null) check.run();
        return prepared.contains(resident + ":" + trip);
    }

    @Override
    public void onTripChanged(long trip, long resident, TripMedicationChangeType type) {}

    private final Map<Long, java.util.List<com.eldercare.backend.care.api.dto.TripParticipantSources.TripDose>> doses =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, com.eldercare.backend.care.api.dto.TripParticipantSources.CarriedMedication> carried =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** TripDetail: liều của người cao tuổi trả cho mọi khoảng hỏi. */
    public void doses(
            long resident, java.util.List<com.eldercare.backend.care.api.dto.TripParticipantSources.TripDose> list) {
        doses.put(resident, list);
    }

    public void carried(
            long resident,
            long trip,
            com.eldercare.backend.care.api.dto.TripParticipantSources.CarriedMedication value) {
        carried.put(resident + ":" + trip, value);
    }

    @Override
    public java.util.List<com.eldercare.backend.care.api.dto.TripParticipantSources.TripDose> dosesDuring(
            long resident, java.time.Instant from, java.time.Instant to) {
        return doses.getOrDefault(resident, java.util.List.of());
    }

    @Override
    public Optional<com.eldercare.backend.care.api.dto.TripParticipantSources.CarriedMedication> carried(
            long resident, long trip) {
        return Optional.ofNullable(carried.get(resident + ":" + trip));
    }

    private final Map<String, TripInterval> intervals = new java.util.concurrent.ConcurrentHashMap<>();

    /** Khoảng mới nhất 014 gửi khi gia hạn hoặc điểm danh về (FR-043, FR-044). */
    public Optional<TripInterval> interval(long resident, long trip) {
        return Optional.ofNullable(intervals.get(resident + ":" + trip));
    }

    @Override
    public void recalculateInterval(long trip, long resident, TripInterval interval) {
        intervals.put(resident + ":" + trip, interval);
    }
}
