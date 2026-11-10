package com.eldercare.backend.health.web.response;

import com.eldercare.backend.health.internal.catalog.domain.VitalSignCatalogView;
import com.eldercare.backend.health.internal.catalog.domain.VitalSignComponentDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** Danh mục chỉ số trả về qua HTTP; phiên bản chỉ dùng trong ETag. */
@Schema(requiredProperties = {"code", "name", "unit", "components"})
public record VitalSignCatalogResponse(
        long id,
        String code,
        String name,
        String unit,
        boolean vitalSign,
        List<VitalSignComponentDefinition> components,
        JsonNode validRange,
        boolean missingValidRange,
        boolean active) {

    public static VitalSignCatalogResponse from(VitalSignCatalogView view) {
        return new VitalSignCatalogResponse(
                view.id(),
                view.code(),
                view.name(),
                view.unit(),
                view.vitalSign(),
                view.components(),
                view.validRange(),
                view.missingValidRange(),
                view.active());
    }
}
