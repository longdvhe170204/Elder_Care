package com.cambae.backend.common.error;

/** Một điều kiện chưa thỏa: mã quy tắc (BR-…) và mô tả (FR-006). */
public record UnmetCondition(String code, String description) {}
