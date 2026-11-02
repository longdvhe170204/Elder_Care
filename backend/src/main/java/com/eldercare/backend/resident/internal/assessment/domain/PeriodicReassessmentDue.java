package com.eldercare.backend.resident.internal.assessment.domain;

/** Hồ sơ tới hạn báo đánh giá lại định kỳ, kèm lần Đã xác nhận gần nhất làm nguồn báo (FR-039). */
public record PeriodicReassessmentDue(long residentId, long assessmentId) {}
