package com.eldercare.backend.roombedallocation.web.response;

import java.time.Instant;

/** Khoảng chồng nửa mở. */
public record HistoryInterval(Instant from, Instant to) {}
