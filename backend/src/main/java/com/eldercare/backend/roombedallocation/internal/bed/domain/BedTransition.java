package com.eldercare.backend.roombedallocation.internal.bed.domain;
/** T008: dữ liệu của máy trạng thái giường, research G-04. */
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
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
        boolean healthReason,
        String note) {
    /** Các lệnh cũ không có ghi chú xác nhận sẵn sàng. */
    public BedTransition(
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
            boolean healthReason) {
        this(
                event,
                reason,
                basis,
                basisType,
                basisId,
                plannedAt,
                actorId,
                hold,
                closedAllocationId,
                disinfectionRequired,
                disinfected,
                ppeUsed,
                startReservedAllocation,
                healthReason,
                null);
    }
}
