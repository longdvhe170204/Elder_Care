package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.asset.service.AssetOverdueMetric;
import com.eldercare.backend.reporting.internal.asset.service.AssetOverdueMetric.AssetRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.roombedallocation.api.AssetStatisticsApi;
import com.eldercare.backend.roombedallocation.api.dto.AssetFlag;
import com.eldercare.backend.roombedallocation.api.dto.OverdueAssetView;
import com.eldercare.backend.support.fake.roombedallocation.FakeAssetStatistics;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Chỉ tiêu tài sản quá hạn ({@code TS.QUA_HAN}; FR-020 dòng "Tài sản quá hạn", 019 FR-014) đọc
 * {@code AssetStatisticsApi.overdue} của 003: dấu quá hạn bảo trì, đăng kiểm, bảo hiểm hiện tại, chia theo loại dấu;
 * dấu "chờ chuyển người" không phải dấu quá hạn; tầng theo vị trí tài sản.
 */
class MetricAssetOverdueTest {

    private static final Instant NOW = Instant.parse("2026-11-08T03:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-11-08");

    private final FakeAssetStatistics assets = new FakeAssetStatistics();
    private final AssetOverdueMetric source = new AssetOverdueMetric(
            new StaticListableBeanFactory(Map.of("assets", assets)).getBeanProvider(AssetStatisticsApi.class),
            new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE)));

    private static OverdueAssetView asset(long id, Long floor, AssetFlag flag, LocalDate dueOn) {
        return new OverdueAssetView(id, "TS-" + id, "Tài sản " + id, 3, floor, flag, dueOn);
    }

    @Test
    void FR_020_overdueFlags_byFlag_awaitingTransferNotCounted_floorFromLocation() {
        assets.overdue(asset(1, 101L, AssetFlag.MAINTENANCE_OVERDUE, TODAY.minusDays(3)))
                .overdue(asset(2, null, AssetFlag.INSURANCE_OVERDUE, TODAY.minusDays(1)))
                .overdue(asset(2, null, AssetFlag.MAINTENANCE_OVERDUE, TODAY.minusDays(1)))
                .overdue(asset(3, 102L, AssetFlag.VEHICLE_INSPECTION_OVERDUE, TODAY.minusDays(10)))
                .overdue(asset(4, 101L, AssetFlag.AWAITING_RESIDENT_TRANSFER, TODAY.minusDays(2)));

        List<AssetRecord> included = source.metric()
                .included(source.records(ScopeFilter.FACILITY, null, null, ReportFilters.NONE), r -> true);
        MetricValue v = source.metric().compute(included);

        assertThat(source.metric().code()).isEqualTo("ASSET.OVERDUE");
        assertThat(source.metric().group()).isEqualTo(MetricGroup.ASSETS);
        assertThat(v.value()).isEqualByComparingTo("4");
        assertThat(v.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> b.value().intValue())
                .containsExactly(
                        tuple("INSURANCE_OVERDUE", 1),
                        tuple("MAINTENANCE_OVERDUE", 2),
                        tuple("VEHICLE_INSPECTION_OVERDUE", 1));
        assertThat(source.metric().details(included, "MAINTENANCE_OVERDUE"))
                .extracting(
                        r -> r.floorAreaId(),
                        r -> r.result().get("code"),
                        r -> r.result().get("dueOn"))
                .containsExactly(tuple(101L, "TS-1", TODAY.minusDays(3)), tuple(null, "TS-2", TODAY.minusDays(1)));
    }
}
