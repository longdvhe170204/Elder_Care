package com.eldercare.backend.health.internal.catalog.domain;

import java.util.List;

/** Nội dung lệnh danh mục; khoảng vật lý chỉ đọc từ CFG-M06-03. */
public record VitalSignCatalogInput(
        String code, String name, String unit, boolean vitalSign, List<VitalSignComponentDefinition> components) {}
