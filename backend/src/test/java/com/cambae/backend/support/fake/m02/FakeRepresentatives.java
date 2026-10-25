package com.cambae.backend.support.fake.m02;

import com.cambae.backend.m02.api.spi.RepresentativePort;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Bản giả {@link RepresentativePort} (012 cài): mặc định chưa ai có người đại diện. */
public class FakeRepresentatives implements RepresentativePort {

    private final Map<Long, Set<Long>> representatives = new HashMap<>();

    public FakeRepresentatives representative(long residentId, long familyMemberId) {
        representatives.computeIfAbsent(residentId, id -> new HashSet<>()).add(familyMemberId);
        return this;
    }

    public FakeRepresentatives reset() {
        representatives.clear();
        return this;
    }

    @Override
    public boolean isActiveRepresentative(long familyMemberId, long residentId, LocalDate date) {
        return representatives.getOrDefault(residentId, Set.of()).contains(familyMemberId);
    }

    @Override
    public boolean hasRepresentative(long residentId) {
        return !representatives.getOrDefault(residentId, Set.of()).isEmpty();
    }
}
