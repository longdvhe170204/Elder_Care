package com.eldercare.backend.care.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * 014 ActivityStatisticsApi.sessions (FR-070); absentByReason theo mã lý do vắng (REFUSED, HEALTH, AWAY, NO_SHOW, OTHER);
 * {@code lateRecorded}: số điểm danh mang nhãn "ghi nhận muộn" (FR-030; 016 FR-032, bổ sung 2026-11-11).
 */
public record SessionStatisticsView(
        long sessionId,
        long activityId,
        Optional<Long> floorAreaId,
        Instant startsAt,
        String status,
        boolean trip,
        int registered,
        int present,
        Map<String, Integer> absentByReason,
        int notRecorded,
        int lateRecorded) {}
