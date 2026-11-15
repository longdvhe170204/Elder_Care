package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.TripInShiftView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 014 cung cấp cho adapter TripDutyPort của 008; giữ API staffing cũ. */
public interface TripDutyReadApi {
    Optional<Long> groupLeader(long tripId);

    List<Long> companions(long tripId);

    /**
     * 002 FR-031 (Q-161, 002 T049): người cao tuổi đã rời viện (chưa về) của các chuyến Đang diễn ra mà nhân viên là Trưởng
     * đoàn hoặc Người đi cùng.
     */
    List<Long> tripResidentsOf(long staffId);

    List<TripInShiftView> inShift(long floorAreaId, Instant from, Instant to);
}
