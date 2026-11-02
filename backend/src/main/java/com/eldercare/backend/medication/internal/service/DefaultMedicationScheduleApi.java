package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.medication.api.MedicationScheduleApi;
import com.eldercare.backend.medication.api.dto.DoseDisplayView;
import com.eldercare.backend.medication.api.dto.MedicationSummary;
import com.eldercare.backend.medication.internal.domain.DoseResult;
import com.eldercare.backend.medication.internal.domain.Medication;
import com.eldercare.backend.medication.internal.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link MedicationScheduleApi} (LichThuocApi; research D-06, FR-018 -> FR-023).
 */
@Service
public class DefaultMedicationScheduleApi implements MedicationScheduleApi {

    private final MedicationScheduleService scheduleService;
    private final MedicationRepository medicationRepository;
    private final MedicationDoseRepository doseRepository;
    private final DoseResultRepository doseResultRepository;

    public DefaultMedicationScheduleApi(
            MedicationScheduleService scheduleService,
            MedicationRepository medicationRepository,
            MedicationDoseRepository doseRepository,
            DoseResultRepository doseResultRepository) {
        this.scheduleService = scheduleService;
        this.medicationRepository = medicationRepository;
        this.doseRepository = doseRepository;
        this.doseResultRepository = doseResultRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoseDisplayView> byResident(long residentId, Instant from, Instant to) {
        LocalDate fromDate = from.atZone(MedicationScheduleCalculator.ZONE_VN).toLocalDate();
        LocalDate toDate = to.atZone(MedicationScheduleCalculator.ZONE_VN).toLocalDate();

        List<CalculatedDoseSlot> slots = scheduleService.getResidentSchedule(residentId, fromDate, toDate, from);
        List<DoseDisplayView> views = new ArrayList<>();

        for (CalculatedDoseSlot slot : slots) {
            Medication med = medicationRepository.findById(slot.medicationId()).orElse(null);
            Optional<MedicationSummary> medSummary = med != null
                    ? Optional.of(new MedicationSummary(
                            med.getId(), med.getName(), "", med.getConcentration(), med.getDosageForm()))
                    : Optional.empty();

            com.eldercare.backend.medication.api.dto.DoseStatus apiStatus = mapToApiStatus(slot.status());

            views.add(new DoseDisplayView(
                    slot.doseKey(),
                    slot.residentId(),
                    slot.prescriptionId(),
                    slot.scheduledTime(),
                    slot.windowFrom(),
                    slot.windowTo(),
                    apiStatus,
                    false,
                    false,
                    false,
                    slot.specialControl(),
                    slot.medicationSource(),
                    Optional.ofNullable(slot.materializedDoseId()),
                    medSummary));
        }

        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> staffRecordedDose(long residentId, Instant from, Instant to) {
        List<DoseResult> results = doseResultRepository.findByResidentIdOrderByRecordedAtDesc(residentId);
        Set<Long> staffIds = new HashSet<>();
        for (DoseResult r : results) {
            if (r.getRecorderId() != null
                    && !r.getRecordedAt().isBefore(from)
                    && !r.getRecordedAt().isAfter(to)) {
                staffIds.add(r.getRecorderId());
            }
        }
        return staffIds;
    }

    private com.eldercare.backend.medication.api.dto.DoseStatus mapToApiStatus(
            com.eldercare.backend.medication.internal.domain.DoseStatus domainStatus) {
        if (domainStatus == null) {
            return com.eldercare.backend.medication.api.dto.DoseStatus.SCHEDULED;
        }
        return switch (domainStatus) {
            case UPCOMING -> com.eldercare.backend.medication.api.dto.DoseStatus.SCHEDULED;
            case DUE -> com.eldercare.backend.medication.api.dto.DoseStatus.DUE;
            case LATE -> com.eldercare.backend.medication.api.dto.DoseStatus.LATE;
            case MISSED -> com.eldercare.backend.medication.api.dto.DoseStatus.MISSED;
            case ADMINISTERED -> com.eldercare.backend.medication.api.dto.DoseStatus.ADMINISTERED;
            case REFUSED -> com.eldercare.backend.medication.api.dto.DoseStatus.REFUSED;
            case NOT_ADMINISTERED -> com.eldercare.backend.medication.api.dto.DoseStatus.NOT_ADMINISTERED;
            case SUSPENDED -> com.eldercare.backend.medication.api.dto.DoseStatus.SUSPENDED;
            case LEAVE_MEDICATION -> com.eldercare.backend.medication.api.dto.DoseStatus.DISPENSED_FOR_TRIP;
            case CANCELLED -> com.eldercare.backend.medication.api.dto.DoseStatus.CANCELLED;
        };
    }
}
