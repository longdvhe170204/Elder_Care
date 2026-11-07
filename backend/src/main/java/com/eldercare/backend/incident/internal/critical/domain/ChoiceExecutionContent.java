package com.eldercare.backend.incident.internal.critical.domain;

/**
 * Lựa chọn thực hiện, bản ghi nhóm 3 gắn cảnh báo nguy kịch (FR-050d, FR-050g; R-15 {@code lua_chon_thuc_hien} →
 * {@code record_event}). Lý do bắt buộc khi không liên lạc được và chưa có nguyện vọng Hiệu lực (FR-050c).
 */
public record ChoiceExecutionContent(ExecutionChoice choice, String reason, Long transferSummaryId) {}
