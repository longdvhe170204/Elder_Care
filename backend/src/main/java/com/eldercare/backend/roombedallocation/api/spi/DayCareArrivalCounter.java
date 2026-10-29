package com.eldercare.backend.roombedallocation.api.spi;

import com.eldercare.backend.roombedallocation.api.dto.DayCareSession;
import com.eldercare.backend.roombedallocation.api.dto.SessionDate;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Đếm lịch đến do 004 cài, FR-040; không tạo lịch hay bản ghi bán trú. */
public interface DayCareArrivalCounter {
    Map<SessionDate, Integer> countFrom(LocalDate fromDate, List<DayCareSession> sessions);

    boolean hasFutureArrivals(LocalDate fromDate);
}
