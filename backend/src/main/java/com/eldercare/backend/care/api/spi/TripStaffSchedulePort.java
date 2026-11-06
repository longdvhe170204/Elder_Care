package com.eldercare.backend.care.api.spi;

import java.time.Instant;

/** 008 cài: thành viên thực tế của ca đã công bố, còn có mặt, có khoảng ca chồng chuyến. */
public interface TripStaffSchedulePort {
    boolean hasOverlappingPublishedShift(long staffId, Instant from, Instant to);
}
