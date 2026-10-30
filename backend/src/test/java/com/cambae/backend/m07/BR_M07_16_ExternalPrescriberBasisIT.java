package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.internal.domain.FrequencyType;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.PrescriberType;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionBasis;
import com.cambae.backend.m07.internal.domain.PrescriptionBasisCommand;
import com.cambae.backend.m07.internal.domain.PrescriptionBasisType;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import com.cambae.backend.m07.internal.domain.PrescriptionType;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionBasisRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import com.cambae.backend.m07.internal.service.PrescriptionService;
import com.cambae.backend.m07.internal.service.request.DiscontinuePrescriptionBody;
import com.cambae.backend.m07.internal.service.request.ExternalBasisBody;
import com.cambae.backend.m07.internal.service.request.OrderPrescriptionBody;
import com.cambae.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
                "INSERT INTO resident_medication (resident_id, version) VALUES (?, 0) "
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

    private Prescription createInternalPrescription() {
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
                null,
                MedicationSource.FACILITY,
                PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                "Uống buổi tối",
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                10L,
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

    @Test
    @DisplayName(
            "BR-M07-16: Nurse modifying external prescription with valid document basis succeeds and records basis")
    void nurse_modifyingExternalWithValidBasis_succeeds() {
        user.set(nurse.user());
        assignNurse(nurse, residentId);
        Prescription p = createExternalPrescription();

        OrderPrescriptionBody cmd = new OrderPrescriptionBody();
        cmd.setReason("Tạm dừng theo chỉ định bệnh viện tái khám");

        ExternalBasisBody basis = new ExternalBasisBody();
        basis.setFacilityName("Bệnh viện Bạch Mai");
        basis.setPrescriberName("BS. Trần Văn B");
        basis.setPrescribedDate(LocalDate.now());
        basis.setBasisType("DISCHARGE_PAPER");
        basis.setDocumentFileId(5001L);
        cmd.setExternalBasis(basis);

        prescriptionService.suspend(prescriptionService.targetOf(p.getId()), p.getId(), cmd, 20L, false);

        // Verify status
        Prescription reloaded = prescriptionRepository.findById(p.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PrescriptionStatus.SUSPENDED);

        // Verify prescription_basis record saved
        List<PrescriptionBasis> basisList = prescriptionBasisRepository.findByPrescriptionId(p.getId());
        assertThat(basisList).isNotEmpty();
        PrescriptionBasis savedBasis = basisList.get(0);
        assertThat(savedBasis.getFacilityName()).isEqualTo("Bệnh viện Bạch Mai");
        assertThat(savedBasis.getCommand()).isEqualTo(PrescriptionBasisCommand.SUSPEND);
        assertThat(savedBasis.getBasisType()).isEqualTo(PrescriptionBasisType.DISCHARGE_PAPER);
    }

    @Test
    @DisplayName("BR-M07-16: Nurse modifying internal prescription is forbidden (403)")
    void nurse_modifyingInternalPrescription_forbidden() {
        user.set(nurse.user());
        assignNurse(nurse, residentId);
        Prescription p = createInternalPrescription();

        OrderPrescriptionBody cmd = new OrderPrescriptionBody();
        cmd.setReason("Thử tạm dừng đơn nội bộ");

        assertThatThrownBy(() -> prescriptionService.suspend(
                        prescriptionService.targetOf(p.getId()), p.getId(), cmd, 20L, false))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("BR-M07-16: Doctor modifying external prescription succeeds without external basis")
    void doctor_modifyingExternalWithoutBasis_succeeds() {
        Prescription p = createExternalPrescription();

        DiscontinuePrescriptionBody cmd = new DiscontinuePrescriptionBody();
        cmd.setReason("Bác sĩ cơ sở quyết định ngừng đơn bên ngoài");

        prescriptionService.discontinue(prescriptionService.targetOf(p.getId()), p.getId(), cmd, 10L, true);

        Prescription reloaded = prescriptionRepository.findById(p.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PrescriptionStatus.DISCONTINUED);
    }
}
