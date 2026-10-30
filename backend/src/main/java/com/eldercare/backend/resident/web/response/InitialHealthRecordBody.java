package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecordContent;
import com.eldercare.backend.resident.internal.health.service.InitialHealthRecordService.RecordView;
import io.swagger.v3.oas.annotations.media.Schema;

/** Hồ sơ sức khỏe ban đầu (contracts/api-001.yaml, schema {@code InitialHealthRecord}), nội dung đã tính đính chính. */
@Schema(name = "InitialHealthRecord")
public record InitialHealthRecordBody(
        String bloodType,
        String mobility,
        String selfCareAbility,
        String cognition,
        String initialNeedsNote,
        String nutritionNeeds,
        String currentMedications,
        String rehabilitationNeeds,
        long id,
        boolean confirmed,
        String confirmedBy,
        boolean corrected) {

    public static InitialHealthRecordBody of(RecordView v) {
        InitialHealthRecordContent c = v.content();
        return new InitialHealthRecordBody(
                c.bloodType(),
                c.mobility(),
                c.selfCareAbility(),
                c.cognition(),
                c.initialNeedsNote(),
                c.nutritionNeeds(),
                c.currentMedications(),
                c.rehabilitationNeeds(),
                v.id(),
                v.confirmed(),
                v.confirmedBy(),
                v.corrected());
    }
}
