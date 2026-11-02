package com.eldercare.backend.health.web.request;

import com.eldercare.backend.health.internal.catalog.domain.VitalSignCatalogInput;
import com.eldercare.backend.health.internal.catalog.domain.VitalSignComponentDefinition;
import java.util.List;

/** Nội dung thêm hoặc sửa danh mục chỉ số theo hợp đồng 007. */
public record VitalSignCatalogRequest(
        String code, String name, String unit, boolean vitalSign, List<ComponentRequest> components) {

    public VitalSignCatalogInput toInput() {
        return new VitalSignCatalogInput(
                code,
                name,
                unit,
                vitalSign,
                components == null
                        ? null
                        : components.stream()
                                .map(ComponentRequest::toDefinition)
                                .toList());
    }

    public record ComponentRequest(String code, String name, int decimalPlaces) {
        VitalSignComponentDefinition toDefinition() {
            return new VitalSignComponentDefinition(code, name, decimalPlaces);
        }
    }
}
