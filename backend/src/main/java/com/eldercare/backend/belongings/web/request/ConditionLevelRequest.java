package com.eldercare.backend.belongings.web.request;

/** Thân thêm mức tình trạng (api-013 {@code /belongings/condition-levels}). */
public record ConditionLevelRequest(String name, int order, Boolean unusable, String status) {}
