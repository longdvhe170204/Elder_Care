package com.cambae.backend.m03.internal.bed.domain;
/** T008: dữ liệu của máy trạng thái giường, research G-04. */
import com.cambae.backend.common.Reason;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.time.Instant;

public record BedTransition(
        BedEvent event,
        Reason reason,
        Basis basis,
        String basisType,
        long basisId,
        Instant plannedAt,
        Long actorId,
        BedHold hold,
        Long closedAllocationId,
        Boolean disinfectionRequired,
        Boolean disinfected,
        Boolean ppeUsed,
        boolean startReservedAllocation,
        boolean healthReason) {}
