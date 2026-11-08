package com.eldercare.backend.staffing.internal.swap.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Khóa "một yêu cầu mở mỗi (ca, nhân viên)" ở {@code simple_record} loại {@code M09.OPEN_SHIFT_LOCK} (data-model mục
 * 0.1: {@code yeu_cau_ca_mo} → {@code khoa_duy_nhat}); khóa trùng thì 409; nhả khi yêu cầu vào trạng thái cuối hoặc ca
 * rời yêu cầu.
 */
public final class OpenShiftLock {

    public static final SharedKind<Void> KIND = SharedKind.of("M09.OPEN_SHIFT_LOCK", Void.class);

    public static String uniqueKey(long shiftId, long staffId) {
        return "M09.LOCK:" + shiftId + ":" + staffId;
    }

    /** Nghỉ theo ngày: khóa theo (nhân viên, ngày) vì chưa có ca để khóa. */
    public static String dayKey(long staffId, java.time.LocalDate date) {
        return "M09.LOCKDAY:" + staffId + ":" + date;
    }

    private OpenShiftLock() {}
}
