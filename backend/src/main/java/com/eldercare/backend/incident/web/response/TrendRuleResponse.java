package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleView;

/** Quy tắc xu hướng trả về qua HTTP. */
public record TrendRuleResponse(String code, String name, boolean enabled, IncidentLevel level, String parameterCode) {
    public static TrendRuleResponse from(TrendRuleView view) {
        return new TrendRuleResponse(view.code(), view.name(), view.enabled(), view.level(), view.parameterCode());
    }
}
