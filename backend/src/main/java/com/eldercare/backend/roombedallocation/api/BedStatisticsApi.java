package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.roombedallocation.api.dto.AwaitingReadyInterval;
import com.eldercare.backend.roombedallocation.api.dto.BedStateOnDate;
import com.eldercare.backend.roombedallocation.api.dto.RoomIsolationView;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

/** Hợp đồng 003 BedStatisticsApi (T003); bản cài thuộc user story tương ứng. */
public interface BedStatisticsApi {
    Stream<BedStateOnDate> stateAt(ScopeFilter scope, Instant at);

    Stream<AwaitingReadyInterval> awaitingReady(ScopeFilter scope, Instant from, Instant to);

    Stream<RoomIsolationView> isolatedRooms(ScopeFilter scope, Instant at);

    /**
     * 016 {@code BED.EMPTY_AWAITING_TRANSFER} (FR-020, Q-235; "Đổi api" duyệt 2026-11-08): id giường Trống tại {@code at}
     * mà tài sản mang dấu "chờ chuyển người" tại {@code at} (019 FR-015), cùng nguồn với
     * {@link BedStatusApi#countEmptyAwaitingTransfer}; thiếu nguồn dấu thì 503.
     */
    Set<Long> emptyAwaitingTransferBedIds(ScopeFilter scope, Instant at);
}
