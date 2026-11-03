package com.eldercare.backend.health.internal.catalog.domain;

import java.util.List;
import tools.jackson.databind.JsonNode;

/** Giá trị danh mục nội bộ; phiên bản dùng ETag, không phải trường HTTP bổ sung. */
public record VitalSignCatalogView(
        long id,
        String code,
        String name,
        String unit,
        boolean vitalSign,
        List<VitalSignComponentDefinition> components,
        JsonNode validRange,
        boolean missingValidRange,
        boolean active,
        int version) {
    public VitalSignCatalogView {
        components = List.copyOf(components);
    }
}
