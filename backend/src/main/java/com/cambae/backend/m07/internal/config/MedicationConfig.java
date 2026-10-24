package com.cambae.backend.m07.internal.config;

import com.cambae.backend.m07.internal.service.MedicationCatalogPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Internal configuration for Module M07 - Medication Management.
 */
@Configuration
public class MedicationConfig {

    @Bean
    @ConditionalOnMissingBean(MedicationCatalogPort.class)
    public MedicationCatalogPort defaultMedicationCatalogPort() {
        // Default fallback when module 004 is not yet active: treat item as absent from facility items
        return medicationId -> false;
    }
}
