package com.eldercare.backend.incident.internal.alert.domain;

import com.eldercare.backend.common.Reason;

/** Nội dung lệnh cấu hình quy tắc xu hướng, FR-039. */
public record TrendRuleUpdate(boolean enabled, IncidentLevel level, Reason reason) {}
