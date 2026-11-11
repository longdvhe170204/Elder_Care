package com.eldercare.backend.resident.api.dto;

/**
 * Lệnh đổi trạng thái: bảy lệnh của người dùng, năm lệnh do hệ thống hoặc module khác gọi (contracts mục 1.2; {@code
 * CANCEL_RETURN_RECORD} bổ sung 2026-11-11, chủ dự án duyệt, 014 FR-031a (c)).
 */
public enum StatusCommand {
    COMPLETE_ADMISSION,
    CANCEL_ADMISSION,
    GRANT_TEMPORARY_ABSENCE,
    RECORD_RETURN,
    TRANSFER_OUT,
    END_RESIDENCY,
    RECORD_DEATH,
    CREATE_PROFILE,
    ROLL_CALL_DEPARTURE,
    ROLL_CALL_RETURN,
    CANCEL_DEPARTURE_RECORD,
    CANCEL_RETURN_RECORD;
}
