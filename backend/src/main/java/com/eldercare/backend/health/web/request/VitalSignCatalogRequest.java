package com.eldercare.backend.health.web.request;

import com.eldercare.backend.health.internal.catalog.domain.VitalSignCatalogInput;
import com.eldercare.backend.health.internal.catalog.domain.VitalSignComponentDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Nội dung thêm hoặc sửa danh mục chỉ số theo hợp đồng 007 (schema {@code VitalSignIndicator} dùng chung cho đọc, ghi):
 * {@code id} lấy theo đường dẫn, {@code active} đổi bằng lệnh Ngừng hiệu lực, hai trường bị bỏ qua khi ghi.
 */
public record VitalSignCatalogRequest(
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String unit,
        boolean vitalSign,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ComponentRequest> components,
        Boolean active) {

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
