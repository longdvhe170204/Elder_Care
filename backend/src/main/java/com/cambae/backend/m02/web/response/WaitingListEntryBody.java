package com.cambae.backend.m02.web.response;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.waitinglist.domain.CircumstanceStatus;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListStatus;
import com.cambae.backend.m02.internal.waitinglist.service.WaitingListService;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Hồ sơ chờ kèm chi tiết điểm (contracts/api-004.yaml, schema {@code WaitingListEntry}; FR-009). */
@Schema(name = "WaitingListEntry")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WaitingListEntryBody(
        long id,
        long residentId,
        String fullName,
        String gender,
        LocalDate applicationDate,
        ResidenceType residenceType,
        CareLevel careLevel,
        boolean notAssessed,
        List<CircumstanceBody> circumstances,
        int score,
        ScoreBreakdownBody scoreBreakdown,
        Long heldBedId,
        WaitingListStatus status) {

    public record CircumstanceBody(String code, CircumstanceStatusBody status) {}

    public enum CircumstanceStatusBody {
        CONFIRMED,
        PENDING_CONFIRMATION;

        static CircumstanceStatusBody of(CircumstanceStatus status) {
            return valueOf(status.name());
        }
    }

    public record ScoreBreakdownBody(int careLevel, int waitingTime, int circumstances, int adjustment) {}

    public static WaitingListEntryBody of(WaitingListService.View view) {
        var entry = view.entry();
        return new WaitingListEntryBody(
                entry.id(),
                entry.residentId(),
                view.fullName(),
                view.gender() == null ? null : view.gender().name(),
                entry.applicationDate(),
                entry.residenceType(),
                entry.careLevel(),
                entry.careLevelNotAssessed(),
                view.circumstances().stream()
                        .map(c -> new CircumstanceBody(c.code(), CircumstanceStatusBody.of(c.status())))
                        .toList(),
                entry.score(),
                new ScoreBreakdownBody(
                        entry.breakdown().careLevel(),
                        entry.breakdown().waitingTime(),
                        entry.breakdown().circumstances(),
                        entry.breakdown().adjustment()),
                entry.heldBedId(),
                entry.status());
    }
}
