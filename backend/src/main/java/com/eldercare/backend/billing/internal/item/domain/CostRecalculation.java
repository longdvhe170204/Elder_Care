package com.eldercare.backend.billing.internal.item.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

/** T005: nội dung lịch sử tính lại; người, thời điểm và căn cứ nằm ở cột chung của HistoryEntry. */
public record CostRecalculation(
        @Valid Calculation before, @Valid Calculation after, String cause, boolean resetToDraft) {
    public static final HistoryTrack<CostRecalculation> TRACK =
            new HistoryTrack<>("M11.COST_ITEM", "RECALCULATION", CostRecalculation.class);

    /** Giữ số thập phân nguồn; đơn giá/số tiền có thể rỗng khi khoản còn thiếu đơn giá. */
    public record Calculation(
            @Digits(integer = 8, fraction = 2) BigDecimal quantity,
            @Digits(integer = 15, fraction = 0) BigDecimal unitPrice,
            @Digits(integer = 3, fraction = 2) BigDecimal factorPercent,
            @Digits(integer = 15, fraction = 0) BigDecimal amount) {}
}
