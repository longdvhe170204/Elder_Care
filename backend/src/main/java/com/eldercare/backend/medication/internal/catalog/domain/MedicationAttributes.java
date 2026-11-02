package com.eldercare.backend.medication.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.util.List;

/** Nội dung JSON của danh mục thuốc M07 trong {@code catalog_item}. */
public record MedicationAttributes(
        String name,
        String concentration,
        String dosageForm,
        String defaultRoute,
        String usageUnit,
        Integer beforeWindowMinutes,
        Integer afterWindowMinutes,
        boolean specialControl,
        List<Long> activeIngredientIds,
        LegacyMedication legacy) {

    public static final SharedKind<MedicationAttributes> KIND =
            SharedKind.of("M07.MEDICATION", MedicationAttributes.class).notDeletable();

    public record LegacyMedication(String table, Long id) {}
}
