package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.incident.internal.emergency.domain.TransferSummaryContent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Bản tóm tắt chuyển viện, schema {@code TransferSummary} của api-007 (FR-050, FR-050h). */
@Schema(name = "TransferSummary")
public record TransferSummaryResponse(
        Long id,
        String receivingFacility,
        List<Map<String, Object>> allergies,
        List<Map<String, Object>> currentMedications,
        List<Map<String, Object>> latestVitalSigns,
        List<Map<String, Object>> progressions,
        Map<String, Object> endOfLifeWish,
        Map<String, Object> treatmentWish,
        Map<String, Object> primaryContact,
        String companion,
        Long documentFileId,
        String preparedBy,
        Instant preparedAt) {

    public static TransferSummaryResponse of(RecordEvent<TransferSummaryContent> event) {
        var s = event.content();
        return new TransferSummaryResponse(
                event.id(),
                s.receivingFacility(),
                s.allergies(),
                s.currentMedications(),
                s.latestVitalSigns(),
                s.progressions(),
                s.endOfLifeWish(),
                s.treatmentWish(),
                TransferOutInfoResponse.contact(s.primaryContact()),
                s.companion(),
                s.documentFileId(),
                s.preparedBy(),
                s.preparedAt());
    }
}
