package com.eldercare.backend.roombedallocation.internal.allocation.domain;

/** Căn cứ chuyển theo lịch và trạng thái chờ của phân bổ. */
public record ScheduledAllocation(
        AllocationRecord allocation,
        long requestId,
        long previousAllocationId,
        boolean waiting,
        java.time.Instant plannedStart) {}
