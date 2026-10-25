package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m07.internal.domain.FrequencyType;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import com.cambae.backend.m07.internal.domain.PrescriptionType;
import com.cambae.backend.m07.internal.domain.TerminationReason;
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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * T030: Test DBR-14 & FR-016: Replacement prescription links to old prescription in a single transaction.
 * Old prescription is terminated with reason REPLACED, new prescription has replacesId set,
 * and direct editing of immutable prescription fields is blocked by database trigger.
 */
class DBR_14_ReplacementPrescriptionIT extends MySqlIntegrationTestBase {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private Long residentId = 8801L;
    private Long medId = 8801L;

    @BeforeEach
    void setUp() {
        jdbc.update(
                "INSERT INTO resident_medication (resident_id, version) VALUES (?, 0) "
                        + "ON DUPLICATE KEY UPDATE version = version",
                residentId);

        Medication med =
                new Medication("Amlodipine 5mg", "5mg", "Viên nén", "Oral", "viên", 30, 30, false, Set.of(101L));
        medicationRepository.save(med);
        medId = med.getId();
    }

    @Test
    @DisplayName("DBR-14: Changing dosage creates replacement prescription and marks old prescription REPLACED")
    void changeDosage_createsReplacementLink() {
        // Create initial prescription (5 mg)
        CreatePrescriptionBody initialBody = new CreatePrescriptionBody();
        initialBody.setMedicationId(medId);
        initialBody.setDosage(new BigDecimal("5.00"));
        initialBody.setDosageUnit("viên");
        initialBody.setRoute("Oral");
        initialBody.setPrescriptionType("PERIODIC");

        FrequencyBody freq = new FrequencyBody();
        freq.setType("TIMED_SLOTS");
        freq.setTimedSlots(List.of("08:00"));
        initialBody.setFrequency(freq);

        initialBody.setStartedAt(Instant.now().minusSeconds(3600));
        initialBody.setMedicationSource("FACILITY");

        PrescriberBody prescriber = new PrescriberBody();
        prescriber.setType("INTERNAL");
        prescriber.setPrescriberId(10L);
        initialBody.setPrescriber(prescriber);

        PrescriptionResponse initial = prescriptionService.create(residentId, initialBody, 10L, true);
        Long oldId = initial.id();

        // Doctor changes dosage to 10 mg effective immediately
        CreatePrescriptionBody newBody = new CreatePrescriptionBody();
        newBody.setMedicationId(medId);
        newBody.setDosage(new BigDecimal("10.00"));
        newBody.setDosageUnit("viên");
        newBody.setRoute("Oral");
        newBody.setPrescriptionType("PERIODIC");
        newBody.setFrequency(freq);
        newBody.setStartedAt(Instant.now());
        newBody.setMedicationSource("FACILITY");
        newBody.setPrescriber(prescriber);

        ChangeDosageBody changeCmd = new ChangeDosageBody();
        changeCmd.setNewPrescription(newBody);
        changeCmd.setEffectiveFrom(Instant.now());
        changeCmd.setReason("Tăng liều điều trị huyết áp");

        PrescriptionResponse replacement = prescriptionService.changeDosage(oldId, changeCmd, 10L, true);

        // Verify replacement prescription
        assertThat(replacement.id()).isNotNull().isNotEqualTo(oldId);
        assertThat(replacement.replacesId()).isEqualTo(oldId);
        assertThat(replacement.status()).isEqualTo("ACTIVE");

        // Verify old prescription is DISCONTINUED with reason REPLACED
        Prescription oldPrescription = prescriptionRepository.findById(oldId).orElseThrow();
        assertThat(oldPrescription.getStatus()).isEqualTo(PrescriptionStatus.DISCONTINUED);
        assertThat(oldPrescription.getTerminationReason()).isEqualTo(TerminationReason.REPLACED);

        // Verify replacement can be queried by replacesId
        Prescription foundReplacement =
                prescriptionRepository.findByReplacesId(oldId).orElseThrow();
        assertThat(foundReplacement.getId()).isEqualTo(replacement.id());
    }

    @Test
    @DisplayName("DBR-14, FR-014: Database trigger blocks modifying immutable prescription fields")
    void trigger_blocksModifyingPrescriptionDirectly() {
        Prescription p = new Prescription(
                residentId,
                medId,
                new BigDecimal("5.00"),
                "viên",
                "Oral",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\"]",
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                null,
                com.cambae.backend.m07.api.dto.MedicationSource.FACILITY,
                com.cambae.backend.m07.internal.domain.PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                "Uống buổi sáng",
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                10L,
                Instant.now(),
                Set.of(101L));

        Prescription saved = prescriptionRepository.save(p);

        // Direct SQL update to dosage should be blocked by trigger trg_prescription_lock
        assertThatThrownBy(() -> jdbc.update("UPDATE prescription SET dosage = 10.00 WHERE id = ?", saved.getId()))
                .isInstanceOf(DataAccessException.class);
    }
}
