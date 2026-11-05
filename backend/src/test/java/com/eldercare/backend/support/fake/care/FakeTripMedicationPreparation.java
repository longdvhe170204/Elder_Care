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

    @Override
    public void recalculateInterval(long trip, long resident, TripInterval interval) {}
}
