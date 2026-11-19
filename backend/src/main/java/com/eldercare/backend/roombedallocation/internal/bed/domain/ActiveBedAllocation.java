package com.eldercare.backend.roombedallocation.internal.bed.domain;

/** Tách từ {@code BedHoldStore.Active} (LayerArchTest: record ở {@code domain}). */
public record ActiveBedAllocation(long allocationId, long bedId) {}
