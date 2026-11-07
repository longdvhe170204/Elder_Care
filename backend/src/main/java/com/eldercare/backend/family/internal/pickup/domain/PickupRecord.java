package com.eldercare.backend.family.internal.pickup.domain;

import java.time.Instant;

/**
 * Bản ghi đón ({@code pickup_record}, nhóm 3; data-model mục 4; FR-026). Trạng thái không nằm trên dòng mà suy ra từ sự
 * kiện kết quả ({@link PickupOutcome}): chưa có là Chưa dùng.
 *
 * @param familyMemberId người đón có hồ sơ người thân; {@code null} với Tự về, ngoại lệ
 * @param pickerName họ tên người đón; {@code null} với Tự về
 * @param expiresAt lúc ghi cộng CFG-M10-09; {@code null} với Viện đưa về (FR-027)
 */
public record PickupRecord(
        long id,
        long residentId,
        PickupType type,
        Long familyMemberId,
        Basis basis,
        Long exceptionId,
        String pickerName,
        String idDocumentType,
        String idDocumentNumber,
        Instant at,
        long handoverStaffId,
        long recordedBy,
        Instant expiresAt) {

    public static final String ENTITY_TYPE = "M10.PICKUP_RECORD";

    /** Căn cứ đón (FR-026). */
    public enum Basis {
        PICKUP_LIST,
        REPRESENTATIVE_EXCEPTION,
        MANAGER_EXCEPTION,
        SELF_RETURN
    }

    /** Trạng thái suy ra (glossary: CHUA_DUNG, DA_DUNG, KHONG_DUNG). */
    public enum Status {
        UNUSED,
        USED,
        NOT_USED
    }

    /** Còn là căn cứ của lệnh nguồn lúc {@code t}: chưa có kết quả, đã ghi trước {@code t}, chưa quá CFG-M10-09. */
    public boolean usableAt(Instant t, Status status) {
        return status == Status.UNUSED && !t.isBefore(at) && (expiresAt == null || t.isBefore(expiresAt));
    }
}
