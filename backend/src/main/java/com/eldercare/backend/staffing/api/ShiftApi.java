package com.eldercare.backend.staffing.api;

import com.eldercare.backend.staffing.api.dto.ShiftView;
import com.eldercare.backend.staffing.api.dto.StaffPublicView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Hợp đồng đọc ca theo 008 mục 1.3; bản cài truy vấn ở T015. */
public interface ShiftApi {
    Optional<ShiftView> currentShift(long floorAreaId, Instant at);

    List<ShiftView> staffShifts(long staffId, Instant from, Instant to);

    boolean onShift(long staffId, Instant at);

    Optional<StaffPublicView> staff(long staffId);

    /**
     * Nhân viên đang có mặt trong ca đang diễn ra của tầng/khu vực (đã trừ vắng ca, rời ca), cho nhóm người nhận
     * {@code OnShiftStaff} của 009 FR-007 (a) (luật "Đổi api", chủ dự án duyệt 2026-10-26).
     */
    List<Long> staffOnShift(long floorAreaId, Instant at);

    /** Bác sĩ trực đang có mặt trong ca tại thời điểm {@code at} (FR-023; 009 FR-007 (a) nhóm {@code OnDutyDoctor}). */
    List<Long> onDutyDoctors(Instant at);
}
