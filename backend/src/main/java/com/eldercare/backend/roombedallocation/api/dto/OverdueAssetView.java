package com.eldercare.backend.roombedallocation.api.dto;

import java.time.LocalDate;

/** 019 contracts mục 2.3 (TaiSanQuaHanView): một dấu quá hạn của một tài sản; floorAreaId rỗng khi ở kho, bãi xe. */
public record OverdueAssetView(
        long assetId, String code, String name, long departmentId, Long floorAreaId, AssetFlag flag, LocalDate dueOn) {}
