package com.eldercare.backend.family.internal.pickup.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Yêu cầu ngoại lệ đón ({@code yeu_cau_ngoai_le_don}, nhóm 2; R-15: {@code simple_record} loại {@link #KIND}; data-model
 * mục 4; FR-024): cho đúng một lượt đón người cao tuổi bởi người ngoài danh sách, không thêm người đó vào danh sách.
 * Trạng thái ở cột {@code status}; trạng thái kết thúc khóa dòng.
 *
 * @param method cách chuyển Hiệu lực: người đại diện qua cổng hoặc Quản lý viện duyệt
 * @param expiresAt lúc Hiệu lực cộng CFG-M10-08
 */
public record PickupException(
        String fullName,
        String idDocumentNumber,
        String relationship,
        String reason,
        Long decidedBy,
        Method method,
        String decisionReason,
        Instant effectiveAt,
        Instant expiresAt,
        Long pickupRecordId) {

    public static final SharedKind<PickupException> KIND = SharedKind.of("M10.PICKUP_EXCEPTION", PickupException.class);

    public enum Status {
        PENDING_CONFIRMATION,
        EFFECTIVE,
        USED,
        EXPIRED,
        REJECTED,
        CANCELLED
    }

    public enum Method {
        PORTAL,
        MANAGER_APPROVAL
    }

    public static PickupException requested(
            String fullName, String idDocumentNumber, String relationship, String reason) {
        return new PickupException(
                fullName, idDocumentNumber, relationship, reason, null, null, null, null, null, null);
    }

    public PickupException effective(long by, Method how, String why, Instant at, Instant until) {
        return new PickupException(fullName, idDocumentNumber, relationship, reason, by, how, why, at, until, null);
    }

    public PickupException decided(Long by, String why) {
        return new PickupException(
                fullName, idDocumentNumber, relationship, reason, by, method, why, effectiveAt, expiresAt, null);
    }

    public PickupException usedBy(long recordId) {
        return new PickupException(
                fullName,
                idDocumentNumber,
                relationship,
                reason,
                decidedBy,
                method,
                decisionReason,
                effectiveAt,
                expiresAt,
                recordId);
    }

    public PickupRecord.Basis basis() {
        return method == Method.PORTAL
                ? PickupRecord.Basis.REPRESENTATIVE_EXCEPTION
                : PickupRecord.Basis.MANAGER_EXCEPTION;
    }
}
