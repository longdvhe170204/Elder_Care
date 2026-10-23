package com.cambae.backend.m07.internal.config;

import com.cambae.backend.m07.internal.service.MedicationCatalogPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình nội bộ cho Module M07 - Quản lý thuốc.
 */
@Configuration
public class MedicationConfig {

    @Bean
    @ConditionalOnMissingBean(MedicationCatalogPort.class)
    public MedicationCatalogPort defaultMedicationCatalogPort() {
        // Mặc định khi chưa có module 004 chạy thật: coi như chưa có mục vật phẩm viện
        return medicationId -> false;
    }
}
