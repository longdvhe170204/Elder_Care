package com.cambae.backend.m07.api.dto;

import java.time.Instant;

/**
 * Request payload to record a dose refusal (TU_CHOI; FR-021, FR-032).
 */
public record RefuseDoseRequest(String reason, String note, Instant deviceRecordedAt, boolean offlineFlag) {}
