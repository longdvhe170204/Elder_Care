package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.m02.internal.registration.domain.RegistrationType;

/**
 * Loại khai báo ({@code LoaiKhaiBao}; FR-001, FR-007; BR-M02-11, BR-M02-14, Q-215; research Z-03), hàm thuần: người được
 * xác nhận thường trú cùng xã/phường với viện luôn Thông báo lưu trú; ngược lại Đăng ký tạm trú khi tổng thời gian lưu
 * trú dự kiến từ CFG-M02-11 ngày trở lên hoặc không có ngày kết thúc, dưới ngưỡng là Thông báo lưu trú.
 */
public final class RegistrationTypeRule {

    private RegistrationTypeRule() {}

    /** @param thresholdDays giá trị CFG-M02-11 bên gọi đọc lúc lập khai báo */
    public static RegistrationType determine(
            ExpectedStayDuration stay, int thresholdDays, boolean localPermanentResident) {
        if (thresholdDays < 1) {
            throw new IllegalArgumentException("CFG-M02-11 phải từ 1 ngày: " + thresholdDays);
        }
        if (localPermanentResident) {
            return RegistrationType.STAY_NOTIFICATION;
        }
        var days = stay.days();
        return days.isEmpty() || days.getAsLong() >= thresholdDays
                ? RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION
                : RegistrationType.STAY_NOTIFICATION;
    }
}
