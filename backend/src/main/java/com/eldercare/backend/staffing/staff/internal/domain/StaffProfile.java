package com.eldercare.backend.staffing.staff.internal.domain;

import java.time.LocalDate;

/** Dữ liệu hồ sơ nội bộ; phần trạng thái chỉ đổi qua lệnh riêng. */
public record StaffProfile(
        long id,
        String fullName,
        String position,
        String specialty,
        LocalDate startDate,
        String phoneNumber,
        String email,
        String status,
        Long accountId,
        String source,
        Long importBatchId,
        Long departmentId,
        int version) {}
