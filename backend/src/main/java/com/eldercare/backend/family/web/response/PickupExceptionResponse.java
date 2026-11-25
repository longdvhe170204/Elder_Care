package com.eldercare.backend.family.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.family.internal.pickup.domain.PickupException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Yêu cầu ngoại lệ đón ({@code PickupException}; FR-024, FR-050; thêm 2026-11-24, test BF-12 C-2, C-5): danh sách cho
 * nhân viên và người đại diện thay nhập mã ngoại lệ.
 */
@Schema(name = "PickupException")
public record PickupExceptionResponse(
        long id,
        Long residentId,
        String fullName,
        String idDocumentNumber,
        String relationship,
        String reason,
        PickupException.Status status,
        Instant createdAt,
        PickupException.Method method,
        String decisionReason,
        Instant effectiveAt,
        Instant expiresAt,
        Long pickupRecordId) {

    public static PickupExceptionResponse of(SimpleRecord<PickupException> record) {
        PickupException e = record.content();
        return new PickupExceptionResponse(
                record.id(),
                record.residentId(),
                e.fullName(),
                e.idDocumentNumber(),
                e.relationship(),
                e.reason(),
                PickupException.Status.valueOf(record.status()),
                record.createdAt(),
                e.method(),
                e.decisionReason(),
                e.effectiveAt(),
                e.expiresAt(),
                e.pickupRecordId());
    }
}
