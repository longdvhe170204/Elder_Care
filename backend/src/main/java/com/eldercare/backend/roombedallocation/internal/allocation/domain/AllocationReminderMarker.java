package com.eldercare.backend.roombedallocation.internal.allocation.domain;

import java.time.Instant;

/** Tách từ {@code AllocationReminderStore.Marker} (LayerArchTest: record ở {@code domain}). */
public record AllocationReminderMarker(Instant started, Instant last, int count) {}
