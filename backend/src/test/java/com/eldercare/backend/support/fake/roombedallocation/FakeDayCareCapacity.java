package com.eldercare.backend.support.fake.roombedallocation;

import com.eldercare.backend.roombedallocation.api.DayCareCapacityApi;
import com.eldercare.backend.roombedallocation.api.dto.CapacityViolation;
import com.eldercare.backend.roombedallocation.api.dto.DayCareConfiguration;
import com.eldercare.backend.roombedallocation.api.dto.RegistrationCount;
import com.eldercare.backend.roombedallocation.api.dto.SessionDate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Bản giả sức chứa khu nghỉ cho test của 004: không dựng khu nghỉ; đặt vi phạm bằng {@link #violations}. */
public class FakeDayCareCapacity implements DayCareCapacityApi {

    private final List<CapacityViolation> violations = new ArrayList<>();

    public FakeDayCareCapacity violations(CapacityViolation... values) {
        violations.clear();
        violations.addAll(List.of(values));
        return this;
    }

    public void reset() {
        violations.clear();
    }

    @Override
    public DayCareConfiguration lock() {
        return new DayCareConfiguration(1L, null, 100, List.of());
    }

    @Override
    public List<SessionDate> occupiedSessions(LocalDate date, LocalTime arrival, LocalTime departure) {
        return List.of();
    }

    @Override
    public List<CapacityViolation> checkCapacity(Map<SessionDate, RegistrationCount> counts) {
        return List.copyOf(violations);
    }
}
