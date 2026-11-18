package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.internal.assessment.domain.FamilyDeclaredTopic;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.FamilyDeclaredInput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Thân lệnh ghi phần "Gia đình khai" (contracts/api-001.yaml, PUT
 * {@code /residents/{id}/initial-assessment/family-declared}; Q-412, FR-025a).
 */
public record FamilyDeclaredBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<@Valid Entry> entries) {

    @Schema(
            name = "FamilyDeclaredInput",
            requiredProperties = {"topic", "content", "source"})
    public record Entry(
            @NotNull FamilyDeclaredTopic topic,
            @NotBlank @Size(max = 2000) String content,
            @NotNull OpinionSource source) {}

    public List<FamilyDeclaredInput> input() {
        return entries.stream()
                .map(e -> new FamilyDeclaredInput(e.topic(), e.content(), e.source()))
                .toList();
    }
}
