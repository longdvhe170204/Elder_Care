package com.eldercare.backend.care.api.spi;

import com.eldercare.backend.care.api.dto.CareShiftView;
import com.eldercare.backend.common.Role;
import java.time.*;
import java.util.*;

/** NhanSuCaPort: phần dữ liệu ca của T015; phần phân công/bàn giao nối cùng T021/T026. */
public interface CareShiftPort {
    boolean handoverConfirmed(long previousShiftId);

    List<com.eldercare.backend.care.api.dto.CareAssignmentView> assignments(long residentId, long shiftId);

    List<Long> assignedNurses(long residentId, Instant at);

    Optional<CareShiftView> shift(long shiftId);

    List<CareShiftView> publishedShifts(long floorAreaId, Instant from, Instant to);

    Optional<CareShiftView> shiftAt(long floorAreaId, Instant at);

    Optional<Long> shiftLead(long shiftId);

    boolean listedInShift(long staffId, Instant at);

    boolean qualified(long staffId, Set<String> certificates, LocalDate date);

    boolean hasQualifiedStaff(long shiftId, Role role, Set<String> certificates);
}
