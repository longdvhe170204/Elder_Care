package com.eldercare.backend.roombedallocation.internal.bed.domain;

import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.List;

/** Ảnh đọc bảng giường, bổ sung quyền và dấu qua service trước khi ra HTTP. */
public record BedDetails(
        long id,
        long roomId,
        String code,
        BedStatus state,
        int version,
        LocationView location,
        String holdReason,
        Long heldForResidentId,
        Instant holdUntil,
        Instant readyDeadline,
        boolean readinessOverdue,
        Boolean awaitingResidentTransfer,
        List<String> allowedCommands) {
    public BedDetails withAccess(Instant now, Boolean awaiting, List<String> commands) {
        return new BedDetails(
                id,
                roomId,
                code,
                state,
                version,
                location,
                holdReason,
                heldForResidentId,
                holdUntil,
                readyDeadline,
                readyDeadline != null && now.isAfter(readyDeadline),
                awaiting,
                List.copyOf(commands));
    }
}
