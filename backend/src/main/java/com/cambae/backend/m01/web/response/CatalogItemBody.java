package com.cambae.backend.m01.web.response;

import com.cambae.backend.common.data.CatalogStatus;
import com.cambae.backend.m01.internal.health.domain.CatalogItemView;
import com.cambae.backend.m01.internal.health.service.CatalogService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Mục danh mục, dùng cho cả thân yêu cầu tạo, sửa và phản hồi (contracts/api-001.yaml, schema {@code CatalogItem}).
 * Khi tạo, sửa chỉ {@code name}, {@code group} (dị nguyên), {@code order} (mức độ dị ứng) được dùng; {@code id},
 * {@code status} do hệ thống đặt.
 */
@Schema(name = "CatalogItem")
public record CatalogItemBody(
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long id,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Chỉ với allergens")
        String group,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer order,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        CatalogStatus status) {

    public static CatalogItemBody of(CatalogItemView view) {
        return new CatalogItemBody(view.id(), view.name(), view.group(), view.order(), view.status());
    }

    public CatalogService.Input input() {
        return new CatalogService.Input(name, group, order);
    }
}
