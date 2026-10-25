package com.cambae.backend.m03.web.response;

import java.util.List;

/** Nhóm khoảng chồng theo người và phòng, không chứa thông tin sức khỏe. */
public record RoommateHistoryRow(
        long residentId,
        String fullName,
        String room,
        List<HistoryInterval> overlaps,
        List<HistoryAbsenceResponse> absences,
        boolean corrected,
        boolean pendingBackfill) {}
