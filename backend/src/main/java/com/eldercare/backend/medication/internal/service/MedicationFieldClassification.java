package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Field classification for Medication Catalog (FR-001, FR-002, data-model section 12).
 * Fields name, concentration, dosageForm, defaultRoute, usageUnit, allergenIds belong to {@link InfoGroup#HEALTH}.
 */
@Component
public class MedicationFieldClassification implements FieldClassificationProvider {

    public static final String TYPE = "M07.MEDICATION";

    @Override
    public String entityType() {
        return TYPE;
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.of(
                "name", InfoGroup.HEALTH,
                "concentration", InfoGroup.HEALTH,
                "dosageForm", InfoGroup.HEALTH,
                "defaultRoute", InfoGroup.HEALTH,
                "usageUnit", InfoGroup.HEALTH,
                "allergenIds", InfoGroup.HEALTH);
    }
}
