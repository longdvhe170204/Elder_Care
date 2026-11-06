package com.eldercare.backend.nutrition.internal.catalog.domain;

import com.eldercare.backend.common.data.CatalogStatus;

/** Chế độ ăn đọc từ danh mục dùng chung. */
public record Diet(long id, String name, CatalogStatus status, DietAttributes attributes, int version) {

    public boolean active() {
        return status == CatalogStatus.ACTIVE;
    }
}
