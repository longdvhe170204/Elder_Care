package com.eldercare.backend.incident.internal.alert.domain;

/** Danh mục diễn biến nội bộ, FR-048a. */
public record ProgressionTypeView(
        long id, String code, String name, ProgressionGroup group, boolean resuscitation, int version) {}
