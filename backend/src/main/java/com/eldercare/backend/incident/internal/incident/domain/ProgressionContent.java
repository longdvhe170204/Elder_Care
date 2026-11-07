package com.eldercare.backend.incident.internal.incident.domain;

/** Nội dung một diễn biến sự cố trong bảng sự kiện chung (data-model {@code dien_bien_su_co}, R-15). */
public record ProgressionContent(long progressionTypeId, String typeCode, String content, boolean outOfScope) {}
