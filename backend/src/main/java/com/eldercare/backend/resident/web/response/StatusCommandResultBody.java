package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.Warning;
import com.eldercare.backend.resident.internal.status.service.ResidentStatusService.Outcome;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Kết quả một lệnh trạng thái (contracts/api-001.yaml, schema {@code StatusCommandResult}). */
@Schema(name = "StatusCommandResult")
public record StatusCommandResultBody(ResidentStatus status, StatusHistoryEntryBody history, List<Warning> warnings) {

    public static StatusCommandResultBody of(Outcome outcome) {
        return new StatusCommandResultBody(
                outcome.status(), StatusHistoryEntryBody.of(outcome.history()), outcome.warnings());
    }
}
