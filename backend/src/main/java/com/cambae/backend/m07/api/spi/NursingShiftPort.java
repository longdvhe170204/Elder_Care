package com.cambae.backend.m07.api.spi;

import com.cambae.backend.m07.api.dto.NursingShiftView;
import java.time.Instant;
import java.util.*;

/** DieuDuongCaPort: phần ca của T015; phân công và xác nhận bàn giao nối ở T021/T026. */
public interface NursingShiftPort {
    Optional<Long> assignedTo(long residentId, Instant at);

    Optional<NursingShiftView> shiftAt(long floorAreaId, Instant at);

    List<Long> nursesWithShift(long floorAreaId, Instant at);

    boolean inProgressShiftMember(long staffId, Instant at);
}
