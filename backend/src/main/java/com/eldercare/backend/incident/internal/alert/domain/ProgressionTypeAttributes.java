package com.eldercare.backend.incident.internal.alert.domain;

/** Thuộc tính loại diễn biến; không suy diễn hồi sức từ tên. */
public record ProgressionTypeAttributes(ProgressionGroup group, boolean resuscitation) {}
