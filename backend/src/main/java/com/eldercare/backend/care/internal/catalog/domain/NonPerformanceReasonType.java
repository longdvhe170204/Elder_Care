package com.eldercare.backend.care.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.common.data.CatalogStatus;

/** Snapshot danh mục từ CatalogApi; không còn entity/bảng riêng sau R-15. */
public record NonPerformanceReasonType(
        long catalogId, long id, String name, boolean fixed, CatalogStatus status, int version) {
    public static NonPerformanceReasonType of(CatalogItem<ReasonAttributes> row) {
        var legacy = row.attributes().legacy();
        return new NonPerformanceReasonType(
                row.id(),
                legacy == null ? row.id() : legacy.id(),
                row.name(),
                row.attributes().fixed(),
                row.status(),
                row.version());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CatalogStatus getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }
}
