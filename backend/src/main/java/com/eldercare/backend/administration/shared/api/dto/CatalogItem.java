package com.eldercare.backend.administration.shared.api.dto;

import com.eldercare.backend.common.data.CatalogStatus;
import java.time.Instant;

/** Một mục của {@code catalog_item} (nhóm 1, data-model 000 mục 8). */
public record CatalogItem<T>(
        long id,
        String code,
        String name,
        Long parentId,
        int sortOrder,
        CatalogStatus status,
        T attributes,
        Long confirmedBy,
        Instant confirmedAt,
        int version) {}
