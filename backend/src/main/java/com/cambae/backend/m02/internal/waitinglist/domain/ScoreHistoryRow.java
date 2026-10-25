package com.cambae.backend.m02.internal.waitinglist.domain;

import java.time.Instant;

/** Một dòng lịch sử điểm (nhóm 3, chỉ thêm; FR-010). {@code approvalRequestId} chỉ có ở điều chỉnh (FR-011). */
public record ScoreHistoryRow(
        long id,
        long entryId,
        int oldScore,
        int newScore,
        ScoreBreakdown components,
        ScoreCause cause,
        Long approvalRequestId,
        Instant at) {}
