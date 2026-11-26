package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.ending.domain.EndingRequestContent;
import com.eldercare.backend.admission.internal.ending.service.EndingRequestService;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** Yêu cầu kết thúc lưu trú của người đại diện (demo 2026-11-26; api-004 schema {@code ResidencyEndingRequest}). */
@Schema(name = "ResidencyEndingRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResidencyEndingRequestBody(
        long id,
        long residentId,
        String residentName,
        long familyMemberId,
        String familyMemberName,
        FamilyEndCase endCase,
        LocalDate expectedEndDate,
        String reason,
        RequestStatus status,
        Instant createdAt,
        String decisionReason,
        Long residencyEndingId) {

    /** Hai trường hợp người thân được gửi. */
    public enum FamilyEndCase {
        CONTRACT_EXPIRED,
        CONTRACT_TERMINATION
    }

    public enum RequestStatus {
        PENDING,
        HANDLED,
        REJECTED,
        CANCELLED
    }

    public static ResidencyEndingRequestBody of(EndingRequestService.View view) {
        EndingRequestContent c = view.record().content();
        return new ResidencyEndingRequestBody(
                view.record().id(),
                view.record().residentId(),
                view.residentName(),
                c.familyMemberId(),
                view.familyMemberName(),
                FamilyEndCase.valueOf(c.endCase().name()),
                c.expectedEndDate(),
                c.reason(),
                RequestStatus.valueOf(view.record().status()),
                c.createdAt(),
                c.decisionReason(),
                c.residencyEndingId());
    }
}
