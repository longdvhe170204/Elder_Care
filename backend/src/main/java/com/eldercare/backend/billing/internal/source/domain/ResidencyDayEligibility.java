package com.eldercare.backend.billing.internal.source.domain;

import com.eldercare.backend.billing.internal.pricing.domain.BillingPeriod;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/** FR-007/011, Q-395: dùng dữ liệu/mốc thời gian caller đã đọc, không tự truy vấn hoặc lập khoản. */
public record ResidencyDayEligibility(boolean allowed, String errorCode, String reason, boolean createdInAdvance) {
    public static ResidencyDayEligibility evaluate(
            BillingPeriod period, LocalDate sourceDay, boolean bedReleased, LocalDate today,
            boolean finalStatement, LocalDate finalThrough, ZoneId zone) {
        Objects.requireNonNull(period);
        Objects.requireNonNull(sourceDay);
        Objects.requireNonNull(today);
        Objects.requireNonNull(zone);
        if (!period.residential() || !period.containsDay(sourceDay, zone)) {
            return rejected("OUTSIDE_BILLING_PERIOD", "Ngày nguồn nằm ngoài thời gian tính phí lưu trú.");
        }
        if (bedReleased) {
            return rejected("CONDITION_NOT_MET", "Không lập phí lưu trú từ ngày giải phóng giường.");
        }
        boolean advance = !sourceDay.isBefore(today);
        if (advance && (!finalStatement || finalThrough == null || sourceDay.isAfter(finalThrough))) {
            return rejected("DAY_NOT_ENDED", "Ngày lưu trú chưa kết thúc và không thuộc phạm vi lập trước của kỳ cuối.");
        }
        return new ResidencyDayEligibility(true, null, null, advance);
    }

    private static ResidencyDayEligibility rejected(String code, String reason) {
        return new ResidencyDayEligibility(false, code, reason, false);
    }
}
