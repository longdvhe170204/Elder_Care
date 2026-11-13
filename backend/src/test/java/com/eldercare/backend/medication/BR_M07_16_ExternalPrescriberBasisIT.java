package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyType;
import com.eldercare.backend.medication.internal.prescription.domain.OrderPrescriptionBody;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionBasisRepository;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.prescription.service.PrescriptionService;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * T030: Test BR-M07-16 & FR-013: External prescriber basis validation.
 * Nurses modifying external prescriptions must supply complete external basis (hospital, physician, date, type).
 * Nurses modifying internal prescriptions are forbidden (403).
 * Physicians do not require external basis.
 */
class BR_M07_16_ExternalPrescriberBasisIT extends M07IntegrationTestBase {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionBasisRepository prescriptionBasisRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    private Long residentId = TestUsers.nextId();
    private Long medId = 8804L;

    @BeforeEach
    void setUp() {
        jdbc.update(
                "INSERT INTO resident_module_state (resident_id, module, data, version, updated_at)"
                        + " VALUES (?, 'M07', JSON_OBJECT(), 0, UTC_TIMESTAMP(6)) "
                        + "ON DUPLICATE KEY UPDATE version = version",
                residentId);

        Medication med = new Medication(
                "Atorvastatin 20mg " + TestUsers.nextId(),
                "20mg",
                "Viên nén",
                "Oral",
                "viên",
                30,
                30,
                false,
                Set.of(104L));
        medicationRepository.save(med);
        medId = med.getId();
    }

    private Prescription createExternalPrescription() {
        Prescription p = new Prescription(
                residentId,
                medId,
                new BigDecimal("20"),
                "mg",
                "Oral",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"20:00\"]",
                null,
                null,
                null,
                null,
                null,
                Instant.now().minusSeconds(86400),
                LocalDate.now().plusDays(10),
                MedicationSource.FAMILY_SENT,
                PrescriberType.EXTERNAL,
                "Bệnh viện Bạch Mai",
                "BS. Trần Văn B",
                null,
                LocalDate.now().minusDays(5),
                1001L,
                "Uống trước khi đi ngủ",
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                null,
                Instant.now().minusSeconds(86400),
                Set.of(104L),
                Instant.now());
        return prescriptionRepository.save(p);
    }

    @Test
    @DisplayName(
            "BR-M07-16: Nurse modifying external prescription without basis is rejected (ConditionNotMetException)")
    void nurse_modifyingExternalWithoutBasis_rejected() {
        user.set(nurse.user());
        assignNurse(nurse, residentId);
        Prescription p = createExternalPrescription();

        OrderPrescriptionBody cmd = new OrderPrescriptionBody();
        cmd.setReason("Tạm dừng theo lời người nhà");
        // No external basis provided

        assertThatThrownBy(() -> prescriptionService.suspend(
                        prescriptionService.targetOf(p.getId()), p.getId(), cmd, 20L, false))
                .isInstanceOf(ConditionNotMetException.class);
    }
}
