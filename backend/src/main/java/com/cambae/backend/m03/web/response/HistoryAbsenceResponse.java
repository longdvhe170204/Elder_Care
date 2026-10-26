package com.cambae.backend.m03.web.response;

import java.time.Instant;

/** Phân biệt khoảng vắng của A và người cùng phòng. */
public record HistoryAbsenceResponse(long residentId, String type, Instant from, Instant to) {}
