package com.cambae.backend.m14.internal.bed.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.BedStatisticsApi;
import com.cambae.backend.m03.api.dto.RoomIsolationView;
import com.cambae.backend.m14.internal.metric.domain.DetailRow;
import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricRecord;
import com.cambae.backend.m14.internal.metric.domain.MetricSource;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code BED.ISOLATED_ROOMS} (FR-020, dòng "Tình trạng phòng/giường"): số phòng đang cách ly theo 003, lúc xem
 * hoặc cuối ngày đã chọn.
 */
@Component
@Transactional(readOnly = true)
public class IsolatedRoomsMetric implements MetricSource<IsolatedRoomsMetric.IsolatedRoomRecord> {

    public static final String CODE = "BED.ISOLATED_ROOMS";

    /** Một phòng đang cách ly tại thời điểm tra cứu. */
    public record IsolatedRoomRecord(RoomIsolationView view, Instant at) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M03.ROOM:" + view.roomId();
        }

        @Override
        public Instant referenceTime() {
            return at;
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

    private static final Metric<IsolatedRoomRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.BEDS,
            "Phòng đang cách ly",
            "Số phòng đang cách ly (003) tại thời điểm xem hoặc cuối ngày đã chọn",
            "003 BedStatisticsApi.isolatedRooms",
            new Metric.Computation.Count<>(r -> true),
            null,
            r -> new DetailRow(
                    r.sourceKey(),
                    "003",
                    "ROOM_ISOLATION",
                    null,
                    r.view().from(),
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
                    false));

    private final ObjectProvider<BedStatisticsApi> statistics;
    private final BusinessTime time;

    public IsolatedRoomsMetric(ObjectProvider<BedStatisticsApi> statistics, BusinessTime time) {
        this.statistics = statistics;
        this.time = time;
    }

    @Override
    public Metric<IsolatedRoomRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<IsolatedRoomRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        BedStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        Instant now = time.now();
        Instant at = to == null || to.isAfter(now) ? now : to.minus(1, ChronoUnit.MICROS);
        return api.isolatedRooms(rough, at).map(v -> new IsolatedRoomRecord(v, at));
    }
}
