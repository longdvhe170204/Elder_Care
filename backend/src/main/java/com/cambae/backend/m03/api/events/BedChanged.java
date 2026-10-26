package com.cambae.backend.m03.api.events;

import java.time.Instant;

public record BedChanged(
        long residentId,
        long previousAllocationId,
        long newAllocationId,
        Long previousFloorId,
        Long newFloorId,
        Instant at) {}
