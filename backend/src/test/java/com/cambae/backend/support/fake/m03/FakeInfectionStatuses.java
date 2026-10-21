package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.api.spi.AllocationInfectionStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/** Bản giả 007: các mốc vùng khoanh và danh sách nghi nhiễm/tiếp xúc do test đặt (003 T002, G-16). */
public class FakeInfectionStatuses implements AllocationInfectionStatus {

    private final NavigableMap<Instant, RestrictedZone> zones = new TreeMap<>();
    private final Map<Long, NavigableMap<Instant, Boolean>> listings = new HashMap<>();

    public FakeInfectionStatuses zone(Instant from, RestrictedZone zone) {
        zones.put(from, zone);
        return this;
    }

    public FakeInfectionStatuses listed(long residentId, Instant from, boolean listed) {
        listings.computeIfAbsent(residentId, key -> new TreeMap<>()).put(from, listed);
        return this;
    }

    public void reset() {
        zones.clear();
        listings.clear();
    }

    @Override
    public RestrictedZone restrictedZone(Instant at) {
        Map.Entry<Instant, RestrictedZone> entry = zones.floorEntry(at);
        return entry == null ? new RestrictedZone(Set.of(), Set.of()) : entry.getValue();
    }

    @Override
    public boolean listed(long residentId, Instant at) {
        NavigableMap<Instant, Boolean> history = listings.get(residentId);
        Map.Entry<Instant, Boolean> entry = history == null ? null : history.floorEntry(at);
        return entry != null && entry.getValue();
    }
}
