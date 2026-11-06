package com.eldercare.backend.care.internal.activity.domain;

import java.time.LocalDate;
import java.util.List;

/** 014 FR-005, FR-009 ({@code mau_lap_phien_ban}): phiên bản mẫu lặp, áp dụng từ {@code effective_from} của dòng. */
public record TemplateVersion(
        List<Integer> daysOfWeek,
        Integer intervalDays,
        String startTime,
        String endTime,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        long changedByStaffId) {}
