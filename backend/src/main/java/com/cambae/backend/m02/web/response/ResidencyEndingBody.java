package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.absence.service.AbsenceNames;
import com.cambae.backend.m02.internal.ending.domain.EndingStatus;
import com.cambae.backend.m02.internal.ending.domain.RecipientType;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.service.EndingHandovers;
import com.cambae.backend.m02.internal.ending.service.ResidencyEndingService.Detail;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/** Hồ sơ kết thúc lưu trú (contracts/api-004.yaml, schema {@code ResidencyEnding}; FR-062, FR-067). */
@Schema(name = "ResidencyEnding")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResidencyEndingBody(
        long id,
        long residentId,
        String endCase,
        LocalDate expectedEndDate,
        Instant actualEndedAt,
        String destination,
        LocalDate overdueSince,
        HandoverBody handover,
        EndingStatus status) {

    /** Lần bàn giao hiện hành (FR-063 (e)). */
    @Schema(name = "ResidencyEndingHandover")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HandoverBody(String recipientType, String recipient, Instant at, String staff) {}

    public static ResidencyEndingBody of(ResidencyEnding ending, AbsenceNames names) {
        return of(new Detail(ending, Optional.empty()), names);
    }

    public static ResidencyEndingBody of(Detail detail, AbsenceNames names) {
        ResidencyEnding ending = detail.ending();
        return new ResidencyEndingBody(
                ending.id(),
                ending.residentId(),
                ending.endCase().name(),
                ending.expectedEndDate(),
                ending.actualEndedAt(),
                ending.destination(),
                ending.overdueSince(),
                detail.handover().map(h -> handover(h, names)).orElse(null),
                ending.status());
    }

    private static HandoverBody handover(EndingHandovers.Effective handover, AbsenceNames names) {
        String recipient = handover.recipientType() == RecipientType.FAMILY_MEMBER
                ? names.familyMember(handover.familyMemberId())
                : handover.facilityName();
        return new HandoverBody(
                handover.recipientType().name(),
                recipient,
                handover.handoverAt(),
                names.staff(handover.handoverStaffId()));
    }
}
