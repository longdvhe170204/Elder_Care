package com.eldercare.backend.support.fake.roombedallocation;

import com.eldercare.backend.roombedallocation.api.*;
import com.eldercare.backend.roombedallocation.api.dto.*;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** Nguồn giường/cảnh báo phân bổ dùng chung của 003. */
public class FakeHandoverLocations implements BedStatusApi, BedStatisticsApi, AllocationHandoverApi {
    private final Map<Long, Long> floors = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Long, List<AwaitingReadyBedView>> beds = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Long, List<AllocationHandoverView>> alerts = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Long, String> states = new java.util.concurrent.ConcurrentHashMap<>();

    public void beds(long floor, List<AwaitingReadyBedView> values) {
        beds.put(floor, List.copyOf(values));
        values.forEach(bed -> floors.put(bed.bedId(), floor));
        values.forEach(b -> states.put(b.bedId(), "AWAITING_READINESS"));
    }

    public void state(long bed, String state) {
        states.put(bed, state);
    }

    public Map<Long, String> currentState(Collection<Long> ids, Instant at) {
        var result = new LinkedHashMap<Long, String>();
        for (long id : ids) if (states.containsKey(id)) result.put(id, states.get(id));
        return result;
    }

    public void alerts(long floor, List<AllocationHandoverView> values) {
        alerts.put(floor, List.copyOf(values));
    }

    public List<AwaitingReadyBedView> awaitingReadinessOverdue(long floor, Instant at) {
        return beds.getOrDefault(floor, List.of());
    }

    private BedStatus state(String value) {
        return switch (value) {
            case "AWAITING_READINESS" -> BedStatus.AWAITING_READY;
            case "READY", "AVAILABLE" -> BedStatus.EMPTY;
            default -> BedStatus.valueOf(value);
        };
    }

    public List<BedView> overdueAwaitingReady(long floor, Instant at) {
        return awaitingReadinessOverdue(floor, at).stream()
                .map(bed -> new BedView(bed.bedId(), bed.roomId(), bed.description(), BedStatus.AWAITING_READY))
                .toList();
    }

    public java.util.stream.Stream<BedStateOnDate> stateAt(
            com.eldercare.backend.administration.account.api.dto.ScopeFilter scope, Instant at) {
        return states.entrySet().stream()
                .filter(row -> scope.facilityWide() || scope.floorAreaIds().contains(floors.get(row.getKey())))
                .map(row -> new BedStateOnDate(
                        row.getKey(), floors.getOrDefault(row.getKey(), 0L), state(row.getValue()), at, 0L, null));
    }

    public java.util.stream.Stream<AwaitingReadyInterval> awaitingReady(
            com.eldercare.backend.administration.account.api.dto.ScopeFilter scope, Instant from, Instant to) {
        return beds.entrySet().stream()
                .filter(row -> scope.facilityWide() || scope.floorAreaIds().contains(row.getKey()))
                .flatMap(row -> row.getValue().stream()
                        .map(bed -> new AwaitingReadyInterval(
                                bed.bedId(),
                                row.getKey(),
                                bed.awaitingSince(),
                                to,
                                bed.awaitingSince(),
                                bed.awaitingSince())));
    }

    public java.util.stream.Stream<RoomIsolationView> isolatedRooms(
            com.eldercare.backend.administration.account.api.dto.ScopeFilter scope, Instant at) {
        return java.util.stream.Stream.empty();
    }

    public Map<BedStatus, Integer> countByState(
            com.eldercare.backend.administration.account.api.dto.ScopeFilter scope, Instant at) {
        var counts = new EnumMap<BedStatus, Integer>(BedStatus.class);
        stateAt(scope, at).forEach(row -> counts.merge(row.state(), 1, Integer::sum));
        return counts;
    }

    public int countAvailable(
            com.eldercare.backend.administration.account.api.dto.ScopeFilter scope,
            com.eldercare.backend.roombedallocation.api.spi.CareLevelCode level,
            ResidentGender gender,
            com.eldercare.backend.roombedallocation.api.spi.ResidencyType type,
            Instant at) {
        throw new UnsupportedOperationException("Chưa đặt dữ liệu sức chứa trong nguồn giả bàn giao");
    }

    public List<BedFlagView> flaggedBeds(com.eldercare.backend.administration.account.api.dto.ScopeFilter scope) {
        return List.of();
    }

    public Optional<Instant> emptyPeriodStarted(long bed) {
        return Optional.empty();
    }

    public List<AllocationHandoverView> atFloor(long floor, Instant at) {
        return alerts.getOrDefault(floor, List.of());
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @Bean
        @Primary
        FakeHandoverLocations fakeHandoverLocations() {
            return new FakeHandoverLocations();
        }
    }
}
