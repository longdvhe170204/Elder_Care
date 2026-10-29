package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.status.service.ResidentStatusService.HistoryEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/** Một dòng lịch sử trạng thái (contracts/api-001.yaml, schema {@code StatusHistoryEntry}). */
@Schema(name = "StatusHistoryEntry")
public record StatusHistoryEntryBody(
        ResidentStatus from,
        ResidentStatus to,
        String command,
        Instant at,
        String performedBy,
        boolean bySystem,
        String reason,
        Map<String, Object> basis,
        Map<String, Object> conditionWaiver) {

    public static StatusHistoryEntryBody of(HistoryEntry e) {
        return new StatusHistoryEntryBody(
                e.from(),
                e.to(),
                e.command(),
                e.at(),
                e.performedBy(),
                e.bySystem(),
                e.reason(),
                e.basis(),
                e.conditionWaiver());
    }
}
