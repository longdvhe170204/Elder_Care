package com.cambae.backend.m14;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.BedStatisticsApi;
import com.cambae.backend.m03.api.dto.AwaitingReadyInterval;
import com.cambae.backend.m03.api.dto.BedStateOnDate;
import com.cambae.backend.m03.api.dto.BedStatus;
import com.cambae.backend.m03.api.dto.RoomIsolationView;
import com.cambae.backend.m14.internal.bed.service.BedOccupantsMetric;
import com.cambae.backend.m14.internal.bed.service.BedOccupantsMetric.OccupantRecord;
import com.cambae.backend.m14.internal.bed.service.BedStateRecords;
import com.cambae.backend.m14.internal.bed.service.BedStateRecords.BedStateRecord;
import com.cambae.backend.m14.internal.bed.service.BedStatusMetric;
import com.cambae.backend.m14.internal.bed.service.BedUsableMetric;
import com.cambae.backend.m14.internal.bed.service.IsolatedRoomsMetric;
import com.cambae.backend.m14.internal.bed.service.IsolatedRoomsMetric.IsolatedRoomRecord;
import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricValue;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters;
import com.cambae.backend.m14.internal.scope.domain.ReportScope;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.spi.BedAllocationApi;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
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
 * Chỉ tiêu giường (tên logic {@code GIUONG.TRANG_THAI}; 016 T007, T013, FR-020, FR-024, US3 #3): số giường theo trạng thái
 * của 003, tách hai loại Đang giữ chỗ; tổng giường có thể dùng không tính Không sử dụng, Tạm ngừng sử dụng (Q-246); số
 * phòng đang cách ly; số người theo tầng, phòng. Lúc xem là hiện tại, báo cáo là cuối khoảng (không quá lúc xem).
 */
class MetricBedStatusTest {

    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");

    /** Stub cục bộ của {@code BedStatisticsApi}: nhớ thời điểm được hỏi, trả dòng đặt sẵn. */
    static final class StubBedStatistics implements BedStatisticsApi {
        List<BedStateOnDate> states = List.of();
        List<RoomIsolationView> rooms = List.of();
        final List<Instant> askedAt = new ArrayList<>();

        @Override
        public Stream<BedStateOnDate> stateAt(ScopeFilter scope, Instant at) {
            askedAt.add(at);
            return states.stream()
                    .filter(s -> scope.facilityWide() || scope.floorAreaIds().contains(s.floorId()));
        }

        @Override
        public Stream<AwaitingReadyInterval> awaitingReady(ScopeFilter scope, Instant from, Instant to) {
            return Stream.empty();
        }

        @Override
        public Stream<RoomIsolationView> isolatedRooms(ScopeFilter scope, Instant at) {
            askedAt.add(at);
            return rooms.stream();
        }
    }

    private final StubBedStatistics statistics = new StubBedStatistics();
    private final FakeBedAllocations allocations = new FakeBedAllocations();
    private final BusinessTime time = new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE));

    private final BedStateRecords records = new BedStateRecords(
            new StaticListableBeanFactory(Map.of("statistics", statistics)).getBeanProvider(BedStatisticsApi.class),
            time);
    private final BedStatusMetric status = new BedStatusMetric(records);
    private final BedUsableMetric usable = new BedUsableMetric(records);
    private final IsolatedRoomsMetric isolated = new IsolatedRoomsMetric(
            new StaticListableBeanFactory(Map.of("statistics", statistics)).getBeanProvider(BedStatisticsApi.class),
            time);
    private final BedOccupantsMetric occupants = new BedOccupantsMetric(
            new StaticListableBeanFactory(Map.of("allocations", allocations)).getBeanProvider(BedAllocationApi.class),
            time);

    private long nextBed = 1;

    private void beds(int count, BedStatus state, String holdReason) {
        List<BedStateOnDate> all = new ArrayList<>(statistics.states);
        for (int i = 0; i < count; i++) {
            all.add(new BedStateOnDate(nextBed++, 101, state, NOW, 11, holdReason));
        }
        statistics.states = all;
    }

    /** US3 #3: 23 Đang sử dụng, 2 Đang giữ chỗ (hồ sơ chờ, người vắng), 1 Chờ sẵn sàng, 1 bảo trì, 1 Không sử dụng, 2 Trống. */
    private void floorOf30() {
        beds(23, BedStatus.OCCUPIED, null);
        beds(1, BedStatus.HELD, "WAITING_RECORD");
        beds(1, BedStatus.HELD, "ABSENT_RESIDENT");
        beds(1, BedStatus.AWAITING_READY, null);
        beds(1, BedStatus.UNDER_MAINTENANCE, null);
        beds(1, BedStatus.RETIRED, null);
        beds(2, BedStatus.EMPTY, null);
    }

    @Test
    void US3_3_countsByState_splitsHolds_usableExcludesRetiredAndSuspended() {
        floorOf30();
        beds(1, BedStatus.SUSPENDED, null);
        Metric<BedStateRecord> metric = status.metric();

        MetricValue byState = metric.compute(
                metric.included(status.current(ScopeFilter.FACILITY, null, null), ReportScope.FACILITY::covers));
        MetricValue usableValue = usable.metric()
                .compute(usable.metric()
                        .included(usable.current(ScopeFilter.FACILITY, null, null), ReportScope.FACILITY::covers));

        assertThat(metric.code()).isEqualTo("BED.STATUS");
        assertThat(metric.group()).isEqualTo(MetricGroup.BEDS);
        assertThat(byState.value()).isEqualByComparingTo("31");
        assertThat(byState.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> b.value().intValue())
                .containsExactly(
                        Tuple.tuple("AWAITING_READY", 1),
                        Tuple.tuple("EMPTY", 2),
                        Tuple.tuple("HELD_ABSENT_RESIDENT", 1),
                        Tuple.tuple("HELD_WAITING_RECORD", 1),
                        Tuple.tuple("OCCUPIED", 23),
                        Tuple.tuple("RETIRED", 1),
                        Tuple.tuple("SUSPENDED", 1),
                        Tuple.tuple("UNDER_MAINTENANCE", 1));
        assertThat(usable.metric().code()).isEqualTo("BED.USABLE");
        assertThat(usableValue.value()).isEqualByComparingTo("29");
        assertThat(statistics.askedAt).containsOnly(NOW);
    }

    @Test
    void FR_024_reportAtPastDay_readsEndOfPeriod_notLaterThanNow() {
        floorOf30();
        Instant from = NOW.minus(Duration.ofDays(10));
        Instant to = from.plus(Duration.ofDays(1));

        status.records(ScopeFilter.FACILITY, from, to, ReportFilters.NONE).toList();
        status.records(
                        ScopeFilter.FACILITY,
                        NOW.minus(Duration.ofHours(1)),
                        NOW.plus(Duration.ofDays(1)),
                        ReportFilters.NONE)
                .toList();

        assertThat(statistics.askedAt).containsExactly(to.minus(1, java.time.temporal.ChronoUnit.MICROS), NOW);
    }

    @Test
    void FR_020_isolatedRooms_andOccupantsByFloor() {
        statistics.rooms = List.of(
                new RoomIsolationView(11, 101, NOW.minus(Duration.ofDays(2)), null),
                new RoomIsolationView(12, 102, NOW.minus(Duration.ofDays(1)), null));
        allocations
                .allocate(1001, new BedAllocationView(1, 1, 11, 101))
                .allocate(1002, new BedAllocationView(2, 2, 11, 101))
                .allocate(1003, new BedAllocationView(3, 3, 21, 102));
        ScopeFilter floor = new ScopeFilter(false, java.util.Set.of(101L), java.util.Set.of(), java.util.Set.of());
        ReportScope scope = ReportScope.of(List.of(ReportScope.Grant.always(floor)));

        List<IsolatedRoomRecord> rooms = isolated.metric().included(isolated.current(floor, null, null), scope::covers);
        MetricValue people = occupants
                .metric()
                .compute(occupants
                        .metric()
                        .included(occupants.current(ScopeFilter.FACILITY, null, null), ReportScope.FACILITY::covers));
        List<OccupantRecord> onFloor = occupants.metric().included(occupants.current(floor, null, null), scope::covers);

        assertThat(isolated.metric().code()).isEqualTo("BED.ISOLATED_ROOMS");
        assertThat(rooms).extracting(IsolatedRoomRecord::roomId).containsExactly(11L);
        assertThat(occupants.metric().code()).isEqualTo("BED.OCCUPANTS");
        assertThat(people.value()).isEqualByComparingTo("3");
        assertThat(people.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> b.value().intValue())
                .containsExactly(Tuple.tuple("101", 2), Tuple.tuple("102", 1));
        assertThat(onFloor).extracting(OccupantRecord::residentId).containsExactlyInAnyOrder(1001L, 1002L);
    }
}
