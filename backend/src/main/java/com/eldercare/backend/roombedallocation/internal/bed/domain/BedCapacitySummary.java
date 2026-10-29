package com.eldercare.backend.roombedallocation.internal.bed.domain;

import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import java.util.Map;

/** Sức chứa thực tế trong phạm vi người xem tại thời điểm đọc. */
public record BedCapacitySummary(Map<BedStatus, Integer> byState, int usable, int emptyMatching) {}
