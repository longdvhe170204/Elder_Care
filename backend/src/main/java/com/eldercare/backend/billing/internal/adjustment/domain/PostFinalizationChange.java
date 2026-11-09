package com.eldercare.backend.billing.internal.adjustment.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.Map;

/** FR-027: chỉ lưu dữ liệu nguồn, không tính hoặc lưu số tiền chênh lệch. */
public record PostFinalizationChange(
        String event, Map<String, Object> before, Map<String, Object> after, String reference, Instant occurredAt) {
    public static final SharedKind<PostFinalizationChange> KIND = SharedKind.of(
                    "M11.POST_FINALIZATION_CHANGE", PostFinalizationChange.class)
            .notDeletable();
}
