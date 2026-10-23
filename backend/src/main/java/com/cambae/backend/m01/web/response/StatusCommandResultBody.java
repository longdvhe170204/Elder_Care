package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService.Outcome;
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
