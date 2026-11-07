package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/** Phạm vi, sự cố liên quan và lý do theo NewIsolation của api-007. */
@Schema(name = "NewIsolation")
public record NewIsolationRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Scope scope,

        @ArraySchema(
                minItems = 1,
                arraySchema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(type = "integer", format = "int64"))
        Set<Long> incidentIds,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {

    public ContainmentScope toScope() {
        if (scope == null) throw new InvalidValueException("Thiếu phạm vi khoanh vùng");
        try {
            return new ContainmentScope(
                    scope.floorIds() == null ? Set.of() : scope.floorIds(),
                    scope.areaId() == null ? Set.of() : Set.of(scope.areaId()));
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new InvalidValueException("Chọn một hoặc nhiều tầng, hoặc đúng một khu vực");
        }
    }

    @Schema(name = "IsolationScope")
    public record Scope(Set<Long> floorIds, Long areaId) {}
}
