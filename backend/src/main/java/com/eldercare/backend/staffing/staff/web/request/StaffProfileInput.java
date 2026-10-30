package com.eldercare.backend.staffing.staff.web.request;

import com.eldercare.backend.staffing.staff.internal.domain.StaffInput;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/** Chỉ các trường mô tả của schema StaffProfile; trường chỉ đọc không tham gia lệnh. */
public record StaffProfileInput(
        @NotBlank String fullName,
        @NotBlank String position,
        String specialty,
        LocalDate startDate,
        String phoneNumber,
        String email,
        Long departmentId) {
    public StaffInput input() {
        return new StaffInput(fullName, position, specialty, startDate, phoneNumber, email, departmentId);
    }
}
