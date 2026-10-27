package com.cambae.backend.m14;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.BedStatisticsApi;
import com.cambae.backend.m03.api.dto.AwaitingReadyInterval;
import com.cambae.backend.m03.api.dto.BedStateOnDate;
import com.cambae.backend.m03.api.dto.RoomIsolationView;
import com.cambae.backend.m14.internal.bed.service.AwaitingReadyMaximumMetric;
import com.cambae.backend.m14.internal.bed.service.AwaitingReadyMetric;
import com.cambae.backend.m14.internal.bed.service.AwaitingReadyOverdueMetric;
import com.cambae.backend.m14.internal.bed.service.AwaitingReadyRecords;
import com.cambae.backend.m14.internal.bed.service.AwaitingReadyRecords.AwaitingReadyRecord;
import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricValue;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters;
import com.cambae.backend.m14.internal.scope.domain.ReportScope;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Chỉ tiêu thời gian giường Chờ sẵn sàng (tên logic {@code GIUONG.CHO_SAN_SANG}; 016 T015, FR-033, Q-282, Q-390): mỗi lần
 * giường vào Chờ sẵn sàng có lúc bắt đầu trong khoảng tính một lần với thời gian đầy đủ, lần còn mở tính tới lúc xem;
 * trung bình, dài nhất (phút) và số lần vượt hạn CFG-M03-04, chia theo tầng.
 */
class MetricBedAwaitingReadyTest {

    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");

    /** Stub cục bộ: trả khoảng đã cắt theo {@code [from, to)} như bản cài 003, nhớ khoảng được hỏi. */
    static final class StubBedStatistics implements BedStatisticsApi {
        List<Instant[]> intervals = List.of();
        final List<Instant[]> asked = new ArrayList<>();

        void add(long bed, long floor, Instant start, Instant end, Duration limit) {
            List<Instant[]> all = new ArrayList<>(intervals);
            all.add(new Instant[] {Instant.ofEpochMilli(bed), Instant.ofEpochMilli(floor), start, end, start.plus(limit)
            });
            intervals = all;
        }

        @Override
        public Stream<BedStateOnDate> stateAt(ScopeFilter scope, Instant at) {
            return Stream.empty();
        }

        @Override
        public Stream<AwaitingReadyInterval> awaitingReady(ScopeFilter scope, Instant from, Instant to) {
            asked.add(new Instant[] {from, to});
            return intervals.stream()
                    .filter(i -> i[2].isBefore(to) && (i[3] == null || i[3].isAfter(from)))
                    .map(i -> new AwaitingReadyInterval(
                            i[0].toEpochMilli(),
                            i[1].toEpochMilli(),
                            i[2].isBefore(from) ? from : i[2],
                            i[3] == null || i[3].isAfter(to) ? to : i[3],
                            i[4],
                            i[2]));
        }

        @Override
        public Stream<RoomIsolationView> isolatedRooms(ScopeFilter scope, Instant at) {
            return Stream.empty();
        }
    }

    private final StubBedStatistics statistics = new StubBedStatistics();
    private final AwaitingReadyRecords records = new AwaitingReadyRecords(
            new StaticListableBeanFactory(Map.of("statistics", statistics)).getBeanProvider(BedStatisticsApi.class),
            new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE)));
    private final AwaitingReadyMetric average = new AwaitingReadyMetric(records);
    private final AwaitingReadyMaximumMetric maximum = new AwaitingReadyMaximumMetric(records);
    private final AwaitingReadyOverdueMetric overdue = new AwaitingReadyOverdueMetric(records);

    private static final Duration LIMIT = Duration.ofHours(4);

    private <R extends com.cambae.backend.m14.internal.metric.domain.MetricRecord> MetricValue value(
            com.cambae.backend.m14.internal.metric.domain.MetricSource<R> source, Instant from, Instant to) {
        Metric<R> metric = source.metric();
        return metric.compute(metric.included(
                source.records(ScopeFilter.FACILITY, from, to, ReportFilters.NONE), ReportScope.FACILITY::covers));
    }

    @Test
    void Q_390_startInPeriod_fullDuration_openUntilNow_overdueByDeadline() {
        Instant from = NOW.minus(Duration.ofDays(10));
        Instant to = NOW.minus(Duration.ofDays(5));
        // tầng 101: 2 giờ; 6 giờ, kết thúc sau cuối khoảng (vẫn tính đủ); tầng 102: bắt đầu trước khoảng (không tính)
        statistics.add(1, 101, from.plus(Duration.ofHours(1)), from.plus(Duration.ofHours(3)), LIMIT);
        statistics.add(2, 101, to.minus(Duration.ofHours(2)), to.plus(Duration.ofHours(4)), LIMIT);
        statistics.add(3, 102, from.minus(Duration.ofHours(1)), from.plus(Duration.ofHours(9)), LIMIT);

        MetricValue avg = value(average, from, to);
        MetricValue max = value(maximum, from, to);
        MetricValue over = value(overdue, from, to);

        assertThat(average.metric().code()).isEqualTo("BED.AWAITING_READY");
        assertThat(average.metric().group()).isEqualTo(MetricGroup.BED_AWAITING_READY);
        assertThat(maximum.metric().code()).isEqualTo("BED.AWAITING_READY_MAX");
        assertThat(overdue.metric().code()).isEqualTo("BED.AWAITING_READY_OVERDUE");
        assertThat(avg.value()).isEqualByComparingTo("240.0");
        assertThat(max.value()).isEqualByComparingTo("360");
        assertThat(over.value()).isEqualByComparingTo("1");
        assertThat(avg.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> b.value().intValue())
                .containsExactly(Tuple.tuple("101", 240));
        assertThat(statistics.asked).allSatisfy(a -> assertThat(a[1]).isEqualTo(NOW));
    }

    @Test
    void Q_390_openIntervalCountsUntilNow_andFuturePeriodIsEmpty() {
        Instant from = NOW.minus(Duration.ofDays(1));
        statistics.add(4, 101, NOW.minus(Duration.ofHours(5)), null, LIMIT);
        Metric<AwaitingReadyRecord> metric = maximum.metric();

        List<AwaitingReadyRecord> included = metric.included(
                maximum.records(ScopeFilter.FACILITY, from, NOW.plus(Duration.ofDays(1)), ReportFilters.NONE),
                ReportScope.FACILITY::covers);

        assertThat(included).singleElement().satisfies(r -> {
            assertThat(r.minutes()).isEqualByComparingTo("300");
            assertThat(r.overdue()).isTrue();
            assertThat(r.referenceTime()).isEqualTo(NOW.minus(Duration.ofHours(5)));
        });
        assertThat(value(overdue, NOW.plus(Duration.ofHours(1)), NOW.plus(Duration.ofDays(1)))
                        .value())
                .isEqualByComparingTo("0");
    }
}
