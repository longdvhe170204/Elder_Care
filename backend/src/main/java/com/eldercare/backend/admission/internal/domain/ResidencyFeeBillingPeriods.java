package com.eldercare.backend.admission.internal.domain;

import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.util.Set;

/**
 * Kỳ tính phí lưu trú được phép theo loại lưu trú (Q-462, FR-019): nội trú dài hạn chỉ giá tháng; nội trú ngắn ngày giá
 * tháng hoặc giá ngày; bán trú giá tháng hoặc giá buổi.
 */
public final class ResidencyFeeBillingPeriods {

    public static final String NOT_ALLOWED = "BILLING_PERIOD_NOT_ALLOWED";

    private ResidencyFeeBillingPeriods() {}

    public static Set<BillingPeriod> allowed(ResidenceType type) {
        return switch (type) {
            case LONG_TERM_RESIDENTIAL -> Set.of(BillingPeriod.MONTH);
            case SHORT_TERM_RESIDENTIAL -> Set.of(BillingPeriod.MONTH, BillingPeriod.DAY);
            case DAY_CARE -> Set.of(BillingPeriod.MONTH, BillingPeriod.SESSION);
        };
    }

    public static boolean isAllowed(ResidenceType type, BillingPeriod period) {
        return allowed(type).contains(period);
    }

    /** Câu báo lỗi chung cho lập, sửa hợp đồng và yêu cầu đổi loại lưu trú. */
    public static String message(ResidenceType type, BillingPeriod period) {
        return "Kỳ tính phí " + period + " không dùng được cho loại lưu trú " + type
                + " (Q-462: dài hạn MONTH; ngắn ngày MONTH, DAY; bán trú MONTH, SESSION)";
    }
}
