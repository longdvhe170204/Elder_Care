package com.eldercare.backend.administration.shared.api.dto;

/** Mục danh mục mới; {@code code} {@code null} khi loại không có mã. */
public record NewCatalogItem<T>(String code, String name, Long parentId, int sortOrder, T attributes) {}
