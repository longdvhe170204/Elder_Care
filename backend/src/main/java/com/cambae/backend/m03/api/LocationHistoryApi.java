package com.cambae.backend.m03.api;

import com.cambae.backend.m03.api.dto.*;
import java.time.*;
import java.util.List;

/** LichSuViTriApi: chỉ phần tra cứu cần cho phân công và truy vết 008. */
public interface LocationHistoryApi {
    /** Bộ lọc vị trí null: toàn viện (ViTriLoc không đặt giới hạn). */
    List<LocationStay> atLocation(LocationRef location, Instant at);

    List<LocationStay> inDay(LocationRef location, LocalDate day);

    List<LocationStay> byResident(long residentId, Instant from, Instant to);
}
