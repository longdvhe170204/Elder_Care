package com.eldercare.backend.care.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** 014 ActivityStatisticsApi.sessions (FR-070); absentByReason theo mã lý do vắng (REFUSED, HEALTH, AWAY, NO_SHOW, OTHER). */
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
        int notRecorded) {}
