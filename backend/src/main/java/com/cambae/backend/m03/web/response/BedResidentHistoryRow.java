package com.cambae.backend.m03.web.response;

import java.time.Instant;

/** Một người từng sử dụng giường trong khoảng được hỏi. */
public record BedResidentHistoryRow(long residentId, String fullName, Instant from, Instant to, boolean corrected) {}
