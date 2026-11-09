package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.billing.internal.adjustment.domain.CostSourceChangeView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(name = "PostFinalizationChange")
public record PostFinalizationChangeBody(
        long id,
        long itemId,
        long residentId,
        String event,
        Map<String, Object> before,
        Map<String, Object> after,
        String reference,
        Instant occurredAt) {
    public static PostFinalizationChangeBody of(CostSourceChangeView value) {
        return new PostFinalizationChangeBody(
                value.id(),
                value.itemId(),
                value.residentId(),
                value.event(),
                value.before(),
                value.after(),
                value.reference(),
                value.occurredAt());
    }
}
