package com.cambae.backend.m03.internal.history.domain;

import com.cambae.backend.m03.api.dto.LocationView;
import java.time.Instant;

/** Khoảng vị trí đã áp đính chính; không chứa dữ liệu sức khỏe hoặc bản ghi M01. */
public record HistoryStay(
        long allocationId, long residentId, LocationView location, Instant from, Instant to, boolean corrected) {}
