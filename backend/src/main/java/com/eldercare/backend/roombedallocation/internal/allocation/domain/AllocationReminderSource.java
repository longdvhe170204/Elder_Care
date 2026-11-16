package com.eldercare.backend.roombedallocation.internal.allocation.domain;

/** Tách từ {@code AllocationReminderStore.Source} (LayerArchTest: record ở {@code domain}). */
public record AllocationReminderSource(long resident, Long floor, boolean open) {}
