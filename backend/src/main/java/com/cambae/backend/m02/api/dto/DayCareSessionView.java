package com.cambae.backend.m02.api.dto;

import java.time.LocalDate;

/**
 * Một buổi bán trú có lịch (FR-060, FR-060a). {@code coefficientPercent} rỗng khi buổi chưa xác định (chưa có điểm
 * danh của 005); {@code closureDay} là ngày khu bán trú nghỉ.
 */
public record DayCareSessionView(
        LocalDate date, long sessionId, Attendance attendance, Integer coefficientPercent, boolean closureDay) {}
