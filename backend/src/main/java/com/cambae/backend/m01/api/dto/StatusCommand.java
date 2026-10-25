package com.cambae.backend.m01.api.dto;

/** Lệnh đổi trạng thái: bảy lệnh của người dùng, bốn lệnh do hệ thống hoặc module khác gọi (contracts mục 1.2). */
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
    CANCEL_DEPARTURE_RECORD;
}
