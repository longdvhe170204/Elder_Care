package com.eldercare.backend.staffing.internal.duty.service;

import com.eldercare.backend.care.api.spi.TripStaffSchedulePort;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Cài {@link TripStaffSchedulePort} (014 FR-034; 008 T043b, 2026-11-11, A thay E): nhân viên là thành viên có mặt của ca
 * đã công bố (không Nháp, Hủy; không nghỉ có duyệt, chưa vắng, rút hay rời) trong khoảng chồng thời gian chuyến; cùng
 * nguồn {@link ShiftQueries#staffShifts}.
 */
@Component
class TripStaffSchedules implements TripStaffSchedulePort {

    private final ShiftQueries shifts;

    TripStaffSchedules(ShiftQueries shifts) {
        this.shifts = shifts;
    }

    @Override
    public boolean hasOverlappingPublishedShift(long staffId, Instant from, Instant to) {
        return !shifts.staffShifts(staffId, from, to).isEmpty();
    }
}
