package com.eldercare.backend.support.fake.health;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.health.api.VitalSignStatisticsApi;
import com.eldercare.backend.health.api.dto.VitalSignReportView;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Test đặt nguồn đã được xác nhận theo phạm vi; không tự ghi hoặc chọn bản đo thay thế. */
public class FakeVitalSignStatistics implements VitalSignStatisticsApi {
    private final Map<ScopeFilter, List<VitalSignReportView>> rows = new HashMap<>();

    private static ScopeFilter snapshot(ScopeFilter scope) {
        return new ScopeFilter(
                scope.facilityWide(),
                Set.copyOf(scope.floorAreaIds()),
                Set.copyOf(scope.roomIds()),
                Set.copyOf(scope.residentIds()));
    }

    public void source(ScopeFilter scope, List<VitalSignReportView> values) {
        rows.put(snapshot(scope), List.copyOf(values));
    }

    @Override
    public Stream<VitalSignReportView> readings(ScopeFilter scope, Instant from, Instant to) {
        return rows.getOrDefault(scope, List.of()).stream()
                .filter(row ->
                        !row.measuredAt().isBefore(from) && row.measuredAt().isBefore(to));
    }
}
