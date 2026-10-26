package com.cambae.backend.m09.api.spi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 014 cài; nhiệm vụ chuyến đi cho 002 FR-019 và nguồn bàn giao 008 FR-039 (e). */
public interface TripDutyPort {

    Optional<Long> groupLeader(long tripId);

    List<Long> companions(long tripId);

    List<TripInShiftView> inShift(long floorAreaId, Instant from, Instant to);
}
