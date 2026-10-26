package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.api.spi.AllocationInfectionStatus;
import com.cambae.backend.m03.api.spi.CordonedArea;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/** Bản giả 007: trạng thái do test đặt có hiệu lực từ mốc chỉ định, không dùng giờ hệ thống. */
public class FakeAllocationInfectionStatus implements AllocationInfectionStatus {
    private final NavigableMap<Instant, CordonedArea> cordons = new TreeMap<>();
    private final Map<Long, NavigableMap<Instant, Boolean>> listed = new HashMap<>();

    public FakeAllocationInfectionStatus cordon(Instant from, CordonedArea area) {
        cordons.put(from, area);
        return this;
    }

    public FakeAllocationInfectionStatus listed(long residentId, Instant from, boolean value) {
        listed.computeIfAbsent(residentId, ignored -> new TreeMap<>()).put(from, value);
        return this;
    }

    @Override
    public CordonedArea cordonedAt(Instant at) {
        var entry = cordons.floorEntry(at);
        return entry == null ? new CordonedArea(Set.of(), Set.of()) : entry.getValue();
    }

    @Override
    public boolean listedAt(long residentId, Instant at) {
        var history = listed.get(residentId);
        var entry = history == null ? null : history.floorEntry(at);
        return entry != null && entry.getValue();
    }

    public void reset() {
        cordons.clear();
        listed.clear();
    }
}
