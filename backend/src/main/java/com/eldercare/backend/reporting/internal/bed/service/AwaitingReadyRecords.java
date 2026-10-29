package com.eldercare.backend.reporting.internal.bed.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.roombedallocation.api.BedStatisticsApi;
import com.eldercare.backend.roombedallocation.api.dto.AwaitingReadyInterval;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Các lần giường ở Chờ sẵn sàng cho {@code BED.AWAITING_READY*} (FR-033, Q-390): lần có lúc bắt đầu trong khoảng, tính một
 * lần với thời gian đầy đủ; lần còn mở tính tới lúc xem. Hạn vượt là {@code deadline} do 003 tính theo CFG-M03-04.
 */
@Component
@Transactional(readOnly = true)
public class AwaitingReadyRecords {

    /** Một lần Chờ sẵn sàng; mốc xếp khoảng là lúc bắt đầu. */
    public record AwaitingReadyRecord(AwaitingReadyInterval view, BigDecimal minutes, boolean overdue)
            implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M03.BED_AWAITING_READY:" + view.bedId() + ":" + view.startedAt();
        }

        @Override
        public Instant referenceTime() {
            return view.startedAt();
        }

        @Override
        public Long floorAreaId() {
            return view.floorId();
        }

        @Override
        public Long residentId() {
            return null;
        }

        String floorKey() {
            return Long.toString(view.floorId());
        }
    }

    private final ObjectProvider<BedStatisticsApi> statistics;
    private final BusinessTime time;

    public AwaitingReadyRecords(ObjectProvider<BedStatisticsApi> statistics, BusinessTime time) {
        this.statistics = statistics;
        this.time = time;
    }

    public Stream<AwaitingReadyRecord> in(ScopeFilter rough, Instant from, Instant to) {
        BedStatisticsApi api = statistics.getIfAvailable();
        Instant now = time.now();
        if (api == null || !from.isBefore(now)) {
            return Stream.empty();
        }
        // Đọc tới lúc xem để lần bắt đầu trong khoảng mà kết thúc sau cuối khoảng vẫn đủ thời gian.
        return api.awaitingReady(rough, from, now)
                .filter(v -> !v.startedAt().isBefore(from) && v.startedAt().isBefore(to))
                .map(v -> new AwaitingReadyRecord(
                        v,
                        BigDecimal.valueOf(
                                Duration.between(v.startedAt(), v.to()).toMinutes()),
                        v.to().isAfter(v.deadline())));
    }

    static DetailRow row(AwaitingReadyRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("minutes", r.minutes());
        result.put("endedAt", r.view().to());
        result.put("deadline", r.view().deadline());
        result.put("overdue", r.overdue());
        return new DetailRow(
                r.sourceKey(),
                "003",
                "BED_AWAITING_READY",
                r.overdue() ? "OVERDUE" : null,
                r.referenceTime(),
                r.floorAreaId(),
                null,
                null,
                null,
                null,
                result,
                null,
                Map.of(),
                false,
                false,
                false);
    }
}
