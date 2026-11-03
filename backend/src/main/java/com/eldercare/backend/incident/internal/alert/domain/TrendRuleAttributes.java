package com.eldercare.backend.incident.internal.alert.domain;

/** Thuộc tính M05.TREND_RULE trên catalog_item; trạng thái chung là bật/tắt. */
public record TrendRuleAttributes(IncidentLevel level, String parameterCode) {}
