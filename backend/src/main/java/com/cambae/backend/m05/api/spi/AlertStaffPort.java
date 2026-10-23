package com.cambae.backend.m05.api.spi;

import com.cambae.backend.m05.api.dto.AlertShiftView;
import java.time.Instant;
import java.util.*;

/** NhanSuCanhBaoPort: phần ca của T015; phân công, truy vết và bàn giao nối cùng story nguồn. */
public interface AlertStaffPort {
    Optional<AlertShiftView> shiftAt(long floorAreaId, Instant at);

    Optional<AlertShiftView> nextShift(long floorAreaId, Instant at);

    List<Long> onDutyDoctors(Instant at);

    Optional<Long> assignedFloorManager(long floorAreaId, Instant at);

    boolean listedInShift(long staffId, long shiftId);

    boolean inProgressShiftMember(long staffId, Instant at);

    boolean onShift(long staffId, Instant at);

    Optional<Long> floorDuringShift(long staffId, Instant at);

    List<Long> floorsWithShift(Instant at);
}
