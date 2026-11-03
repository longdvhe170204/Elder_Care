package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleUpdate;
import io.swagger.v3.oas.annotations.media.Schema;

/** Lệnh bật/tắt hoặc đổi mức quy tắc xu hướng. */
public record TrendRuleRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) IncidentLevel level,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {
    public TrendRuleUpdate toInput() {
        return new TrendRuleUpdate(enabled, level);
    }
}
