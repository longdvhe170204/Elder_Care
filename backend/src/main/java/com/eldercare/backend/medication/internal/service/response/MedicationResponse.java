package com.eldercare.backend.medication.internal.service.response;

import com.eldercare.backend.medication.internal.service.MedicationService.MedicationDetailView;
import java.util.List;

/**
 * Medication detail response per OpenAPI contract api-006.yaml (schema Thuoc).
 */
public record MedicationResponse(
        Long id,
        String name,
        String concentration,
        String dosageForm,
        String defaultRoute,
        String usageUnit,
        List<Long> allergenIds,
        Integer beforeWindowMinutes,
        Integer afterWindowMinutes,
        boolean specialControl,
        boolean active,
        boolean missingPricingItem,
        int version) {

    public static MedicationResponse from(MedicationDetailView view) {
        return new MedicationResponse(
                view.id(),
                view.name(),
                view.concentration(),
                view.dosageForm(),
                view.defaultRoute(),
                view.usageUnit(),
                view.allergenIds(),
                view.beforeWindowMinutes(),
                view.afterWindowMinutes(),
                view.specialControl(),
                view.active(),
                view.missingPricingItem(),
                view.version());
    }
}
