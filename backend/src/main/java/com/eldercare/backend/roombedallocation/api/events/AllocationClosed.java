package com.eldercare.backend.roombedallocation.api.events;

import java.time.Instant;

public record AllocationClosed(long allocationId, long residentId, long bedId, Instant at, String reason) {}
