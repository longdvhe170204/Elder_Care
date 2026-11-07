package com.eldercare.backend.care.api.spi;

import com.eldercare.backend.care.api.dto.TripInterval;
import com.eldercare.backend.care.api.dto.TripMedicationChangeType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 006 cài nguồn thuốc cho chuyến; chủ dự án duyệt hướng API 05/11/2026. */
public interface TripMedicationPreparationPort {
    boolean isPrepared(long residentId, long tripId);

    @Transactional(propagation = Propagation.MANDATORY)
    void onTripChanged(long tripId, long residentId, TripMedicationChangeType changeType);

    @Transactional(propagation = Propagation.MANDATORY)
    void recalculateInterval(long tripId, long residentId, TripInterval newInterval);
}
