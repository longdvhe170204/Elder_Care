package com.eldercare.backend.incident.internal.alert.domain;

/**
 * Một lần bấm nút hỗ trợ khẩn cấp ({@code lan_bam_ho_tro}, data-model mục 7; R-15: {@code record_event} kind
 * {@code M05.ASSIST_PRESS}, cha là tài khoản người bấm). Tầng của cảnh báo lúc bấm (tầng xảy ra người bấm chọn, không chọn
 * thì tầng theo ca), rỗng là khu vực chung hoặc chưa rõ tầng. {@code offShift}: người bấm không trong ca (đề xuất
 * 2026-11-23); dòng cũ không có trường này.
 */
public record AssistPressContent(
        long accountId, long staffId, String idempotencyKey, Long shiftFloorAreaId, Boolean offShift) {
    public AssistPressContent(long accountId, long staffId, String idempotencyKey, Long shiftFloorAreaId) {
        this(accountId, staffId, idempotencyKey, shiftFloorAreaId, null);
    }
}
