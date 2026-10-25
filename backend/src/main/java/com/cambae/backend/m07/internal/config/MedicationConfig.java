package com.cambae.backend.m07.internal.config;

import com.cambae.backend.m07.api.spi.ShiftNursePort;
import com.cambae.backend.m07.internal.service.DefaultShiftNursePort;
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

    @Bean
    @ConditionalOnMissingBean(com.cambae.backend.m07.internal.service.HealthAllergyPort.class)
    public com.cambae.backend.m07.internal.service.HealthAllergyPort defaultHealthAllergyPort() {
        return new com.cambae.backend.m07.internal.service.HealthAllergyPort() {
            @Override
            public java.util.List<com.cambae.backend.m07.internal.service.AllergyItemView> getActiveAllergies(
                    long residentId) {
                return java.util.Collections.emptyList();
            }

            @Override
            public void markAllergyChecked(long allergyId, String module) {
                // Default fallback: no-op
            }
        };
    }
}
