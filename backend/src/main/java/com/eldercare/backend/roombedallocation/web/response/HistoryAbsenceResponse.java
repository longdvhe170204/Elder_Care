package com.eldercare.backend.roombedallocation.web.response;

import java.time.Instant;

/** Phân biệt khoảng vắng của A và người cùng phòng. */
public record HistoryAbsenceResponse(long residentId, String type, Instant from, Instant to) {}
