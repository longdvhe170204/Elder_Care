package com.cambae.backend.support.fake.m04;

import com.cambae.backend.m04.api.spi.DayCarePickupPort;
import java.time.Instant;
import java.util.*;

/** Fixture 012: kiểm theo người/thời điểm; phát hiện dùng lại bản ghi đón. */
public class FakeDayCarePickups implements DayCarePickupPort {
    private final FakeM04Switches switches;
    private final Map<PickupKey, PickupView> pickups = new HashMap<>();
    private final Map<Long, Long> used = new HashMap<>();

    public FakeDayCarePickups(FakeM04Switches switches) {
        this.switches = switches;
    }

    public void valid(long pickupRecordId, long residentId, Instant at, PickupView value) {
        pickups.put(new PickupKey(pickupRecordId, residentId, at), value);
    }

    @Override
    public Optional<PickupView> valid(long pickupRecordId, long residentId, Instant at) {
        switches.requireEnabled("012");
        if (used.containsKey(pickupRecordId)) return Optional.empty();
        return Optional.ofNullable(pickups.get(new PickupKey(pickupRecordId, residentId, at)));
    }

    @Override
    public void markUsed(long pickupRecordId, long attendanceRecordId) {
        switches.requireEnabled("012");
        if (used.putIfAbsent(pickupRecordId, attendanceRecordId) != null)
            throw new IllegalStateException("PICKUP_RECORD_ALREADY_USED");
    }

    public Map<Long, Long> used() {
        return Map.copyOf(used);
    }

    public void reset() {
        pickups.clear();
        used.clear();
    }

    private record PickupKey(long pickupRecordId, long residentId, Instant at) {}
}
