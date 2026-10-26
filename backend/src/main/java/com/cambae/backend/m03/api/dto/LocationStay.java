package com.cambae.backend.m03.api.dto;

import java.time.Instant;
import java.util.List;

/** Kết quả lịch sử 003 mục 1.3; cờ đính chính/nhập bù và khoảng vắng đi cùng nguồn. */
public record LocationStay(
        long residentId,
        String fullName,
        LocationView location,
        Instant from,
        Instant to,
        boolean corrected,
        boolean pendingLateEntry,
        List<AbsenceInterval> absences) {
    public LocationStay {
        absences = List.copyOf(absences);
    }

    public record AbsenceInterval(Instant from, Instant to) {}
}
