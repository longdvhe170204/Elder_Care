package com.cambae.backend.m15.correction.api;

/**
 * Giá trị hiện hành của bản gốc (FR-030): nội dung của bản sửa mới nhất ({@code null} khi chưa sửa), có đã đính chính,
 * đã Hủy ghi nhận.
 */
public record CurrentValue(Object content, boolean corrected, boolean voided) {}
