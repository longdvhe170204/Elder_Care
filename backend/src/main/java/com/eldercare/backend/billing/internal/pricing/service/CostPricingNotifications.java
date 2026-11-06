package com.eldercare.backend.billing.internal.pricing.service;

import com.eldercare.backend.billing.internal.item.domain.CostPricingSnapshot;
import com.eldercare.backend.billing.internal.item.domain.RepriceableCost;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** FR-004/012: không đưa tên thuốc hoặc giá trị sức khỏe vào thông báo tính lại tài chính. */
public final class CostPricingNotifications {
    private CostPricingNotifications() {}

    /** Caller của T011 ghép yêu cầu này vào CostItemChanged cùng transaction lập khoản thiếu giá. */
    public static List<NotificationRequest> missingPrice(long residentId, long itemId, Instant at) {
        String text = "Khoản chi phí chưa xác định được đơn giá. Cần bổ sung căn cứ đơn giá phù hợp.";
        return List.of(new NotificationRequest(
                "010",
                "COST_MISSING_UNIT_PRICE",
                SourceGroup.OTHER,
                new SourceRecord("M11.COST_ITEM", Long.toString(itemId)),
                "M11.MISSING_PRICE:" + itemId,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                List.of(new ContentPart(InformationType.COSTS, text)),
                Optional.of("Có khoản chi phí cần bổ sung đơn giá."),
                Optional.of("Chi phí thiếu đơn giá"),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()));
    }

    public static List<NotificationRequest> forChange(
            RepriceableCost row, CostPricingSnapshot next, String key, Instant at) {
        boolean resolved = row.flags().contains("MISSING_UNIT_PRICE")
                && !next.pricingFlags().contains("MISSING_UNIT_PRICE");
        boolean reset = !row.status().equals("DRAFT") || row.statementStatus().equals("AWAITING_FINALIZATION");
        if (!resolved && !reset) return List.of();
        String text = reset
                ? "Khoản chi phí đã về Nháp do căn cứ tính giá thay đổi. Cần kiểm tra lại."
                : "Khoản chi phí đã được bổ sung đơn giá. Cần kiểm tra lại.";
        return List.of(new NotificationRequest(
                "010",
                "COST_PRICE_RECALCULATED",
                SourceGroup.OTHER,
                new SourceRecord("M11.COST_ITEM", Long.toString(row.id())),
                key,
                Optional.of(NotificationLevel.LOW),
                Optional.of(row.residentId()),
                Optional.empty(),
                List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                List.of(new ContentPart(InformationType.COSTS, text)),
                Optional.of("Có cập nhật chi phí cần kiểm tra."),
                Optional.of("Cập nhật chi phí"),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
