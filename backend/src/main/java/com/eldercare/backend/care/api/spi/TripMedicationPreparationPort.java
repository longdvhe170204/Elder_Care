package com.eldercare.backend.care.api.spi;

import com.eldercare.backend.care.api.dto.TripInterval;
import com.eldercare.backend.care.api.dto.TripMedicationChangeType;
import com.eldercare.backend.care.api.dto.TripParticipantSources.CarriedMedication;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripDose;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 006 cài nguồn thuốc cho chuyến; chủ dự án duyệt hướng API 05/11/2026. */
public interface TripMedicationPreparationPort {
    boolean isPrepared(long residentId, long tripId);

    /** TripDetail (FR-036; chủ dự án duyệt 2026-11-10): liều có thời điểm dự kiến trong [from, to). */
    List<TripDose> dosesDuring(long residentId, Instant from, Instant to);

    /** TripDetail: lần giao thuốc mang theo của người ở chuyến, hoặc quyết định không mang; rỗng khi chưa có. */
    Optional<CarriedMedication> carried(long residentId, long tripId);

    @Transactional(propagation = Propagation.MANDATORY)
    void onTripChanged(long tripId, long residentId, TripMedicationChangeType changeType);

    @Transactional(propagation = Propagation.MANDATORY)
    void recalculateInterval(long tripId, long residentId, TripInterval newInterval);
}
