package com.eldercare.backend.care.internal.qualitycheck.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * 014 T024 (research L-10; R-15): danh sách kiểm tra ({@code danh_sach_kiem_tra} → {@code simple_record}
 * {@code M04.QUALITY_CHECKLIST}, khóa duy nhất ca:tầng) và mục kiểm tra ({@code muc_kiem_tra} → {@code record_event}
 * {@code M04.QUALITY_CHECK_ITEM}, nháp tới khi có kết quả, xác nhận thì thành nhóm 3; khóa duy nhất theo công việc).
 */
public final class QualityCheckKinds {
    private QualityCheckKinds() {}

    public static final SharedKind<Checklist> CHECKLISTS = SharedKind.of("M04.QUALITY_CHECKLIST", Checklist.class);
    public static final SharedKind<Item> ITEMS = SharedKind.of("M04.QUALITY_CHECK_ITEM", Item.class);

    public static final String PENDING = "PENDING";
    public static final String PASS = "PASS";
    public static final String FAIL = "FAIL";
    public static final String NOT_CHECKABLE = "NOT_CHECKABLE";
    public static final String OVERDUE = "OVERDUE";

    /** Vai trò kiểm tra (FR-065, FR-066). */
    public static final String FLOOR_MANAGER = "FLOOR_MANAGER";

    public static final String SHIFT_LEAD = "SHIFT_LEAD";

    public record Checklist(
            long shiftId,
            long floorAreaId,
            long createdBy,
            String inspectorRole,
            Instant createdAt,
            int eligibleAtCreation) {}

    public record Item(
            long checklistId,
            long shiftId,
            long floorAreaId,
            long taskId,
            Long performerAccountId,
            Long performerStaffId,
            String status,
            String note,
            List<Long> photoFileIds,
            Long inspectorId,
            String inspectorRole,
            Instant recordedAt) {
        public Item {
            photoFileIds = photoFileIds == null ? List.of() : List.copyOf(photoFileIds);
        }

        public Item result(
                String result, String resultNote, List<Long> photos, long inspector, String role, Instant at) {
            return new Item(
                    checklistId,
                    shiftId,
                    floorAreaId,
                    taskId,
                    performerAccountId,
                    performerStaffId,
                    result,
                    resultNote,
                    photos,
                    inspector,
                    role,
                    at);
        }
    }

    /** FR-059 (research L-10 {@code SoMucToiThieu}): trần(tỷ lệ × n), tối thiểu 1 khi có công việc đủ điều kiện. */
    public static int minimumItems(int eligible, BigDecimal percent) {
        if (eligible <= 0) return 0;
        int value = percent.multiply(BigDecimal.valueOf(eligible))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
                .intValue();
        return Math.max(1, value);
    }
}
