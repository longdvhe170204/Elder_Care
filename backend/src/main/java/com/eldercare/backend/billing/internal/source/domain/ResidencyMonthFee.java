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
        if (endsOnTermsOrByDeath(whole)) longTerm = false;
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

    /**
     * Q-459 (sửa 2026-11-27): tháng có mốc dừng tính phí (ngày sau ngày tính phí cuối nằm ngoài thời gian tính phí) tính
     * theo ngày khi người cao tuổi qua đời hoặc ngày cuối không sớm hơn ngày kết thúc hợp đồng; chấm dứt trước hạn vẫn
     * thu trọn tháng.
     */
    static boolean endsOnTermsOrByDeath(List<CostSourceCandidate> whole) {
        int last = -1;
        for (int i = 0; i < whole.size(); i++) if (whole.get(i).blockedCode() == null) last = i;
        if (last < 0 || last == whole.size() - 1) return false;
        var after = whole.get(last + 1);
        if (!"OUTSIDE_BILLING_PERIOD".equals(after.blockedCode())) return false;
        if (Boolean.TRUE.equals(after.facts().get("afterDeath"))) return true;
        Object end = whole.get(last).facts().get("contractEndDate");
        return end instanceof String s
                && !s.isBlank()
                && !whole.get(last).date().isBefore(java.time.LocalDate.parse(s));
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
