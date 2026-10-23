package com.cambae.backend.m03.api.dto;

import java.time.Instant;
import java.util.List;

/** Kiểu trao đổi RoommateView, hợp đồng 003 mục 1.2–1.9 (T003). */
public record RoommateView(
        long residentId,
        String fullName,
        LocationView location,
        Instant from,
        Instant to,
        boolean corrected,
        boolean pendingBackfill,
        List<AbsenceInterval> absences) {}
