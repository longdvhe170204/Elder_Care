package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.emergency.domain.Call115Attempt;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** {@code Call115Attempt} của api-007 (FR-047k). */
@Schema(name = "Call115Attempt")
public record Call115AttemptResponse(long id, Instant pressedAt, String pressedBy, Outcome result) {

    /** Kết quả gọi; rỗng khi người bấm chưa chọn. */
    public enum Outcome {
        CONNECTED,
        NOT_CONNECTED
    }

    public static Call115AttemptResponse of(Call115Attempt attempt) {
        return new Call115AttemptResponse(
                attempt.id(),
                attempt.pressedAt(),
                attempt.pressedBy(),
                attempt.result() == null ? null : Outcome.valueOf(attempt.result()));
    }
}
