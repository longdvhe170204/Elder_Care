package com.eldercare.backend.admission.internal.application.service;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Chữ hiển thị trong mô tả điều kiện, cảnh báo tiếp nhận (004 FR-004, FR-076): tên mức chăm sóc, loại lưu trú bằng tiếng Việt
 * và ngày dạng dd/MM/yyyy thay cho mã enum, ngày ISO (audit luồng tiếp nhận 2026-11-18). Mã điều kiện, mã cảnh báo giữ nguyên.
 */
final class AdmissionTexts {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private AdmissionTexts() {}

    static String level(CareLevel level) {
        return switch (level) {
            case BASIC -> "Cơ bản";
            case REGULAR -> "Thường xuyên";
            case SPECIAL -> "Đặc biệt";
        };
    }

    static String type(ResidenceType type) {
        return switch (type) {
            case LONG_TERM_RESIDENTIAL -> "Nội trú dài hạn";
            case SHORT_TERM_RESIDENTIAL -> "Nội trú ngắn ngày";
            case DAY_CARE -> "Bán trú";
        };
    }

    static String date(LocalDate date) {
        return DATE.format(date);
    }
}
