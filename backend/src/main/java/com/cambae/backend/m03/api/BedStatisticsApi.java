package com.cambae.backend.m03.api;

import com.cambae.backend.m03.api.dto.AwaitingReadyInterval;
import com.cambae.backend.m03.api.dto.BedStateOnDate;
import com.cambae.backend.m03.api.dto.RoomIsolationView;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.util.stream.Stream;

/** Hợp đồng 003 BedStatisticsApi (T003); bản cài thuộc user story tương ứng. */
public interface BedStatisticsApi {
    Stream<BedStateOnDate> stateAt(ScopeFilter scope, Instant at);

    Stream<AwaitingReadyInterval> awaitingReady(ScopeFilter scope, Instant from, Instant to);

    Stream<RoomIsolationView> isolatedRooms(ScopeFilter scope, Instant at);
}
