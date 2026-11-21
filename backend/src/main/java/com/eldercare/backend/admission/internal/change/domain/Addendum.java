package com.eldercare.backend.admission.internal.change.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Phụ lục hợp đồng (data-model mục 7; FR-042, FR-043d, FR-047, DBR-07). {@code contentBefore}, {@code contentAfter} chỉ giữ
 * các thuộc tính bị đổi; {@code contentVersionId} có khi {@code APPLIED}. {@code addendumNo} tăng dần theo hợp đồng, không
 * tái sử dụng; {@code signatureRequired} khi yêu cầu không phải thay đổi nhỏ (Q-419), khi đó phụ lục bắt đầu ở Chờ ký.
 */
public record Addendum(
        long id,
        long contractId,
        long approvalRequestId,
        Set<ChangeType> changeTypes,
        JsonNode contentBefore,
        JsonNode contentAfter,
        LocalDate effectiveDate,
        Status status,
        Long contentVersionId,
        Instant createdAt,
        int addendumNo,
        boolean signatureRequired,
        LocalDate signedDate,
        Long signedFileId,
        Long signedByFamilyMemberId,
        Long facilitySignatoryStaffId) {

    public enum Status {
        /** Chờ Hành chính ghi nhận bản đã ký (Q-419); không được áp dụng. */
        PENDING_SIGNATURE,
        PENDING_EFFECTIVE,
        APPLIED,
        NOT_APPLIED,
        /** Yêu cầu bị hủy khi phụ lục còn Chờ ký, Chờ hiệu lực (Q-420). */
        CANCELLED
    }

    /** Còn chờ: Chờ ký hoặc Chờ hiệu lực. */
    public boolean open() {
        return status == Status.PENDING_SIGNATURE || status == Status.PENDING_EFFECTIVE;
    }
}
