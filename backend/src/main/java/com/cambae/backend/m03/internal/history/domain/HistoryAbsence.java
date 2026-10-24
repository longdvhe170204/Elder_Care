package com.cambae.backend.m03.internal.history.domain;

import java.time.Instant;

/** Chỉ công bố khoảng vắng và loại trạng thái phục vụ FR-036. */
public record HistoryAbsence(long residentId, String type, Instant from, Instant to) {}
