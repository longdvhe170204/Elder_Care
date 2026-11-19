package com.eldercare.backend.roombedallocation.internal.bed.domain;

import java.time.Instant;

/** Tách từ {@code BedReadinessStore.Waiting} (LayerArchTest: record ở {@code domain}). */
public record WaitingBedTransfer(long requestId, Instant plannedAt) {}
