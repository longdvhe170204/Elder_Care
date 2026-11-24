package com.eldercare.backend.belongings.internal.inventorycheck.config;

import com.eldercare.backend.belongings.internal.inventorycheck.service.InventoryCheckService;
import com.eldercare.backend.belongings.internal.inventorycheck.service.InventorySourceAccess;
import com.eldercare.backend.notification.api.spi.SourceRecordAccessPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ba bản cài {@link SourceRecordAccessPort} của các nguồn kiểm kê (test luồng BF-14 2026-11-23). */
@Configuration
class InventorySourceAccessConfig {

    @Bean
    SourceRecordAccessPort inventoryCheckSourceAccess(InventoryCheckService checks) {
        return InventorySourceAccess.all(checks).get(0);
    }

    @Bean
    SourceRecordAccessPort inventoryPeriodSourceAccess(InventoryCheckService checks) {
        return InventorySourceAccess.all(checks).get(1);
    }

    @Bean
    SourceRecordAccessPort storageLocationSourceAccess(InventoryCheckService checks) {
        return InventorySourceAccess.all(checks).get(2);
    }
}
