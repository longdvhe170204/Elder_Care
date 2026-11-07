package com.eldercare.backend.belongings.internal.inventorycheck.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Nội dung phiếu kiểm kê đồ có giá trị ({@code phieu_kiem_ke} gộp vào {@code simple_record} loại {@code
 * M12.INVENTORY_CHECK} theo R-15; 013 T025, data-model mục 7, FR-028a, FR-028b). Một phiếu cho một vị trí lưu giữ trong
 * một kỳ ({@code unique_key = vị trí:kỳ}); trạng thái Chờ kiểm kê → Hoàn thành hoặc Đã hủy (khóa dòng khi chốt).
 */
public record InventoryCheck(
        long storageLocationId,
        String periodCode,
        LocalDate dueDate,
        boolean late,
        List<Line> lines,
        List<UnknownOwnerItem> unknownOwnerItems,
        Long completedBy,
        Instant completedAt,
        String cancelReason) {

    public static final SharedKind<InventoryCheck> KIND = SharedKind.of("M12.INVENTORY_CHECK", InventoryCheck.class);

    public static final String WAITING = "WAITING_CHECK";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    public InventoryCheck {
        lines = lines == null ? List.of() : List.copyOf(lines);
        unknownOwnerItems = unknownOwnerItems == null ? List.of() : List.copyOf(unknownOwnerItems);
    }

    /** Kết quả của một dòng: {@code MATCHED}, {@code CONDITION_MISMATCH}, {@code NOT_FOUND}, {@code DROPPED}. */
    public static final String MATCHED = "MATCHED";

    public static final String CONDITION_MISMATCH = "CONDITION_MISMATCH";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String DROPPED = "DROPPED";

    /**
     * Một đồ trong phiếu: số lượng (hoặc số tiền), mức tình trạng và ảnh lúc lập; kết quả, mức mới, ghi chú và mã bản ghi
     * bàn giao sinh ra (trống với Đúng và Lệch tình trạng không ghi hư hỏng) khi Hoàn thành.
     */
    public record Line(
            long belongingId,
            long residentId,
            BigDecimal amountAtCreation,
            long conditionLevelIdAtCreation,
            String result,
            Long newConditionLevelId,
            String note,
            Long handoverId) {

        public Line completed(String result, Long newConditionLevelId, String note, Long handoverId) {
            return new Line(
                    belongingId,
                    residentId,
                    amountAtCreation,
                    conditionLevelIdAtCreation,
                    result,
                    newConditionLevelId,
                    note,
                    handoverId);
        }
    }

    /** Đồ thấy ở vị trí mà không khớp đồ gửi nào (FR-028b): không tạo đồ gửi. */
    public record UnknownOwnerItem(String description, List<Long> photoFileIds) {

        public UnknownOwnerItem {
            photoFileIds = photoFileIds == null ? List.of() : List.copyOf(photoFileIds);
        }
    }
}
