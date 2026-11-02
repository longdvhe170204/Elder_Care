package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleUpdate;

/** Lệnh bật/tắt hoặc đổi mức quy tắc xu hướng. */
public record TrendRuleRequest(boolean enabled, IncidentLevel level, Reason reason) {
    public TrendRuleUpdate toInput() {
        return new TrendRuleUpdate(enabled, level);
    }
}
