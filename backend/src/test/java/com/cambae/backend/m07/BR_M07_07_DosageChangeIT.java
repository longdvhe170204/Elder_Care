package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.internal.domain.DoseCancellationReason;
import com.cambae.backend.m07.internal.domain.DoseStatus;
import com.cambae.backend.m07.internal.domain.FrequencyType;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.MedicationDose;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import com.cambae.backend.m07.internal.domain.PrescriptionType;
import com.cambae.backend.m07.internal.persistence.MedicationDoseRepository;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import com.cambae.backend.m07.internal.service.PrescriptionService;
import com.cambae.backend.m07.web.request.ChangeDosageBody;
import com.cambae.backend.m07.web.request.CreatePrescriptionBody;
import com.cambae.backend.m07.web.request.FrequencyBody;
import com.cambae.backend.m07.web.request.PrescriberBody;
import com.cambae.backend.m07.web.response.PrescriptionResponse;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * T030: Test BR-M07-07 & FR-016: Changing dosage or discontinuing a prescription cancels open doses
 * from the effective time, while preserving already-administered doses.
 */
class BR_M07_07_DosageChangeIT extends MySqlIntegrationTestBase {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private MedicationDoseRepository doseRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private Long residentId = 8802L;
    private Long medId = 8802L;

    @BeforeEach
    void setUp() {
        jdbc.update(
                "INSERT INTO resident_medication (resident_id, version) VALUES (?, 0) "
                        + "ON DUPLICATE KEY UPDATE version = version",
                residentId);

        Medication med =
                new Medication("Metformin 500mg", "500mg", "Viên nén", "Oral", "viên", 30, 30, false, Set.of(102L));
        medicationRepository.save(med);
        medId = med.getId();
    }

    @Test
    @DisplayName("BR-M07-07: Changing dosage cancels unclosed doses from effective time, but keeps administered doses")
    void changeDosage_cancelsUnclosedDoses_keepsAdministered() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // 1. Create prescription
        Prescription p = new Prescription(
                residentId,
                medId,
                new BigDecimal("500"),
                "mg",
                "Oral",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\", \"12:00\", \"20:00\"]",
                null,
                null,
                null,
                null,
                null,
                now.minus(1, ChronoUnit.DAYS),
                null,
                MedicationSource.FACILITY,
                com.cambae.backend.m07.internal.domain.PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                "Sau ăn",
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                10L,
                now.minus(1, ChronoUnit.DAYS),
                Set.of(102L));
        Prescription savedOld = prescriptionRepository.save(p);
        Long oldId = savedOld.getId();

        // 2. Materialize three doses:
        // Dose 1: 08:00 today - already administered
        Instant dose1Time = now.minus(4, ChronoUnit.HOURS);
        MedicationDose dose1 = new MedicationDose(
                oldId,
                residentId,
                medId,
                dose1Time,
                dose1Time.minus(30, ChronoUnit.MINUTES),
                dose1Time.plus(30, ChronoUnit.MINUTES),
                new BigDecimal("500"),
                "mg",
                MedicationSource.FACILITY,
                false,
                DoseStatus.ADMINISTERED,
                1L);
        doseRepository.save(dose1);

        // Dose 2: 12:00 today - due (open)
        Instant dose2Time = now.plus(1, ChronoUnit.HOURS);
        MedicationDose dose2 = new MedicationDose(
                oldId,
                residentId,
                medId,
                dose2Time,
                dose2Time.minus(30, ChronoUnit.MINUTES),
                dose2Time.plus(30, ChronoUnit.MINUTES),
                new BigDecimal("500"),
                "mg",
                MedicationSource.FACILITY,
                false,
                DoseStatus.DUE,
                1L);
        doseRepository.save(dose2);

        // Dose 3: 20:00 today - upcoming (open)
        Instant dose3Time = now.plus(7, ChronoUnit.HOURS);
        MedicationDose dose3 = new MedicationDose(
                oldId,
                residentId,
                medId,
                dose3Time,
                dose3Time.minus(30, ChronoUnit.MINUTES),
                dose3Time.plus(30, ChronoUnit.MINUTES),
                new BigDecimal("500"),
                "mg",
                MedicationSource.FACILITY,
                false,
                DoseStatus.UPCOMING,
                1L);
        doseRepository.save(dose3);

        // 3. Change dosage from 10:00 today (effective immediately)
        CreatePrescriptionBody newBody = new CreatePrescriptionBody();
        newBody.setMedicationId(medId);
        newBody.setDosage(new BigDecimal("1000"));
        newBody.setDosageUnit("mg");
        newBody.setRoute("Oral");
        newBody.setPrescriptionType("PERIODIC");

        FrequencyBody freq = new FrequencyBody();
        freq.setType("TIMED_SLOTS");
        freq.setTimedSlots(List.of("08:00", "20:00"));
        newBody.setFrequency(freq);
        newBody.setStartedAt(now);
        newBody.setMedicationSource("FACILITY");

        PrescriberBody prescriber = new PrescriberBody();
        prescriber.setType("INTERNAL");
        prescriber.setPrescriberId(10L);
        newBody.setPrescriber(prescriber);

        ChangeDosageBody changeCmd = new ChangeDosageBody();
        changeCmd.setNewPrescription(newBody);
        changeCmd.setEffectiveFrom(now);
        changeCmd.setReason("Đổi sang liều 1000mg");

        PrescriptionResponse replacement = prescriptionService.changeDosage(oldId, changeCmd, 10L, true);

        // 4. Assertions:
        // Dose 1 remains ADMINISTERED
        MedicationDose reloadedDose1 = doseRepository.findById(dose1.getId()).orElseThrow();
        assertThat(reloadedDose1.getStatus()).isEqualTo(DoseStatus.ADMINISTERED);

        // Dose 2 and Dose 3 were cancelled with reason REPLACED
        MedicationDose reloadedDose2 = doseRepository.findById(dose2.getId()).orElseThrow();
        assertThat(reloadedDose2.getStatus()).isEqualTo(DoseStatus.CANCELLED);
        assertThat(reloadedDose2.getCancellationReason()).isEqualTo(DoseCancellationReason.REPLACED);

        MedicationDose reloadedDose3 = doseRepository.findById(dose3.getId()).orElseThrow();
        assertThat(reloadedDose3.getStatus()).isEqualTo(DoseStatus.CANCELLED);
        assertThat(reloadedDose3.getCancellationReason()).isEqualTo(DoseCancellationReason.REPLACED);

        // Old prescription is discontinued with reason REPLACED
        Prescription reloadedOld = prescriptionRepository.findById(oldId).orElseThrow();
        assertThat(reloadedOld.getStatus()).isEqualTo(PrescriptionStatus.DISCONTINUED);
    }
}
