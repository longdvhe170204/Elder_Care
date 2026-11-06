package com.eldercare.backend.family.internal.pickup.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Sự kiện kết quả của bản ghi đón ({@code ban_ghi_don_su_kien}, nhóm 3; R-15: {@code record_event} loại {@link #KIND}
 * gắn {@code M10.PICKUP_RECORD}): Đã dùng kèm lệnh nguồn, hoặc Không dùng kèm lý do (FR-017, FR-027). Mỗi bản ghi có tối
 * đa một kết quả: khóa {@link #key} vừa là {@code khoa_da_dung} (BR-M10-03, một lần dùng) vừa chặn Đã dùng sau Không dùng.
 */
public record PickupOutcome(Kind kind, String sourceCommand, Long sourceId, String reason) {

    public static final SharedKind<PickupOutcome> KIND = SharedKind.of("M10.PICKUP_RECORD_EVENT", PickupOutcome.class);

    public enum Kind {
        USED,
        NOT_USED
    }

    /** Lý do Không dùng. */
    public static final String EXPIRED = "EXPIRED";

    public static final String PICKER_REMOVED = "PICKER_REMOVED";
    public static final String SELF_RETURN_OFF = "SELF_RETURN_OFF";

    public static String key(long pickupRecordId) {
        return "M10.PICKUP_DONE:" + pickupRecordId;
    }

    public PickupRecord.Status status() {
        return kind == Kind.USED ? PickupRecord.Status.USED : PickupRecord.Status.NOT_USED;
    }
}
