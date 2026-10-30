package com.cambae.backend.m03.api.events;

import java.time.Instant;

public record AllocationStarted(long allocationId, long residentId, long bedId, Instant at) {}
