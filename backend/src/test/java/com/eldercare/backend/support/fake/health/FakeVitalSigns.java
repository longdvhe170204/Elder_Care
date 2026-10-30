package com.eldercare.backend.support.fake.health;

import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.health.api.dto.VitalSignHandoverView;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** Nguồn chỉ số vượt ngưỡng do test đặt, không phát sinh số đo. */
public class FakeVitalSigns implements VitalSignApi {
    private final Map<Long, List<VitalSignHandoverView>> rows = new java.util.concurrent.ConcurrentHashMap<>();

    public void source(long floor, List<VitalSignHandoverView> values) {
        rows.put(floor, List.copyOf(values));
    }

    public List<VitalSignHandoverView> aboveThresholdInRange(long floor, Instant from, Instant to) {
        return rows.getOrDefault(floor, List.of()).stream()
                .filter(r -> !r.measuredAt().isBefore(from) && r.measuredAt().isBefore(to))
                .toList();
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @Bean
        @Primary
        FakeVitalSigns fakeVitalSigns() {
            return new FakeVitalSigns();
        }
    }
}
