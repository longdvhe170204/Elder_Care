package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.api.*;
import com.cambae.backend.m03.api.dto.*;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** Nguồn giường/cảnh báo phân bổ dùng chung của 003. */
public class FakeHandoverLocations implements BedStatusApi, AllocationHandoverApi {
    private final Map<Long, List<AwaitingReadyBedView>> beds = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Long, List<AllocationHandoverView>> alerts = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Long, String> states = new java.util.concurrent.ConcurrentHashMap<>();

    public void beds(long floor, List<AwaitingReadyBedView> values) {
        beds.put(floor, List.copyOf(values));
        values.forEach(b -> states.put(b.bedId(), "AWAITING_READINESS"));
    }

    public void state(long bed, String state) {
        states.put(bed, state);
    }

    public Map<Long, String> currentState(Collection<Long> ids, Instant at) {
        var result = new LinkedHashMap<Long, String>();
        for (long id : ids) if (states.containsKey(id)) result.put(id, states.get(id));
        return result;
    }

    public void alerts(long floor, List<AllocationHandoverView> values) {
        alerts.put(floor, List.copyOf(values));
    }

    public List<AwaitingReadyBedView> awaitingReadinessOverdue(long floor, Instant at) {
        return beds.getOrDefault(floor, List.of());
    }

    public List<AllocationHandoverView> atFloor(long floor, Instant at) {
        return alerts.getOrDefault(floor, List.of());
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @Bean
        @Primary
        FakeHandoverLocations fakeHandoverLocations() {
            return new FakeHandoverLocations();
        }
    }
}
