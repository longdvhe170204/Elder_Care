package com.eldercare.backend.administration.shared.internal.domain;

import com.eldercare.backend.common.data.CatalogStatus;
import java.time.Instant;

/** Một dòng {@code catalog_item} như lưu; {@code attributes} là JSON chưa đọc theo loại. */
public record CatalogRow(
        long id,
        String code,
        String name,
        Long parentId,
        int sortOrder,
        CatalogStatus status,
        String attributes,
        Long confirmedBy,
        Instant confirmedAt,
        int version) {}
