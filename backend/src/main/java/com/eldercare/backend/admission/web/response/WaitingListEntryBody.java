package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.waitinglist.domain.WaitingListStatus;
import com.eldercare.backend.admission.internal.waitinglist.service.WaitingListService;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Hồ sơ chờ kèm dấu Ưu tiên (contracts/api-004.yaml, schema {@code WaitingListEntry}; FR-009, Q-394). */
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
        boolean priority,
        String priorityReason,
        boolean previouslyResided,
        Long heldBedId,
        WaitingListStatus status) {

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
                entry.priority(),
                entry.priorityReason(),
                view.previouslyResided(),
                entry.heldBedId(),
                entry.status());
    }
}
