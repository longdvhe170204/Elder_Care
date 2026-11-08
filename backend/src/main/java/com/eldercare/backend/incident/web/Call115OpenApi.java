package com.eldercare.backend.incident.web;

import io.swagger.v3.oas.models.media.Schema;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * api-007 khai {@code Call115Attempt.result} là {@code [string, null]} với enum có {@code null} (chưa chọn kết quả);
 * springdoc không sinh được phần tử {@code null} từ annotation nên thêm ở đây cho đặc tả sinh khớp hợp đồng (R-14).
 */
@Component
class Call115OpenApi implements OpenApiCustomizer {

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void customise(io.swagger.v3.oas.models.OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) return;
        Schema attempt = openApi.getComponents().getSchemas().get("Call115Attempt");
        if (attempt == null || attempt.getProperties() == null) return;
        Schema result = (Schema) attempt.getProperties().get("result");
        if (result == null || result.getEnum() == null || result.getEnum().contains(null)) return;
        result.setTypes(Set.of("string", "null"));
        result.getEnum().add(null);
    }
}
