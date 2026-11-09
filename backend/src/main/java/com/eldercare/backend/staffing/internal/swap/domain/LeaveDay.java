package com.eldercare.backend.staffing.internal.swap.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.LocalDate;

/**
 * Nghỉ theo ngày đã duyệt ở {@code simple_record} loại {@code M09.LEAVE_DAY} (nhóm 3, khóa dòng ngay khi tạo; data-model
 * mục 5: {@code nghi_theo_ngay} → {@code su_kien_chung}); {@code UNIQUE (nhân viên, ngày)} qua khóa duy nhất.
 */
public record LeaveDay(long staffId, LocalDate date, long requestId) {

    public static final SharedKind<LeaveDay> KIND = SharedKind.of("M09.LEAVE_DAY", LeaveDay.class);

    public static String uniqueKey(long staffId, LocalDate date) {
        return "M09.LEAVEDAY:" + staffId + ":" + date;
    }
}
