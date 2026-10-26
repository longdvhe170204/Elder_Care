package com.cambae.backend.m03.web.response;

import java.time.Instant;

/** Khoảng chồng nửa mở. */
public record HistoryInterval(Instant from, Instant to) {}
