package com.eldercare.backend.staffing.shift.internal.domain;

import com.eldercare.backend.common.Reason;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Nội dung sửa ca Nháp đúng NewShift của api-008. */
public record ShiftInput(
        long shiftTemplateId,
        LocalDate date,
        Instant startsAt,
        Instant endsAt,
        List<Member> staff,
        Long leadStaffId,
        boolean confirmWarnings,
        Reason reason) {
    public ShiftInput {
        staff = staff == null ? List.of() : List.copyOf(staff);
    }

    public record Member(long staffId, boolean onDutyDoctor) {}
}
