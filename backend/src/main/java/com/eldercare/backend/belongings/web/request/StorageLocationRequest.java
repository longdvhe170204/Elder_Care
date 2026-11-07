package com.eldercare.backend.belongings.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân tạo, sửa, ngừng vị trí lưu giữ (api-013 {@code /belongings/storage-locations}); có {@code id} là sửa. */
public record StorageLocationRequest(
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        String description,
        Long floorAreaId,
        String status) {}
