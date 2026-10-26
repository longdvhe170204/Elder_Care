package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m07.api.dto.DoseDetailView;
import com.cambae.backend.m07.internal.domain.DoseResult;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.MedicationDose;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.persistence.DoseResultRepository;
import com.cambae.backend.m07.internal.persistence.MedicationDoseRepository;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for calculating medication schedules and medication administration rounds (FR-018 -> FR-023; research D-06, D-09).
 */
@Service
public class MedicationScheduleService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationDoseRepository doseRepository;
    private final DoseResultRepository doseResultRepository;
    private final MedicationScheduleCalculator scheduleCalculator;
    private final DoseAssigneeResolver assigneeResolver;

    public MedicationScheduleService(
            PrescriptionRepository prescriptionRepository,
            MedicationRepository medicationRepository,
            MedicationDoseRepository doseRepository,
            DoseResultRepository doseResultRepository,
            MedicationScheduleCalculator scheduleCalculator,
            DoseAssigneeResolver assigneeResolver) {
        this.prescriptionRepository = prescriptionRepository;
        this.medicationRepository = medicationRepository;
        this.doseRepository = doseRepository;
        this.doseResultRepository = doseResultRepository;
        this.scheduleCalculator = scheduleCalculator;
        this.assigneeResolver = assigneeResolver;
    }

    /**
     * Retrieves resident medication schedule for a date range (combines pure calculator and materialized doses).
     */
    @Transactional(readOnly = true)
    public List<CalculatedDoseSlot> getResidentSchedule(
            Long residentId, LocalDate fromDate, LocalDate toDate, Instant now) {

        Instant fromTime =
                fromDate.atStartOfDay(MedicationScheduleCalculator.ZONE_VN).toInstant();
        Instant toTime = toDate.atTime(LocalTime.MAX)
                .atZone(MedicationScheduleCalculator.ZONE_VN)
                .toInstant();

        List<Prescription> unended = prescriptionRepository.findUnendedByResidentId(residentId);
        List<CalculatedDoseSlot> allSlots = new ArrayList<>();

        // Load all materialized doses for this resident in the range
        List<MedicationDose> materializedDoses =
                doseRepository.findByResidentIdAndScheduledTimeBetween(residentId, fromTime, toTime);
        Map<String, MedicationDose> materializedMap = new HashMap<>();
        for (MedicationDose md : materializedDoses) {
            String key = String.format(
                    "%d-%s",
                    md.getPrescriptionId(),
                    MedicationScheduleCalculator.DOSE_KEY_FORMATTER.format(
                            md.getScheduledTime().atZone(MedicationScheduleCalculator.ZONE_VN)));
            materializedMap.put(key, md);
        }

        ScheduleContext context = ScheduleContext.empty(residentId);

        for (Prescription p : unended) {
            Medication med = medicationRepository.findById(p.getMedicationId()).orElse(null);
            List<CalculatedDoseSlot> slots = scheduleCalculator.calculate(p, med, fromTime, toTime, now, context);

            for (CalculatedDoseSlot slot : slots) {
                if (materializedMap.containsKey(slot.doseKey())) {
                    MedicationDose md = materializedMap.get(slot.doseKey());
                    allSlots.add(new CalculatedDoseSlot(
                            slot.doseKey(),
                            slot.prescriptionId(),
                            slot.residentId(),
                            slot.medicationId(),
                            slot.medicationName(),
                            slot.dosage(),
                            slot.dosageUnit(),
                            slot.route(),
                            slot.medicationSource(),
                            slot.specialControl(),
                            slot.scheduledTime(),
                            md.getWindowFrom(),
                            md.getWindowTo(),
                            md.getStatus(),
                            md.getCancellationReason(),
                            true,
                            md.getId()));
                } else {
                    allSlots.add(slot);
                }
            }
        }

        allSlots.sort(Comparator.comparing(CalculatedDoseSlot::scheduledTime));
        return allSlots;
    }

    /**
     * Retrieves list of doses for a medication round by floor area and time interval (FR-026, FR-026a).
     */
    @Transactional(readOnly = true)
    public List<DoseDetailView> getMedicationRound(Long floorAreaId, Instant fromTime, Instant toTime, Instant now) {

        List<MedicationDose> doses =
                doseRepository.findByFloorAreaIdAndScheduledTimeBetween(floorAreaId, fromTime, toTime);
        List<DoseDetailView> views = new ArrayList<>();

        for (MedicationDose d : doses) {
            DoseResult res = doseResultRepository.findByDoseId(d.getId()).orElse(null);
            Medication med = medicationRepository.findById(d.getMedicationId()).orElse(null);
            String medName = med != null ? med.getName() : "Medication";

            DoseAssigneeResolver.DoseAssignee assignee =
                    assigneeResolver.resolve(d.getResidentId(), d.getFloorAreaId(), d.getScheduledTime(), now);

            String doseKey = String.format(
                    "%d-%s",
                    d.getPrescriptionId(),
                    MedicationScheduleCalculator.DOSE_KEY_FORMATTER.format(
                            d.getScheduledTime().atZone(MedicationScheduleCalculator.ZONE_VN)));

            views.add(new DoseDetailView(
                    doseKey,
                    d.getId(),
                    d.getPrescriptionId(),
                    d.getResidentId(),
                    d.getMedicationId(),
                    medName,
                    d.getDosage(),
                    d.getDosageUnit(),
                    med != null ? med.getDefaultRoute() : "Oral",
                    d.getMedicationSource(),
                    d.isSpecialControl(),
                    d.getScheduledTime(),
                    d.getWindowFrom(),
                    d.getWindowTo(),
                    d.getStatus(),
                    d.getCancellationReason(),
                    res != null ? res.getAdministeredAt() : null,
                    res != null ? res.getResultType() : null,
                    res != null ? res.getAuthorityBasis() : null,
                    res != null ? res.getRecorderId() : null,
                    res != null ? res.getReason() : null,
                    res != null ? res.getAdverseReaction() : null,
                    res != null ? res.getNote() : null,
                    res != null && res.isLateFlag(),
                    res != null && res.isOfflineFlag(),
                    assignee.assignedNurseId(),
                    assignee.floorShared()));
        }

        views.sort(Comparator.comparing(DoseDetailView::scheduledTime));
        return views;
    }
}
