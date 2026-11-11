package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;

@Schema(name = "CareTask")
public record CareTaskResponse(
        long id,
        Long residentId,
        ResidentCard residentCard,
        String location,
        String taskType,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) long taskTypeId,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) ResultType resultType,
        String content,
        Basis basis,
        Instant plannedAt,
        Instant windowFrom,
        Instant windowTo,
        Importance importance,
        String performer,
        boolean shared,

        @Schema(allowableValues = {"FROM_PREVIOUS_SHIFT", "AWAITING_CONFIRMATION"})
        String carryover,

        TaskStatus status,
        boolean awaitingRemeasurementConfirmation,
        int reminderCount,
        Set<String> allowedCommands) {
    @Schema(name = "CareTaskBasis")
    public record Basis(SourceType type, Long id) {}

    @Schema(name = "CareResidentCard")
    public record ResidentCard(String fullName, String photo, String roomBed, String careNotes) {}

    public static CareTaskResponse of(
            CareTask t, ResidentCard resident, String performer, boolean health, Set<String> commands) {
        return of(t, resident, performer, health, commands, 0);
    }

    public static CareTaskResponse of(
            CareTask t,
            ResidentCard resident,
            String performer,
            boolean health,
            Set<String> commands,
            int reminderCount) {
        return new CareTaskResponse(
                t.id(),
                t.residentId(),
                resident,
                t.locationId() == null ? null : t.locationType() + " " + t.locationId(),
                t.data().taskTypeName(),
                t.data().publicTaskTypeId(),
                ResultType.valueOf(t.data().resultType()),
                health ? t.data().content() : null,
                new Basis(t.basisType(), t.basisId()),
                t.plannedAt(),
                t.windowFrom(),
                t.windowTo(),
                t.importance(),
                performer,
                t.performerId() == null,
                t.carryover(),
                t.status(),
                t.awaitingRemeasurement(),
                reminderCount,
                commands);
    }
}
