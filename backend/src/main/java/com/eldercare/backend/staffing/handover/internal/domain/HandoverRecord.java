package com.eldercare.backend.staffing.handover.internal.domain;

import java.time.Instant;

/** Trạng thái nhóm 2, nội dung nhóm 3 chốt theo finalizedAt. */
public record HandoverRecord(
        long id,
        long shiftId,
        Long nextShiftId,
        long handoverStaffId,
        Long createdBy,
        String status,
        Instant createdAt,
        Instant finalizedAt,
        Instant confirmedAt,
        long version) {}
