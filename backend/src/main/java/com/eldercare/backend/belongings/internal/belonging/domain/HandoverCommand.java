package com.eldercare.backend.belongings.internal.belonging.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Lệnh bàn giao trên một hoặc nhiều đồ của cùng người cao tuổi ({@code LenhBanGiao}; api-013 {@code HandoverCommand}).
 * {@code at} trống là lúc lưu; người giao trống là người giữ hiện tại khi người cao tuổi giữ, ngược lại là người thực hiện.
 */
public record HandoverCommand(
        HandoverType type,
        List<Item> items,
        GiverType giverType,
        Long giverId,
        Long recipientStaffId,
        Long storageLocationId,
        Instant at,
        String reason,
        boolean holderAbsent,
        ConditionHandling conditionHandling,
        Instant discoveredAt) {

    public HandoverCommand {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * Một đồ trong lệnh: số lượng (hoặc số tiền) bàn giao, số đếm được, tình trạng, ảnh; {@code incidentMethod} của
     * {@code IncidentOption} ({@code NEW}, {@code LINK}, {@code NONE}).
     */
    public record Item(
            long belongingId,
            int version,
            BigDecimal amount,
            BigDecimal countedAmount,
            Long conditionLevelId,
            String conditionDescription,
            List<Long> photoFileIds,
            String incidentMethod) {

        public Item {
            photoFileIds = photoFileIds == null ? List.of() : List.copyOf(photoFileIds);
        }
    }
}
