package com.eldercare.backend.medication.internal.config;

import com.eldercare.backend.medication.api.spi.ShiftNursePort;
import com.eldercare.backend.medication.internal.service.DefaultShiftNursePort;
import com.eldercare.backend.medication.internal.service.MedicationCatalogPort;
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

    @Bean
    @ConditionalOnMissingBean(ShiftNursePort.class)
    public ShiftNursePort defaultShiftNursePort() {
        return new DefaultShiftNursePort();
    }

    @Bean
    @ConditionalOnMissingBean(com.fasterxml.jackson.databind.ObjectMapper.class)
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
