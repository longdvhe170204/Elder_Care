package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m04.api.MedicationChecklistItem;
import com.cambae.backend.m04.api.MedicationDoseChecklistPort;
import com.cambae.backend.m04.api.ScheduleSourcePort;
import com.cambae.backend.m07.api.MedicationScheduleApi;
import com.cambae.backend.m07.api.dto.DoseDisplayView;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing SPI ports for Module M04 (005): {@link MedicationDoseChecklistPort} and {@link ScheduleSourcePort}.
 */
@Component
public class MedicationChecklistPortAdapter implements MedicationDoseChecklistPort, ScheduleSourcePort {

    private final MedicationScheduleApi medicationScheduleApi;

    public MedicationChecklistPortAdapter(MedicationScheduleApi medicationScheduleApi) {
        this.medicationScheduleApi = medicationScheduleApi;
    }

    @Override
    public List<MedicationChecklistItem> byShift(Collection<Long> residentIds, Instant from, Instant to) {
        if (residentIds == null || residentIds.isEmpty()) {
            return List.of();
        }
        List<MedicationChecklistItem> results = new ArrayList<>();
        for (Long resId : residentIds) {
            for (DoseDisplayView view : medicationScheduleApi.byResident(resId, from, to)) {
                results.add(toItem(view));
            }
        }
        return results;
    }

    @Override
    public List<MedicationChecklistItem> byDate(long residentId, LocalDate date) {
        Instant from = date.atStartOfDay(MedicationScheduleCalculator.ZONE_VN).toInstant();
        Instant to = date.atTime(LocalTime.MAX)
                .atZone(MedicationScheduleCalculator.ZONE_VN)
                .toInstant();
        return medicationScheduleApi.byResident(residentId, from, to).stream()
                .map(this::toItem)
                .toList();
    }

    private MedicationChecklistItem toItem(DoseDisplayView v) {
        String medName = v.medication().isPresent() ? v.medication().get().name() : "";
        String dosage = v.medication().isPresent() ? v.medication().get().strength() : "";
        return new MedicationChecklistItem(
                v.doseKey(),
                v.residentId(),
                v.prescriptionId(),
                v.scheduledTime(),
                v.windowStart(),
                v.windowEnd(),
                v.status() != null ? v.status().name() : null,
                v.floorSharedDose(),
                v.source() != null ? v.source().name() : null,
                medName,
                dosage);
    }
}
