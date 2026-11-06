package com.eldercare.backend.billing.internal.item.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** T005: nội dung lịch sử tính lại; người, thời điểm và căn cứ nằm ở cột chung của HistoryEntry. */
public record CostRecalculation(
        @Valid Calculation before,
        @Valid Calculation after,
        String cause,
        boolean resetToDraft,
        Map<String, Object> basisBefore,
        Map<String, Object> basisAfter,
        Map<String, Object> sourceBefore,
        Map<String, Object> sourceAfter) {
    public CostRecalculation(Calculation before, Calculation after, String cause, boolean resetToDraft) {
        this(before, after, cause, resetToDraft, Map.of(), Map.of(), Map.of(), Map.of());
    }

    public CostRecalculation {
        // Đọc được các dòng lịch sử cũ chưa có căn cứ/snapshot trước và sau.
        basisBefore = copy(basisBefore);
        basisAfter = copy(basisAfter);
        sourceBefore = copy(sourceBefore);
        sourceAfter = copy(sourceAfter);
    }

    private static Map<String, Object> copy(Map<String, Object> values) {
        return values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static final HistoryTrack<CostRecalculation> TRACK =
            new HistoryTrack<>("M11.COST_ITEM", "RECALCULATION", CostRecalculation.class);
    public static final AuditAction PRICE_RECALCULATED = new AuditAction("M11.COST_PRICE_RECALCULATED", false);

    /** Giữ số thập phân nguồn; đơn giá/số tiền có thể rỗng khi khoản còn thiếu đơn giá. */
    public record Calculation(
            @Digits(integer = 9, fraction = 3) BigDecimal quantity,
            @Digits(integer = 15, fraction = 0) BigDecimal unitPrice,
            @Digits(integer = 3, fraction = 2) BigDecimal factorPercent,
            @Digits(integer = 15, fraction = 0) BigDecimal amount) {}
}
