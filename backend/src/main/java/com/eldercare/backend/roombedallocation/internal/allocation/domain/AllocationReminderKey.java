package com.eldercare.backend.roombedallocation.internal.allocation.domain;

/** Tách từ {@code AllocationReminderStore.Key} (LayerArchTest: record ở {@code domain}). */
public record AllocationReminderKey(String type, long id, String flag) {}
