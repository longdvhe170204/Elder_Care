package com.eldercare.backend.administration.account.api.spi;

/** Phân bổ giường đang hiệu lực: giường → phòng → tầng/khu vực (003 contracts mục 1.1, {@code PhanBoView}). */
public record BedAllocationView(long allocationId, long bedId, long roomId, long floorAreaId) {}
