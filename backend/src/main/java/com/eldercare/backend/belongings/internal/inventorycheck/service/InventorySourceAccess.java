package com.eldercare.backend.belongings.internal.inventorycheck.service;

import com.eldercare.backend.notification.api.spi.SourceRecordAccessPort;
import java.util.List;

/**
 * Cài {@link SourceRecordAccessPort} của 009 cho các nguồn kiểm kê (phiếu, kỳ, vị trí lưu giữ; test luồng BF-14
 * 2026-11-23): mở được khi người dùng xem được tab Kiểm kê (Hành chính, Quản lý viện), như
 * {@code GET /belonging-inventory-checks}. Đăng ký bean ở {@code InventorySourceAccessConfig}.
 */
public final class InventorySourceAccess implements SourceRecordAccessPort {

    private final String type;
    private final InventoryCheckService checks;

    private InventorySourceAccess(String type, InventoryCheckService checks) {
        this.type = type;
        this.checks = checks;
    }

    /** Ba nguồn: phiếu kiểm kê, kỳ kiểm kê, vị trí lưu giữ. */
    public static List<InventorySourceAccess> all(InventoryCheckService checks) {
        return List.of(
                new InventorySourceAccess(InventoryCheckService.RECORD_TYPE, checks),
                new InventorySourceAccess("M12.INVENTORY_PERIOD", checks),
                new InventorySourceAccess("M12.STORAGE_LOCATION", checks));
    }

    @Override
    public String sourceRecordType() {
        return type;
    }

    @Override
    public boolean canOpen(String sourceRecordId) {
        return checks.canRead();
    }
}
