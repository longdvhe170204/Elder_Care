package com.eldercare.backend.support.fake.health;

import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.health.api.dto.LatestVitalSignView;
import com.eldercare.backend.health.api.dto.VitalSignHandoverView;
import com.eldercare.backend.health.api.dto.VitalSignReadingView;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** Nguồn chỉ số vượt ngưỡng do test đặt, không phát sinh số đo. */
public class FakeVitalSigns implements VitalSignApi {
    private record HistoryQuery(long residentId, long indicatorId) {}

    private final Map<HistoryQuery, List<VitalSignReadingView>> history = new HashMap<>();
    private Set<Long> vitalSignIndicators = Set.of();

    public void history(long residentId, long indicatorId, List<VitalSignReadingView> values) {
        history.put(new HistoryQuery(residentId, indicatorId), List.copyOf(values));
    }

    public void vitalSignIndicators(Set<Long> indicatorIds) {
        vitalSignIndicators = Set.copyOf(indicatorIds);
    }

    @Override
    public List<VitalSignReadingView> withinRange(long residentId, long indicatorId, Instant from, Instant to) {
        return history.getOrDefault(new HistoryQuery(residentId, indicatorId), List.of()).stream()
                .filter(row ->
                        !row.measuredAt().isBefore(from) && row.measuredAt().isBefore(to))
                .toList();
    }

    @Override
    public Set<Long> vitalSignIndicatorIds() {
        return vitalSignIndicators;
    }

    private final Map<Long, Map<Long, LatestVitalSignView>> latestByResident =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Test đặt kết quả hiện hành đã được nguồn chọn; bản giả không tự ghi hoặc đính chính số đo. */
    public void latest(long residentId, Map<Long, LatestVitalSignView> values) {
        latestByResident.put(residentId, Map.copyOf(values));
    }

    @Override
    public Map<Long, LatestVitalSignView> latestPerType(long residentId) {
        return latestByResident.getOrDefault(residentId, Map.of());
    }

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
