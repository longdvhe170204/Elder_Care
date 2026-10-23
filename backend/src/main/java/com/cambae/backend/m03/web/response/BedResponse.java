package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.api.dto.BedStatus;
import com.cambae.backend.m03.internal.bed.domain.BedDetails;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** T017: dữ liệu giường; các trường tra cứu phân bổ tùy chọn được bổ sung ở story tương ứng. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedResponse(
        long id,
        String code,
        LocationResponse location,
        BedStatus state,

        @Schema(allowableValues = {"ABSENT_RESIDENT", "WAITING_RECORD"})
        String holdReason,

        Long heldForResidentId,
        Instant holdUntil,
        Instant readyDeadline,
        boolean readinessOverdue,
        Boolean awaitingResidentTransfer,
        Instant futureAllocationFrom,

        @Schema(implementation = ResidentSummarySchema.class)
        Object occupant,

        List<BedFlagCode> flags,
        List<String> allowedCommands) {
    public static BedResponse of(BedDetails d) {
        return new BedResponse(
                d.id(),
                d.code(),
                LocationResponse.of(d.location()),
                d.state(),
                d.holdReason(),
                d.heldForResidentId(),
                d.holdUntil(),
                d.readyDeadline(),
                d.readinessOverdue(),
                d.awaitingResidentTransfer(),
                null,
                null,
                List.of(),
                d.allowedCommands());
    }
}
