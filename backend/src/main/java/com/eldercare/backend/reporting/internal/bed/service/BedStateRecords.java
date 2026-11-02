package com.eldercare.backend.reporting.internal.bed.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.roombedallocation.api.BedStatisticsApi;
import com.eldercare.backend.roombedallocation.api.dto.BedStateOnDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trạng thái giường của 003 ({@code BedStatisticsApi.stateAt}) cho {@code BED.STATUS}, {@code BED.USABLE} (FR-020,
 * FR-024): lúc xem là hiện tại; báo cáo là cuối khoảng, không quá lúc xem. 016 không suy lại trạng thái (FR-002).
 */
@Component
@Transactional(readOnly = true)
public class BedStateRecords {

    /** Một giường tại thời điểm tra cứu; khóa chia nhóm tách hai loại Đang giữ chỗ. */
    public record BedStateRecord(BedStateOnDate view) implements MetricRecord {

        public String key() {
            return view.holdReason() == null
                    ? view.state().name()
                    : view.state().name() + "_" + view.holdReason();
        }

        @Override
        public String sourceKey() {
            return "M03.BED:" + view.bedId();
        }

        @Override
        public Instant referenceTime() {
            return view.at();
        }

        @Override
        public Long floorAreaId() {
            return view.floorId();
        }

        @Override
        public Long roomId() {
            return view.roomId();
        }

        @Override
        public Long residentId() {
            return null;
        }
    }

    private final ObjectProvider<BedStatisticsApi> statistics;
    private final BusinessTime time;

    public BedStateRecords(ObjectProvider<BedStatisticsApi> statistics, BusinessTime time) {
        this.statistics = statistics;
        this.time = time;
    }

    /** Trạng thái ngay trước {@code to} (cuối khoảng loại trừ), hoặc lúc xem nếu khoảng chưa kết thúc. */
    public Stream<BedStateRecord> at(ScopeFilter rough, Instant to) {
        BedStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        Instant now = time.now();
        Instant at = to == null || to.isAfter(now) ? now : to.minus(1, ChronoUnit.MICROS);
        return api.stateAt(rough, at).map(BedStateRecord::new);
    }

    static DetailRow row(BedStateRecord r) {
        return new DetailRow(
                r.sourceKey(),
                "003",
                "BED",
                r.key(),
                r.referenceTime(),
                r.floorAreaId(),
                null,
                null,
                null,
                null,
                Map.of("roomId", r.view().roomId()),
                null,
                Map.of(),
                false,
                false,
                false);
    }
}
