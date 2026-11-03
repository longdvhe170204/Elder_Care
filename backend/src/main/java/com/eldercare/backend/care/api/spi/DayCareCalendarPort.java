package com.eldercare.backend.care.api.spi;

import java.time.LocalDate;

/** Nguồn 004 đã duyệt Phase5: không tự coi nguồn thiếu là không nghỉ hoặc không báo vắng. */
public interface DayCareCalendarPort {
    CalendarView onDate(long residentId, LocalDate date);

    record CalendarView(boolean holiday, Long timelyAbsenceId, Long lateAbsenceId) {}
}
