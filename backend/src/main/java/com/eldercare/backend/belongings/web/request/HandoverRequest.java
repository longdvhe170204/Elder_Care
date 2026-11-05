package com.eldercare.backend.belongings.web.request;

import com.eldercare.backend.belongings.internal.belonging.domain.ConditionHandling;
import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommand;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Thân {@code HandoverCommand} (api-013; FR-011 → FR-015). {@code quantity} của từng đồ là số lượng, hoặc số tiền với
 * tiền mặt (FR-012).
 */
public record HandoverRequest(
        HandoverType type,
        List<ItemRequest> items,
        BelongingIntakeRequest.GiverRequest giver,
        Long recipientStaffId,
        Long storageLocationId,
        OffsetDateTime at,
        String reason,
        Boolean holderAbsent,
        ConditionHandling conditionHandling,
        OffsetDateTime discoveredAt) {

    /** Một đồ trong lệnh. */
    public record ItemRequest(
            Long belongingId,
            Integer version,
            BigDecimal quantity,
            BigDecimal countedQuantity,
            BelongingIntakeRequest.ItemConditionRequest condition,
            List<Long> photoFileIds,
            IncidentOptionRequest incident) {}

    /** {@code IncidentOption}; Phase 4 chỉ nhận {@code NONE} (US4 cài {@code NEW}, {@code LINK}). */
    public record IncidentOptionRequest(String method, Long incidentId) {}

    /** Thiếu trường bắt buộc của hợp đồng thì {@code null} (controller trả 422). */
    public HandoverCommand toCommand() {
        if (type == null || items == null || items.isEmpty()) {
            return null;
        }
        List<HandoverCommand.Item> mapped = new java.util.ArrayList<>();
        for (ItemRequest item : items) {
            if (item == null || item.belongingId() == null || item.version() == null) {
                return null;
            }
            mapped.add(new HandoverCommand.Item(
                    item.belongingId(),
                    item.version(),
                    item.quantity(),
                    item.countedQuantity(),
                    item.condition() == null ? null : item.condition().levelId(),
                    item.condition() == null ? null : item.condition().description(),
                    item.photoFileIds(),
                    item.incident() == null ? null : item.incident().method()));
        }
        GiverType giverType = giver == null ? null : giver.type();
        return new HandoverCommand(
                type,
                mapped,
                giverType,
                giver == null ? null : giver.id(),
                recipientStaffId,
                storageLocationId,
                at == null ? null : at.toInstant(),
                reason,
                Boolean.TRUE.equals(holderAbsent),
                conditionHandling,
                discoveredAt == null ? null : discoveredAt.toInstant());
    }

    public List<Long> belongingIds() {
        return items.stream().map(ItemRequest::belongingId).toList();
    }
}
