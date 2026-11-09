package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.TripInterval;
import java.util.Optional;

/** 014 cung cấp cho adapter TripMedicationPort của 006; không tạo phụ thuộc care → medication. */
public interface TripMedicationReadApi {
    Optional<TripInterval> approvedTripInterval(long residentId, long tripId);

    Optional<TripInterval> tripInterval(long tripId);
}
