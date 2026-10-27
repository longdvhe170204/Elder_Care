package com.cambae.backend.m13.web.request;

import com.cambae.backend.m13.internal.urgent.service.CallReminderQueries.ResultView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Kết quả gọi (schema {@code CallResult}; FR-030, FR-031): thân {@code POST /call-reminders/{id}/results} và mục kết quả
 * của lượt nhắc; Đã liên lạc được bắt buộc ghi chú, Không liên lạc được bắt buộc lý do (kiểm ở lệnh, 422).
 */
public record CallResultBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Result result,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime calledAt,

        String calledPhoneNumber,
        Reason reason,
        String answeredBy,
        String note,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Boolean supplementary) {

    public enum Result {
        REACHED,
        NOT_REACHED
    }

    public enum Reason {
        NO_ANSWER,
        WRONG_NUMBER,
        NO_NUMBER,
        OTHER
    }

    public static CallResultBody of(ResultView r) {
        return new CallResultBody(
                Result.valueOf(r.result()),
                r.calledAt().atOffset(ZoneOffset.UTC),
                r.calledPhoneNumber(),
                r.reason() == null ? null : Reason.valueOf(r.reason()),
                r.answeredBy(),
                r.note(),
                r.supplementary());
    }
}
