package com.eldercare.backend.roombedallocation.internal.asset.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.AssetStatisticsApi;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStatisticsStore;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleSchedule;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T023, contracts mục 2.3: chỉ đọc, không khóa. Ngày on lọc dấu/hạn hiện tại (chủ dự án xác nhận 2026-11-06),
 * không tái dựng lịch sử. Lịch xe toàn viện lấy từ bảng chung qua API của 000, khoảng ngày nghiệp vụ gồm hai đầu.
 */
@Service
@Transactional(readOnly = true)
public class AssetStatisticsService implements AssetStatisticsApi {
    private final AssetStatisticsStore store;
    private final SimpleRecordApi records;

    public AssetStatisticsService(AssetStatisticsStore store, SimpleRecordApi records) {
        this.store = store;
        this.records = records;
    }

    @Override
    public Stream<OverdueAssetView> overdue(ScopeFilter scope, LocalDate on) {
        return store.overdue(scope, on).stream();
    }

    @Override
    public Map<AssetFlag, Long> countOverdue(ScopeFilter scope, LocalDate on) {
        return store.countOverdue(scope, on);
    }

    @Override
    public Stream<VehicleScheduleView> vehicleSchedules(LocalDate from, LocalDate to) {
        return records
                .listByStatus(
                        VehicleSchedule.KIND,
                        Set.of(
                                VehicleSchedule.BOOKED,
                                VehicleSchedule.IN_USE,
                                VehicleSchedule.COMPLETED,
                                VehicleSchedule.CANCELLED))
                .stream()
                .filter(row -> {
                    var day = LocalDate.ofInstant(row.content().plannedDepartureAt(), BusinessTime.ZONE);
                    return !day.isBefore(from) && !day.isAfter(to);
                })
                .map(row -> {
                    var c = row.content();
                    return new VehicleScheduleView(
                            row.id(),
                            c.vehicleId(),
                            c.plannedDepartureAt(),
                            c.plannedReturnAt(),
                            VehicleSchedulePurpose.valueOf(c.purpose()),
                            c.tripId(),
                            VehicleScheduleStatus.valueOf(row.status()),
                            c.departedAt(),
                            c.returnedAt());
                });
    }
}
