package com.cambae.backend.m02.internal.absence.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Kết quả tra chính sách của một ngày vắng (data-model mục 8, bảng {@code absence_day}; FR-052, FR-058). */
public record AbsenceDay(
        LocalDate date,
        int sequenceNo,
        BigDecimal coefficient,
        boolean holdBed,
        String holdPolicy,
        String policyLineSource,
        String policyLine,
        boolean pendingDecision,
        boolean noPolicyLine,
        Long decisionId) {}
