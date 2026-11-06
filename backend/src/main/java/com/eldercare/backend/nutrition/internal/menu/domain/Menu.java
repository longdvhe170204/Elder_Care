package com.eldercare.backend.nutrition.internal.menu.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Thực đơn tuần (bảng {@code menu}, data-model mục 5). */
public record Menu(
        long id,
        LocalDate weekStart,
        MenuStatus status,
        MenuContent content,
        boolean processingCheckConfirmed,
        String processingNote,
        Long processingCheckedBy,
        Instant processingCheckedAt,
        List<String> confirmedWarnings,
        Long submittedBy,
        Long submitterStaffId,
        Instant submittedAt,
        Long publishedBy,
        Instant publishedAt,
        Instant appliedAt,
        String returnReason,
        String cancelReason,
        Long copiedFromId,
        Long lastSwapId,
        int version) {

    public Menu {
        confirmedWarnings = confirmedWarnings == null ? List.of() : List.copyOf(confirmedWarnings);
    }

    public LocalDate weekEnd() {
        return weekStart.plusDays(6);
    }
}
