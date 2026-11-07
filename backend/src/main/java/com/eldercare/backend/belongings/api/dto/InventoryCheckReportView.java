package com.eldercare.backend.belongings.api.dto;

/** Phiếu kiểm kê của một kỳ cho 016 (BR-M12-07): trạng thái, dấu trễ hạn. */
public record InventoryCheckReportView(
        long inventoryCheckId, long storageLocationId, String periodCode, String status, boolean late) {}
