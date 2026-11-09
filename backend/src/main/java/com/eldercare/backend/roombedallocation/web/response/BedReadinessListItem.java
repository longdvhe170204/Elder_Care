package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedDetails;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/** T060: một giường Chờ sẵn sàng của {@code GET /bed-readiness/beds}; không có trường người cao tuổi (duyệt 2026-11-06). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedReadinessListItem(
        long id, LocationResponse location, BedStatus state, Instant readyDeadline, boolean readinessOverdue) {
    public static BedReadinessListItem of(BedDetails d) {
        return new BedReadinessListItem(
                d.id(), LocationResponse.of(d.location()), d.state(), d.readyDeadline(), d.readinessOverdue());
    }
}
