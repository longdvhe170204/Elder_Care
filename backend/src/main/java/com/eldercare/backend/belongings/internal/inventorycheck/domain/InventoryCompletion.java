package com.eldercare.backend.belongings.internal.inventorycheck.domain;

import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommand;
import java.util.List;

/**
 * Kết quả kiểm kê khi hoàn thành phiếu ({@code InventoryCheckResult} của api-013; FR-028b): kết quả từng đồ trong phiếu,
 * đồ Thất lạc tìm thấy tại vị trí, đồ không rõ chủ.
 */
public record InventoryCompletion(
        List<LineResult> lines, List<Long> found, List<InventoryCheck.UnknownOwnerItem> unknownOwnerItems) {

    public InventoryCompletion {
        lines = lines == null ? List.of() : List.copyOf(lines);
        found = found == null ? List.of() : List.copyOf(found);
        unknownOwnerItems = unknownOwnerItems == null ? List.of() : List.copyOf(unknownOwnerItems);
    }

    /**
     * Kết quả một đồ: {@code MATCHED}, {@code CONDITION_MISMATCH} (kèm mức mới; {@code recordDamage} chuyển thành Ghi hư
     * hỏng theo FR-015), {@code NOT_FOUND} (ghi Báo thất lạc trong cùng lần). {@code incident} là lựa chọn sự cố của
     * người ghi cho Báo thất lạc, Ghi hư hỏng (FR-021).
     */
    public record LineResult(
            long belongingId,
            String result,
            Long newConditionLevelId,
            String newConditionDescription,
            boolean recordDamage,
            List<Long> photoFileIds,
            HandoverCommand.IncidentChoice incident) {

        public LineResult {
            photoFileIds = photoFileIds == null ? List.of() : List.copyOf(photoFileIds);
        }
    }
}
