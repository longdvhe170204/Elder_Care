package com.eldercare.backend.care.api.spi;

import java.time.Instant;
import java.util.Optional;

/** 012 kiểm người đón; markUsed chạy MANDATORY, lần hai báo PICKUP_RECORD_ALREADY_USED. */
public interface DayCarePickupPort {
    Optional<PickupView> valid(long pickupRecordId, long residentId, Instant at);

    void markUsed(long pickupRecordId, long attendanceRecordId);

    record PickupView(long id, String pickupType) {}
}
