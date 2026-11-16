package com.eldercare.backend.roombedallocation.internal.bed.domain;

import com.eldercare.backend.roombedallocation.api.dto.BedStatus;

/** Tách từ {@code BedReadinessStore.Snapshot} (LayerArchTest: record ở {@code domain}). */
public record ReadinessBedSnapshot(long id, long floorId, BedStatus state, Long departedResidentId) {}
