package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.roombedallocation.api.dto.AwaitingReadyInterval;
import com.eldercare.backend.roombedallocation.api.dto.BedStateOnDate;
import com.eldercare.backend.roombedallocation.api.dto.RoomIsolationView;
import java.time.Instant;
import java.util.stream.Stream;

/** Hợp đồng 003 BedStatisticsApi (T003); bản cài thuộc user story tương ứng. */
public interface BedStatisticsApi {
    Stream<BedStateOnDate> stateAt(ScopeFilter scope, Instant at);

    Stream<AwaitingReadyInterval> awaitingReady(ScopeFilter scope, Instant from, Instant to);

    Stream<RoomIsolationView> isolatedRooms(ScopeFilter scope, Instant at);
}
