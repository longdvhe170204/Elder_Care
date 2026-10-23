package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.HealthItemType;
import com.cambae.backend.m01.internal.health.domain.HealthItemStatus;
import com.cambae.backend.m01.internal.health.service.HealthItemService.ItemView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Một mục sức khỏe (contracts/api-001.yaml, schema {@code HealthItem}). */
@Schema(name = "HealthItem")
public record HealthItemBody(
        long id,
        HealthItemType type,
        AllergenRef allergen,
        String freeText,
        boolean notAutoChecked,
        boolean pendingCheck,
        String severity,
        String source,
        String recordedBy,
        Instant at,
        HealthItemStatus status,
        ExclusionBody exclusion) {

    public record AllergenRef(long id, String name) {}

    public record ExclusionBody(Instant at, String by, String reason) {}

    public static HealthItemBody of(ItemView v) {
        return new HealthItemBody(
                v.id(),
                v.type(),
                v.allergen() == null
                        ? null
                        : new AllergenRef(v.allergen().id(), v.allergen().name()),
                v.freeText(),
                v.notAutoChecked(),
                v.pendingCheck(),
                v.severity(),
                v.source(),
                v.recordedBy(),
                v.at(),
                v.status(),
                v.exclusion() == null
                        ? null
                        : new ExclusionBody(
                                v.exclusion().at(),
                                v.exclusion().by(),
                                v.exclusion().reason()));
    }
}
