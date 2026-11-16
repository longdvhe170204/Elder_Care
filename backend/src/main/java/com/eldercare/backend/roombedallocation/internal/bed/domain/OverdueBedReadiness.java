package com.eldercare.backend.roombedallocation.internal.bed.domain;

import java.time.Instant;

/** Tách từ {@code BedReadinessStore.Overdue} (LayerArchTest: record ở {@code domain}). */
public record OverdueBedReadiness(long bedId, long floorId, Instant deadline) {}
