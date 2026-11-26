package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.DoseTransitionTable;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.dose.service.DoseViewService.DoseView;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionResponse;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ghép {@link DoseExtras} cho liều đã xem (BF-07). Chỉ đọc. */
@Service
public class DoseExtrasService {

    private final MedicationDoseRepository doses;
    private final DoseResultRepository results;
    private final PrescriptionRepository prescriptions;
    private final BusinessTime businessTime;
    private final ObjectMapper mapper = new ObjectMapper();

    public DoseExtrasService(
            MedicationDoseRepository doses,
            DoseResultRepository results,
            PrescriptionRepository prescriptions,
            BusinessTime businessTime) {
        this.doses = doses;
        this.results = results;
        this.prescriptions = prescriptions;
        this.businessTime = businessTime;
    }

    @Transactional(readOnly = true)
    public DoseExtras of(DoseView view) {
        var d = view.dose();
        Prescription prescription = d.prescriptionId() == null
                ? null
                : prescriptions.findById(d.prescriptionId()).orElse(null);
        MedicationDose dose =
                d.doseId() == null ? null : doses.findById(d.doseId()).orElse(null);
        DoseResult result =
                d.doseId() == null ? null : results.findByDoseId(d.doseId()).orElse(null);
        Instant now = businessTime.now();

        boolean postponed =
                dose != null && dose.getPostponedAt() != null && !DoseTransitionTable.isTerminal(dose.getStatus());
        List<DoseExtras.MeasurementValue> measured = List.of();
        if (result != null && result.getPreDoseMeasurements() != null) {
            try {
                measured = mapper.readValue(
                        result.getPreDoseMeasurements(), new TypeReference<List<DoseExtras.MeasurementValue>>() {});
            } catch (Exception ignored) {
                measured = List.of();
            }
        }
        return new DoseExtras(
                postponed,
                postponed ? dose.getPostponedReason() : null,
                null,
                null,
                prescription == null || prescription.getTimeCriticality() == null
                        ? "NORMAL"
                        : prescription.getTimeCriticality().name(),
                prescription == null
                        ? List.of()
                        : PrescriptionResponse.parseMeasurements(prescription.getPreDoseMeasurements()),
                result != null && result.isLateFlag(),
                measured,
                result == null ? null : result.getId());
    }
}
