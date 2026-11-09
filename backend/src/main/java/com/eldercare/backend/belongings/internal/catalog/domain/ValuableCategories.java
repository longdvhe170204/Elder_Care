package com.eldercare.backend.belongings.internal.catalog.domain;

import java.util.List;

/** Giá trị CFG-M12-01: tên loại đồ có giá trị bắt buộc chụp ảnh (FR-009). */
public record ValuableCategories(List<String> categories) {

    public ValuableCategories {
        categories = categories == null ? List.of() : List.copyOf(categories);
    }

    public boolean contains(String typeName) {
        return categories.stream().anyMatch(c -> c.equalsIgnoreCase(typeName.strip()));
    }
}
