package com.eldercare.backend.care.api.spi;

import com.eldercare.backend.common.Role;
import java.time.*;
import java.util.*;

/** Nguồn ca và phân công của 008, contracts 005 mục 2.1. */
public interface ShiftStaffPort {
    Optional<ShiftView> shift(long shiftId);

    List<ShiftView> publishedShifts(long floorAreaId, Instant from, Instant to);

    Optional<ShiftView> shiftContaining(long floorAreaId, Instant at);

    Optional<Long> shiftLead(long shiftId);

    List<CareAssignmentView> assignments(long residentId, long shiftId);

    List<Long> responsibleNurses(long residentId, Instant at);

    boolean onShift(long staffId, Instant at);

    boolean qualified(long staffId, Set<String> certificates, LocalDate date);

    boolean hasQualifiedStaff(long shiftId, Role role, Set<String> certificates);

    boolean handoverConfirmed(long previousShiftId);

    HandoverTasksView handoverTasks(long staffId, long shiftId);

    record ShiftView(long id, long floorAreaId, Instant startsAt, Instant endsAt, boolean published) {}

    record CareAssignmentView(long staffId, Role role, boolean primary) {}
    /** Hai việc checklist điều dưỡng: nhận đầu ca và lập cuối ca (FR-029). */
    record HandoverTasksView(boolean receiveAtStart, boolean prepareAtEnd) {}
}
