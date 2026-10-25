package com.cambae.backend.m03.internal.bed.domain;

import com.cambae.backend.m03.api.dto.BedStatus;
import java.util.Map;

/** Sức chứa thực tế trong phạm vi người xem tại thời điểm đọc. */
public record BedCapacitySummary(Map<BedStatus, Integer> byState, int usable, int emptyMatching) {}
