package com.eldercare.backend.incident.internal.alert.domain;

/** Giá trị quy tắc sau lệnh, kèm phiên bản để dùng ETag. */
public record TrendRuleView(
        String code, String name, boolean enabled, IncidentLevel level, String parameterCode, int version) {}
