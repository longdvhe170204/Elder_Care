package com.eldercare.backend.support.fake.roombedallocation;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.roombedallocation.api.AssetStatisticsApi;
import com.eldercare.backend.roombedallocation.api.dto.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 019 khai, chủ fake là C: số liệu tài sản, lịch xe cho 016. Dấu quá hạn có hiệu lực từ ngày sau {@code dueOn}; phạm
 * vi theo tầng của vị trí (tài sản không có tầng chỉ thấy ở phạm vi toàn viện). Không mô phỏng job hay quyền thật.
 */
public class FakeAssetStatistics implements AssetStatisticsApi {
    private final List<OverdueAssetView> overdue = new ArrayList<>();
    private final List<VehicleScheduleView> schedules = new ArrayList<>();

    public FakeAssetStatistics overdue(OverdueAssetView row) {
        overdue.add(row);
        return this;
    }

    public FakeAssetStatistics schedule(VehicleScheduleView row) {
        schedules.add(row);
        return this;
    }

    @Override
    public Stream<OverdueAssetView> overdue(ScopeFilter scope, LocalDate on) {
        return overdue.stream()
                .filter(r -> r.dueOn().isBefore(on))
                .filter(r -> scope.facilityWide()
                        || (r.floorAreaId() != null && scope.floorAreaIds().contains(r.floorAreaId())));
    }

    @Override
    public Map<AssetFlag, Long> countOverdue(ScopeFilter scope, LocalDate on) {
        return overdue(scope, on).collect(Collectors.groupingBy(OverdueAssetView::flag, Collectors.counting()));
    }

    @Override
    public Stream<VehicleScheduleView> vehicleSchedules(LocalDate from, LocalDate to) {
        return schedules.stream().filter(r -> {
            var day = LocalDate.ofInstant(r.plannedDepartureAt(), com.eldercare.backend.common.time.BusinessTime.ZONE);
            return !day.isBefore(from) && !day.isAfter(to);
        });
    }

    public void reset() {
        overdue.clear();
        schedules.clear();
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        FakeAssetStatistics fakeAssetStatistics() {
            return new FakeAssetStatistics();
        }
    }
}
