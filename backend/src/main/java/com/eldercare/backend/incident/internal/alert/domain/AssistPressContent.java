package com.eldercare.backend.incident.internal.alert.domain;

/**
 * Một lần bấm nút hỗ trợ khẩn cấp ({@code lan_bam_ho_tro}, data-model mục 7; R-15: {@code record_event} kind
 * {@code M05.ASSIST_PRESS}, cha là tài khoản người bấm). Tầng theo ca lúc bấm, rỗng khi người bấm không có tầng.
 */
public record AssistPressContent(long accountId, long staffId, String idempotencyKey, Long shiftFloorAreaId) {}
