package com.cambae.backend.m02.internal.waitinglist.domain;

/** Thành phần điểm ưu tiên (FR-009): mức chăm sóc, thời gian chờ, tình huống đặc biệt, điều chỉnh đã duyệt. */
public record ScoreBreakdown(int careLevel, int waitingTime, int circumstances, int adjustment) {

    public int total() {
        return careLevel + waitingTime + circumstances + adjustment;
    }
}
