package com.eldercare.backend.administration.correction.internal.domain;

/** Số bản đính chính đã cấp và cờ Hủy ghi nhận của chuỗi một bản gốc (FR-027 → FR-029). */
public record ChainState(int correctionCount, boolean voided) {}
