package com.cambae.backend.m03.api.spi;

import com.cambae.backend.m03.api.dto.DayCareSession;
import com.cambae.backend.m03.api.dto.SessionDate;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 004 cài; đếm lịch đăng ký theo khung buổi và kiểm lịch tương lai (003 FR-038, FR-040). */
public interface DayCareAttendanceCounter {
    Map<SessionDate, Integer> countFrom(LocalDate fromDate, List<DayCareSession> sessions);

    boolean hasFutureSchedule(LocalDate fromDate);
}
