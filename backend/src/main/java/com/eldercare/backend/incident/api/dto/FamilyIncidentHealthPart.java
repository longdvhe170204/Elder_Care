package com.eldercare.backend.incident.api.dto;

import java.util.List;
import java.util.Optional;

/** Phần sức khỏe của sự cố, chỉ trả khi người thân thuộc phạm vi chia sẻ đang hiệu lực (research S-19a). */
public record FamilyIncidentHealthPart(
        String description,
        String initialResponse,
        List<String> progressions,
        Optional<String> result,
        List<String> examinations,
        List<String> vitalSigns,
        List<String> treatments,
        List<String> medications,
        Optional<String> medicalCause,
        Optional<String> transfer) {

    public FamilyIncidentHealthPart {
        progressions = List.copyOf(progressions);
        examinations = List.copyOf(examinations);
        vitalSigns = List.copyOf(vitalSigns);
        treatments = List.copyOf(treatments);
        medications = List.copyOf(medications);
    }
}
