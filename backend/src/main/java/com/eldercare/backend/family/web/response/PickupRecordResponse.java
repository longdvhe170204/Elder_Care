package com.eldercare.backend.family.web.response;

import com.eldercare.backend.family.internal.pickup.domain.PickupRecord;
import com.eldercare.backend.family.internal.pickup.service.PickupService.PickupView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Bản ghi đón ({@code PickupRecord}; FR-026); {@code familyMemberId} trống với Tự về, ngoại lệ. */
@Schema(name = "PickupRecord")
public record PickupRecordResponse(
        long id,
        String type,
        Long familyMemberId,
        PickupRecord.Basis basis,
        Instant at,
        PickupRecord.Status status,
        Instant expiresAt) {

    public static PickupRecordResponse of(PickupView view) {
        PickupRecord r = view.record();
        return new PickupRecordResponse(
                r.id(), r.type().name(), r.familyMemberId(), r.basis(), r.at(), view.status(), r.expiresAt());
    }
}
