package com.cambae.backend.m09.api;

import com.cambae.backend.m09.api.dto.ShiftView;
import com.cambae.backend.m09.api.dto.StaffPublicView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Hợp đồng đọc ca theo 008 mục 1.3; bản cài truy vấn ở T015. */
public interface ShiftApi {
    Optional<ShiftView> currentShift(long floorAreaId, Instant at);

    List<ShiftView> staffShifts(long staffId, Instant from, Instant to);

    boolean onShift(long staffId, Instant at);

    Optional<StaffPublicView> staff(long staffId);
}
