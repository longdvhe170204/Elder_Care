package com.cambae.backend.m01.internal.assessment.service;

/**
 * Bản đính chính đổi mức chăm sóc của lần Đã xác nhận gần nhất, hồ sơ chưa ở trạng thái cuối (FR-032; research H-10).
 * Nội bộ M01: bộ báo đánh giá lại (T062) nghe để thêm lần báo căn cứ {@code DINH_CHINH_DANH_GIA}.
 */
public record AssessmentCorrectedLevel(long residentId, long assessmentId, long correctionId) {}
