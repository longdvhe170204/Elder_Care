package com.eldercare.backend.billing.internal.source.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Q-459 (FR-009): nội trú dài hạn giá tháng thu một khoản cho mỗi tháng có ngày tính phí (đơn vị {@code MONTH}, mã
 * {@code người:yyyy-MM}, ngày phát sinh là ngày tính phí đầu tiên). Tháng có ngày nội trú ngắn ngày, hợp đồng giá ngày,
 * hoặc đã có khoản theo ngày thì giữ nguồn theo ngày.
 */
public final class ResidencyMonthFee {
    public static final String UNIT = "MONTH";

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

    private ResidencyMonthFee() {}

    /**
     * @param whole nguồn theo ngày của cả tháng, theo thứ tự ngày, không rỗng
     * @param inRange phần của {@code whole} trong khoảng đang tra
     * @param monthItemId khoản cả tháng hiện hành, {@code null} khi chưa lập
     */
    public static List<CostSourceCandidate> select(
            YearMonth month, List<CostSourceCandidate> whole, List<CostSourceCandidate> inRange, Long monthItemId) {
        boolean longTerm = whole.stream()
                .allMatch(c -> "LONG_TERM_RESIDENTIAL".equals(c.facts().get("residenceType"))
                        && "MONTH".equals(c.facts().get("billingPeriod")));
        boolean hasDailyItems = whole.stream().anyMatch(c -> c.existingItemId() != null);
        var first =
                whole.stream().filter(c -> c.blockedCode() == null).findFirst().orElse(whole.getFirst());
        if (longTerm && !hasDailyItems)
            return List.of(monthSource(month, first, monthItemId, first.blockedCode(), first.blockedReason()));
        if (monthItemId == null) return inRange;
        // Khoản cả tháng đã lập mà tháng chuyển sang theo ngày: nguồn tháng hết tính phí (tính lại sẽ hủy khoản);
        // chưa hủy thì chưa lập khoản ngày để không thu hai lần.
        var result = new ArrayList<CostSourceCandidate>();
        result.add(
                monthSource(month, first, monthItemId, "NOT_A_BILLABLE_EVENT", "Tháng chuyển sang tính phí theo ngày"));
        inRange.forEach(c -> result.add(held(c)));
        return result;
    }

    private static CostSourceCandidate monthSource(
            YearMonth month, CostSourceCandidate first, Long itemId, String code, String reason) {
        var facts = new LinkedHashMap<>(first.facts());
        facts.put("month", month.toString());
        return new CostSourceCandidate(
                first.residentId(),
                first.sourceType(),
                first.residentId() + ":" + month,
                UNIT,
                first.date(),
                first.occurredAt(),
                first.serviceId(),
                first.costType(),
                "tháng",
                "Phí lưu trú tháng " + month.format(LABEL),
                BigDecimal.ONE,
                first.factor(),
                first.flags(),
                itemId,
                code,
                reason,
                facts);
    }

    private static CostSourceCandidate held(CostSourceCandidate c) {
        return new CostSourceCandidate(
                c.residentId(),
                c.sourceType(),
                c.sourceId(),
                c.billingUnit(),
                c.date(),
                c.occurredAt(),
                c.serviceId(),
                c.costType(),
                c.unit(),
                c.description(),
                c.quantity(),
                c.factor(),
                c.flags(),
                c.existingItemId(),
                "COST_ITEM_ALREADY_EXISTS",
                "Tháng đang có khoản phí cả tháng; hủy hoặc điều chỉnh khoản đó trước",
                c.facts());
    }
}
