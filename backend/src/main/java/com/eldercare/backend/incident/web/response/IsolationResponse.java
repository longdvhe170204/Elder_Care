package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentContent;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;

/** Phản hồi khoanh vùng, chỉ gồm trường của Isolation trong api-007. */
@Schema(name = "Isolation")
public record IsolationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Scope scope,

        @ArraySchema(
                minItems = 1,
                arraySchema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(type = "integer", format = "int64"))
        Set<Long> incidentIds,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 2000)
        String reason,

        long id,
        @Schema(allowableValues = {"ACTIVE", "LIFTED"}) String status,
        Instant startedAt,
        Instant endedAt) {
    public static IsolationResponse from(SimpleRecord<ContainmentContent> row) {
        var content = row.content();
        var period = content.period();
        var scope = period.scope();
        return new IsolationResponse(
                new Scope(scope.floorIds(), scope.areaIds().stream().findFirst().orElse(null)),
                content.incidentIds(),
                content.reason(),
                row.id(),
                period.releasedAt() == null ? "ACTIVE" : "LIFTED",
                period.startedAt(),
                period.releasedAt());
    }

    @Schema(name = "IsolationScope")
    public record Scope(Set<Long> floorIds, Long areaId) {}
}
