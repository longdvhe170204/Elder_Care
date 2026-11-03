package com.eldercare.backend.incident.internal.alert.domain;

/** Nội dung lệnh thêm loại diễn biến. */
public record ProgressionTypeInput(
        String code, String name, ProgressionGroup group, boolean resuscitation, int sortOrder) {}
