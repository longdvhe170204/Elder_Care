package com.eldercare.backend.family.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay;
import com.eldercare.backend.family.web.request.FamilyStayRegistrationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

// Hợp đồng ghi allOf(FamilyStayRegistration, {trạng thái}); JSON vẫn là một object phẳng
@Schema(
        name = "FamilyStay",
        allOf = {FamilyStayRegistrationRequest.class, FamilyStayResponse.State.class})
public record FamilyStayResponse(
        long familyMemberId,
        long residentId,
        Instant expectedStartAt,
        Instant expectedEndAt,
        String location,
        String reason,
        boolean mealRegistered,
        long id,
        FamilyStay.Status status,
        Instant actualStartAt,
        Instant actualEndAt,
        int nightCount) {

    /** Phần trạng thái của lượt ở lại (data-model mục 6). */
    @Schema(name = "FamilyStayState")
    public record State(
            long id,
            FamilyStay.Status status,
            Instant actualStartAt,
            Instant actualEndAt,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            int nightCount) {}

    public static FamilyStayResponse of(SimpleRecord<FamilyStay> record, int nightCount) {
        FamilyStay s = record.content();
        return new FamilyStayResponse(
                s.familyMemberId(),
                record.residentId(),
                s.expectedStartAt(),
                s.expectedEndAt(),
                s.location(),
                s.reason(),
                s.mealRegistered(),
                record.id(),
                FamilyStay.Status.valueOf(record.status()),
                s.actualStartAt(),
                s.actualEndAt(),
                nightCount);
    }
}
