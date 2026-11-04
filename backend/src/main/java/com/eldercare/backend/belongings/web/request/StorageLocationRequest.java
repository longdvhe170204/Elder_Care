package com.eldercare.backend.belongings.web.request;

/** Thân tạo, sửa, ngừng vị trí lưu giữ (api-013 {@code /belongings/storage-locations}); có {@code id} là sửa. */
public record StorageLocationRequest(Long id, String name, String description, Long floorAreaId, String status) {}
