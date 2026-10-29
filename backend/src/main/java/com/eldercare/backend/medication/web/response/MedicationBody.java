package com.eldercare.backend.medication.web.response;

import com.eldercare.backend.medication.internal.service.MedicationService.MedicationDetailView;
import com.eldercare.backend.medication.internal.service.response.MedicationResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

/** api-006 {@code Medication} trong phản hồi (FR-001, FR-003); phiên bản đi ở ETag. */
public record MedicationBody(
        Long id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = RequiredMode.REQUIRED) String concentration,
        @Schema(requiredMode = RequiredMode.REQUIRED) String dosageForm,
        @Schema(requiredMode = RequiredMode.REQUIRED) String defaultRoute,
        @Schema(requiredMode = RequiredMode.REQUIRED) String usageUnit,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<Long> allergenIds,
        Integer beforeWindowMinutes,
        Integer afterWindowMinutes,
        Boolean specialControl,
        Boolean active,
        Boolean missingPricingItem) {

    public static MedicationBody of(MedicationDetailView view) {
        MedicationResponse r = MedicationResponse.from(view);
        return new MedicationBody(
                r.id(),
                r.name(),
                r.concentration(),
                r.dosageForm(),
                r.defaultRoute(),
                r.usageUnit(),
                r.allergenIds(),
                r.beforeWindowMinutes(),
                r.afterWindowMinutes(),
                r.specialControl(),
                r.active(),
                r.missingPricingItem());
    }
}
