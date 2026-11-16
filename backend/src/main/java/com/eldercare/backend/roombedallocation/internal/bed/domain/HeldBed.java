package com.eldercare.backend.roombedallocation.internal.bed.domain;

import java.time.Instant;

/** Tách từ {@code BedHoldStore.Held} (LayerArchTest: record ở {@code domain}). */
public record HeldBed(long bedId, long residentId, long waitingId, String reason, Instant until) {}
