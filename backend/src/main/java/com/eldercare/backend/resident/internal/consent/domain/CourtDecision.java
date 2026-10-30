package com.eldercare.backend.resident.internal.consent.domain;

import java.time.LocalDate;

/**
 * Bằng chứng quyết định của Tòa án khi người đại diện theo pháp luật ký thay (FR-011, Q-348): số, ngày, Tòa án và tệp
 * scan. Khi nhận từ client các trường có thể thiếu; bản đã lưu luôn đủ cả bốn.
 */
public record CourtDecision(String number, LocalDate date, String court, Long fileId) {

    public boolean complete() {
        return number != null
                && !number.isBlank()
                && date != null
                && court != null
                && !court.isBlank()
                && fileId != null;
    }
}
