package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.admission.api.DayCareCalendarApi;
import com.eldercare.backend.admission.api.dto.DayCareAbsenceNoticeView;
import com.eldercare.backend.admission.api.dto.DayCareCalendarView;
import com.eldercare.backend.care.api.spi.DayCareCalendarPort;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * T029: {@link DayCareCalendarPort} đọc lịch bán trú của 004 ({@code DayCareCalendarApi}, module-interfaces 004 mục 1.6a).
 * Ngày nghỉ là {@code closureDay}. Báo vắng đúng hạn (trước CFG-M02-06, FR-013) và báo muộn (FR-014) lấy báo đầu tiên
 * theo thời điểm ghi của từng loại; mỗi ngày một khung lịch đến ({@link AttendanceSources#day}), nên báo theo buổi
 * ({@code sessionId}) cũng là báo cho ngày đó.
 */
@Component
class DayCareCalendarAdapter implements DayCareCalendarPort {
    private final DayCareCalendarApi calendar;

    DayCareCalendarAdapter(DayCareCalendarApi calendar) {
        this.calendar = calendar;
    }

    @Override
    public CalendarView onDate(long residentId, LocalDate date) {
        DayCareCalendarView day = calendar.onDate(residentId, date);
        return new CalendarView(day.closureDay(), first(day, true), first(day, false));
    }

    private static Long first(DayCareCalendarView day, boolean withinNoticePeriod) {
        return day.notices().stream()
                .filter(n -> n.withinNoticePeriod() == withinNoticePeriod)
                .map(DayCareAbsenceNoticeView::noticeId)
                .findFirst()
                .orElse(null);
    }
}
