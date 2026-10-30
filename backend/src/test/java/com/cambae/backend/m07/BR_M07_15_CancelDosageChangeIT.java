package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.internal.domain.FrequencyType;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import com.cambae.backend.m07.internal.domain.PrescriptionType;
import com.cambae.backend.m07.internal.domain.TerminationReason;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import com.cambae.backend.m07.internal.service.PrescriptionService;
import com.cambae.backend.m07.internal.service.request.ChangeDosageBody;
import com.cambae.backend.m07.internal.service.request.CreatePrescriptionBody;
import com.cambae.backend.m07.internal.service.request.DiscontinuePrescriptionBody;
import com.cambae.backend.m07.internal.service.request.FrequencyBody;
import com.cambae.backend.m07.internal.service.request.PrescriberBody;
import com.cambae.backend.m07.internal.service.response.PrescriptionResponse;
import com.cambae.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * T030: Test BR-M07-15 & FR-016: Discontinuing a pending replacement prescription
 * cancels the planned dosage change: the replacement prescription becomes DISCONTINUED,
 * and the original prescription has its scheduled discontinuation cleared and remains ACTIVE.
 */
class BR_M07_15_CancelDosageChangeIT extends M07IntegrationTestBase {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    private Long residentId = TestUsers.nextId();
    private Long medId = 8803L;

    @BeforeEach
    void setUp() {
        jdbc.update(
                "INSERT INTO resident_medication (resident_id, version) VALUES (?, 0) "
                        + "ON DUPLICATE KEY UPDATE version = version",
                residentId);

        Medication med = new Medication(
                "Losartan 50mg " + TestUsers.nextId(), "50mg", "Viên nén", "Oral", "viên", 30, 30, false, Set.of(103L));
        medicationRepository.save(med);
        medId = med.getId();
    }

    @Test
    @DisplayName(
            "BR-M07-15: Discontinuing pending replacement prescription restores original prescription to unscheduled discontinuation")
    void cancelPlannedDosageChange_restoresOriginalPrescription() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant futureEffective = now.plus(2, ChronoUnit.DAYS);

        // 1. Initial prescription
        Prescription p = new Prescription(
                residentId,
                medId,
                new BigDecimal("50"),
                "mg",
                "Oral",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\"]",
                null,
                null,
                null,
                null,
                null,
                now.minus(5, ChronoUnit.DAYS),
                null,
                MedicationSource.FACILITY,
                com.cambae.backend.m07.internal.domain.PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                "Buổi sáng",
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                10L,
                now.minus(5, ChronoUnit.DAYS),
                Set.of(103L),
                Instant.now());
        Prescription savedOriginal = prescriptionRepository.save(p);
        Long originalId = savedOriginal.getId();

        // 2. Plan a future dosage change
        CreatePrescriptionBody newBody = new CreatePrescriptionBody();
        newBody.setMedicationId(medId);
        newBody.setDosage(new BigDecimal("100"));
        newBody.setDosageUnit("mg");
        newBody.setRoute("Oral");
        newBody.setPrescriptionType("PERIODIC");

        FrequencyBody freq = new FrequencyBody();
        freq.setType("TIMED_SLOTS");
        freq.setTimedSlots(List.of("08:00"));
        newBody.setFrequency(freq);
        newBody.setStartedAt(futureEffective);
        newBody.setMedicationSource("FACILITY");

        PrescriberBody prescriber = new PrescriberBody();
        prescriber.setType("INTERNAL");
        prescriber.setPrescriberId(10L);
        newBody.setPrescriber(prescriber);

        ChangeDosageBody changeCmd = new ChangeDosageBody();
        changeCmd.setNewPrescription(newBody);
        changeCmd.setEffectiveFrom(futureEffective);
        changeCmd.setReason("Dự kiến đổi sang 100mg sau 2 ngày");

        PrescriptionResponse replacement = prescriptionService.changeDosage(
                prescriptionService.targetOf(originalId), originalId, changeCmd, 10L, true);
        Long replacementId = replacement.id();

        // Check intermediate state:
        // Original prescription is still ACTIVE but scheduled to discontinue at futureEffective
        Prescription intermediateOriginal =
                prescriptionRepository.findById(originalId).orElseThrow();
        assertThat(intermediateOriginal.getStatus()).isEqualTo(PrescriptionStatus.ACTIVE);
        assertThat(intermediateOriginal.getSuspendedFrom()).isEqualTo(futureEffective);

        // Replacement prescription is PENDING_EFFECTIVE
        Prescription intermediateReplacement =
                prescriptionRepository.findById(replacementId).orElseThrow();
        assertThat(intermediateReplacement.getStatus()).isEqualTo(PrescriptionStatus.PENDING_EFFECTIVE);
        assertThat(intermediateReplacement.getReplacesId()).isEqualTo(originalId);

        // 3. Doctor decides to cancel the change by discontinuing the pending replacement prescription
        DiscontinuePrescriptionBody discontinueCmd = new DiscontinuePrescriptionBody();
        discontinueCmd.setReason("Hủy thay đổi liều, giữ nguyên liều cũ");

        prescriptionService.discontinue(
                prescriptionService.targetOf(replacementId), replacementId, discontinueCmd, 10L, true);

        // 4. Assertions:
        // Replacement prescription is DISCONTINUED
        Prescription finalReplacement =
                prescriptionRepository.findById(replacementId).orElseThrow();
        assertThat(finalReplacement.getStatus()).isEqualTo(PrescriptionStatus.DISCONTINUED);
        assertThat(finalReplacement.getTerminationReason()).isEqualTo(TerminationReason.DISCONTINUED);

        // Original prescription has its suspendedFrom cleared to null and remains ACTIVE
        Prescription finalOriginal = prescriptionRepository.findById(originalId).orElseThrow();
        assertThat(finalOriginal.getStatus()).isEqualTo(PrescriptionStatus.ACTIVE);
        assertThat(finalOriginal.getSuspendedFrom()).isNull();
    }
}
