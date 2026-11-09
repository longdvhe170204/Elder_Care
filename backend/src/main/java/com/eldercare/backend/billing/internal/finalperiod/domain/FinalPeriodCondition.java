package com.eldercare.backend.billing.internal.finalperiod.domain;

import java.time.LocalDate;
import java.util.*;

/** FR-034a: chỉ khoản ảnh hưởng kết thúc mới chặn điều kiện ngoài phần ngày bắt buộc. */
public final class FinalPeriodCondition {
    private FinalPeriodCondition() {}

    public static List<String> reasons(
            boolean finalized, Set<LocalDate> required, Set<LocalDate> covered, boolean pendingTerminationAdjustment) {
        var reasons = new ArrayList<String>();
        if (!finalized) reasons.add("Bảng kỳ cuối chưa chốt");
        if (!covered.containsAll(required)) reasons.add("Còn ngày lưu trú chưa có khoản phí thuộc bảng đã chốt");
        if (pendingTerminationAdjustment) reasons.add("Còn khoản ảnh hưởng kết thúc lưu trú chưa chốt");
        return List.copyOf(reasons);
    }
}
