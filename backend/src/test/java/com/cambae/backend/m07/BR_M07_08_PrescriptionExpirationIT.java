package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.ConditionNotMetException;
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
import com.cambae.backend.m07.internal.service.M07JobService;
import com.cambae.backend.m07.internal.service.PrescriptionService;
import com.cambae.backend.m07.internal.service.request.ExtendPrescriptionBody;
import com.cambae.backend.support.TestUsers;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * T030: Test BR-M07-08 & FR-015, FR-017: Prescription expiration and extension.
 * Both ACTIVE and SUSPENDED prescriptions expire when end date passes.
 * Expiration warning is issued CFG-M07-04 (3 days) before end date.
 * Extending requires new end date strictly after current end date.
 */
class BR_M07_08_PrescriptionExpirationIT extends M07IntegrationTestBase {

    @Autowired
    private M07JobService jobService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private MedicationDoseRepository doseRepository;

    private Long residentId = TestUsers.nextId();
    private Long medId = 8805L;

    @BeforeEach
    void setUp() {
        jdbc.update(
                "INSERT INTO resident_medication (resident_id, version) VALUES (?, 0) "
                        + "ON DUPLICATE KEY UPDATE version = version",
                residentId);

        Medication med = new Medication(
                "Omeprazole 20mg " + TestUsers.nextId(),
                "20mg",
                "Viên nang",
                "Oral",
                "viên",
                30,
                30,
                false,
                Set.of(105L));
        medicationRepository.save(med);
        medId = med.getId();
    }

    @Test
    @DisplayName("BR-M07-08: Both ACTIVE and SUSPENDED prescriptions expire and cancel open doses")
    void dailyJob_expiresActiveAndSuspendedPrescriptions() throws SQLException {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        // Việc hằng ngày chạy một lần mỗi ngày (research D-19): bỏ dấu đã chạy của lần chạy test trước cùng ngày
        try (Connection c = adminConnection();
                Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM m07_job_run WHERE job_key = 'DAILY_CHECK' AND checkpoint = '" + today + "'");
        }

        // 1. ACTIVE prescription ended yesterday
        Prescription activePresc = new Prescription(
                residentId,
                medId,
                new BigDecimal("20"),
                "mg",
                "Oral",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"07:00\"]",
                null,
                null,
                null,
                null,
                null,
                now.minus(10, ChronoUnit.DAYS),
                today.minusDays(1),
                MedicationSource.FACILITY,
                com.cambae.backend.m07.internal.domain.PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                "Trước ăn sáng",
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                10L,
                now.minus(10, ChronoUnit.DAYS),
                Set.of(105L),
                Instant.now());
        Prescription savedActive = prescriptionRepository.save(activePresc);

        // Open dose for active prescription
        MedicationDose dose = new MedicationDose(
                savedActive.getId(),
                residentId,
                medId,
                now.plus(1, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.DAYS).minus(30, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
                new BigDecimal("20"),
                "mg",
                MedicationSource.FACILITY,
                false,
                DoseStatus.UPCOMING,
                1L,
                Instant.now());
        doseRepository.save(dose);

        // 2. SUSPENDED prescription ended yesterday (BR-M07-08 scenario 9)
        Prescription suspendedPresc = new Prescription(
                residentId,
                medId,
                new BigDecimal("20"),
                "mg",
                "Oral",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"07:00\"]",
                null,
                null,
                null,
                null,
                null,
                now.minus(10, ChronoUnit.DAYS),
                today.minusDays(1),
                MedicationSource.FACILITY,
                com.cambae.backend.m07.internal.domain.PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                "Trước ăn sáng",
                PrescriptionStatus.SUSPENDED,
                false,
                null,
                null,
                10L,
                now.minus(10, ChronoUnit.DAYS),
                Set.of(105L),
                Instant.now());
        Prescription savedSuspended = prescriptionRepository.save(suspendedPresc);

        // Run daily task for today
        jobService.runDailyTasks(today, now);

        // Both prescriptions should be EXPIRED
        Prescription reloadedActive =
                prescriptionRepository.findById(savedActive.getId()).orElseThrow();
        assertThat(reloadedActive.getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);

        Prescription reloadedSuspended =
                prescriptionRepository.findById(savedSuspended.getId()).orElseThrow();
        assertThat(reloadedSuspended.getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);

        // Open dose is cancelled with reason EXPIRED
        MedicationDose reloadedDose = doseRepository.findById(dose.getId()).orElseThrow();
        assertThat(reloadedDose.getStatus()).isEqualTo(DoseStatus.CANCELLED);
        assertThat(reloadedDose.getCancellationReason()).isEqualTo(DoseCancellationReason.EXPIRED);
    }

    @Test
    @DisplayName("FR-015: Extending prescription requires new end date strictly after current end date")
    void extendPrescription_validation() {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();

        Prescription p = new Prescription(
                residentId,
                medId,
                new BigDecimal("20"),
                "mg",
                "Oral",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"07:00\"]",
                null,
                null,
                null,
                null,
                null,
                now.minus(5, ChronoUnit.DAYS),
                today.plusDays(5),
                MedicationSource.FACILITY,
                com.cambae.backend.m07.internal.domain.PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                "Trước ăn sáng",
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                10L,
                now.minus(5, ChronoUnit.DAYS),
                Set.of(105L),
                Instant.now());
        Prescription saved = prescriptionRepository.save(p);

        // 1. Extending with earlier or same end date should fail
        ExtendPrescriptionBody invalidCmd = new ExtendPrescriptionBody();
        invalidCmd.setNewEndedAt(today.plusDays(3)); // earlier than +5
        invalidCmd.setReason("Gia hạn nhưng chọn ngày sớm hơn");

        assertThatThrownBy(() -> prescriptionService.extend(
                        prescriptionService.targetOf(saved.getId()), saved.getId(), invalidCmd, 10L, true))
                .isInstanceOf(ConditionNotMetException.class);

        // 2. Extending with strictly later date succeeds
        ExtendPrescriptionBody validCmd = new ExtendPrescriptionBody();
        validCmd.setNewEndedAt(today.plusDays(15));
        validCmd.setReason("Gia hạn thêm 10 ngày");

        prescriptionService.extend(prescriptionService.targetOf(saved.getId()), saved.getId(), validCmd, 10L, true);

        Prescription reloaded = prescriptionRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getEndedAt()).isEqualTo(today.plusDays(15));
    }
}
