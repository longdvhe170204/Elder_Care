package com.eldercare.backend.medication.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Thân lệnh trên liều của api-006 ({@code DoseResult}, kết quả xem lại; FR-021 → FR-029). */
public final class DoseRequests {

    private DoseRequests() {}

    /** api-006 {@code ExternalPerson}: người báo lại, người nhận, người giao ngoài hệ thống. */
    public record ExternalPersonRequest(PersonType type, Long id, String fullName, String relationship) {}

    public enum PersonType {
        STAFF,
        FAMILY_MEMBER,
        FREE
    }

    public enum ResultBasis {
        REGULAR,
        LEAVE_MEDICATION,
        REPORTED_BY_OTHERS
    }

    /** api-006 {@code DoseResult}: Đã dùng, Từ chối, Không thực hiện. */
    public record DoseResultRequest(
            Instant administeredAt,
            ResultBasis basis,
            @Valid ExternalPersonRequest reportedBy,
            String adverseReaction,
            @Size(min = 1, max = 2000) String reason,
            String note,
            Boolean recordedAfterDisconnect) {}

    public enum ReviewResult {
        ACCEPTED,
        REQUIRES_CORRECTION
    }

    /** Kết quả xem lại bản ghi ghi sau mất kết nối (FR-029). */
    public record DoseReviewResultRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            ReviewResult result,

            String note) {}
}
