package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.internal.wish.domain.DraftDiscard;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.function.LongFunction;

/** Hủy nháp phiếu nguyện vọng (Q-412, FR-023p): thời điểm, người hủy, lý do. */
@Schema(name = "WishDraftDiscard")
public record DraftDiscardBody(Instant at, String by, String reason) {

    static DraftDiscardBody of(DraftDiscard discard, LongFunction<String> names) {
        return discard == null ? null : new DraftDiscardBody(discard.at(), names.apply(discard.by()), discard.reason());
    }
}
