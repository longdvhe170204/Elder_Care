package com.eldercare.backend.belongings.internal.catalog.domain;

/** Thuộc tính vị trí lưu giữ (FR-002): mô tả, tầng/khu vực nếu vị trí đặt tại tầng. */
public record StorageLocationAttributes(String description, Long floorAreaId) {}
