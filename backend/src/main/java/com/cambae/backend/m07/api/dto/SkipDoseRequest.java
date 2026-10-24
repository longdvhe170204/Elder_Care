package com.cambae.backend.m07.api.dto;

import java.time.Instant;

/**
 * Request payload to record a dose as not administered / skipped (KHONG_THUC_HIEN; FR-021).
 */
public record SkipDoseRequest(String reason, String note, Instant deviceRecordedAt, boolean offlineFlag) {}
