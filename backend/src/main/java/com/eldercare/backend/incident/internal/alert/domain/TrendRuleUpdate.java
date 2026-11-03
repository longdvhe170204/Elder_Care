package com.eldercare.backend.incident.internal.alert.domain;

/** Nội dung lệnh cấu hình quy tắc xu hướng, FR-039. */
public record TrendRuleUpdate(boolean enabled, IncidentLevel level) {}
