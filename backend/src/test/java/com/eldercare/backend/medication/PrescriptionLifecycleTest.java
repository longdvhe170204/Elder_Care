package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseCancellationReason;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.prescription.domain.ChangeDosageBody;
import com.eldercare.backend.medication.internal.prescription.domain.CreatePrescriptionBody;
import com.eldercare.backend.medication.internal.prescription.domain.DiscontinuePrescriptionBody;
import com.eldercare.backend.medication.internal.prescription.domain.ExtendPrescriptionBody;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyBody;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyType;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberBody;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionCheckResult;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionResponse;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.domain.TerminationReason;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionBasisRepository;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionIssueRepository;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.prescription.service.HealthAllergyPort;
import com.eldercare.backend.medication.internal.prescription.service.PrescriptionService;
import com.eldercare.backend.medication.internal.prescription.service.PrescriptionValidator;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationCheckPort;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T030: Test đơn vị cho User Story 3 (Phase 5):
 * Đổi liều bằng đơn thay thế, ngừng, gia hạn, huỷ đổi liều, căn cứ cơ sở kê.
 * (FR-014 -> FR-017, DBR-14, BR-M07-07, BR-M07-08, BR-M07-15, BR-M07-16)
 */
class PrescriptionLifecycleTest {

    private PrescriptionRepository prescriptionRepository;
    private PrescriptionIssueRepository prescriptionIssueRepository;
    private PrescriptionBasisRepository prescriptionBasisRepository;
    private MedicationRepository medicationRepository;
    private MedicationDoseRepository doseRepository;
    private ResidentMedicationLock residentMedicationLock;
    private PrescriptionValidator prescriptionValidator;
    private HealthAllergyPort healthAllergyPort;
    private ReconciliationCheckPort reconciliationCheckPort;
    private PrescriptionService prescriptionService;

    private Long residentId = 8801L;
    private Long medId = 501L;

    @BeforeEach
    void setUp() {
        prescriptionRepository = mock(PrescriptionRepository.class);
        prescriptionIssueRepository = mock(PrescriptionIssueRepository.class);
        prescriptionBasisRepository = mock(PrescriptionBasisRepository.class);
        medicationRepository = mock(MedicationRepository.class);
        doseRepository = mock(MedicationDoseRepository.class);
        residentMedicationLock = mock(ResidentMedicationLock.class);
        prescriptionValidator = mock(PrescriptionValidator.class);
        healthAllergyPort = mock(HealthAllergyPort.class);
        reconciliationCheckPort = mock(ReconciliationCheckPort.class);

        prescriptionService = new PrescriptionService(
                prescriptionRepository,
                prescriptionIssueRepository,
                prescriptionBasisRepository,
                medicationRepository,
                doseRepository,
                residentMedicationLock,
                prescriptionValidator,
                healthAllergyPort,
                reconciliationCheckPort,
                new BusinessTime(Clock.systemUTC()),
                M07TestParameters.defaults());

        when(prescriptionValidator.validate(any(), any(), any(), any()))
                .thenReturn(new PrescriptionCheckResult(List.of(), List.of()));

        Medication med =
                new Medication("Amlodipine 5mg", "5mg", "Viên nén", "Oral", "viên", 30, 30, false, Set.of(101L));
        setId(med, medId);
        when(medicationRepository.findById(medId)).thenReturn(Optional.of(med));
    }

    private void setId(Object obj, Long id) {
        try {
            Field idField = obj.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(obj, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Prescription createSamplePrescription(Long id, PrescriptionStatus status, PrescriberType prescriberType) {
        Prescription p = new Prescription(
                residentId,
                medId,
                new BigDecimal("5.0"),
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
                Instant.parse("2026-10-20T00:00:00Z"),
                LocalDate.of(2026, 11, 20),
                MedicationSource.FACILITY,
                prescriberType,
                prescriberType == PrescriberType.EXTERNAL ? "Bệnh viện Đa khoa" : null,
                prescriberType == PrescriberType.EXTERNAL ? "Bác sĩ Nguyễn" : null,
                prescriberType == PrescriberType.INTERNAL ? 101L : null,
                prescriberType == PrescriberType.EXTERNAL ? LocalDate.of(2026, 10, 19) : null,
                null,
                "Uống sau ăn",
                status,
                false,
                null,
                null,
                1L,
                Instant.parse("2026-10-20T00:00:00Z"),
                Set.of(101L),
                Instant.now());
        setId(p, id);
        return p;
    }

    @Test
    @DisplayName("DBR-14 & BR-M07-07: Đổi liều ngay tạo đơn thay thế mới và đơn cũ chuyển REPLACED")
    void changeDosage_immediate_success() {
        Prescription oldPrescription =
                createSamplePrescription(1001L, PrescriptionStatus.ACTIVE, PrescriberType.INTERNAL);
        when(prescriptionRepository.findById(1001L)).thenReturn(Optional.of(oldPrescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant doseTime = Instant.now().plusSeconds(3600);
        MedicationDose openDose = new MedicationDose(
                1001L,
                residentId,
                medId,
                doseTime,
                doseTime.minusSeconds(1800),
                doseTime.plusSeconds(1800),
                new BigDecimal("5.0"),
                "mg",
                MedicationSource.FACILITY,
                false,
                DoseStatus.UPCOMING,
                1L,
                Instant.now());
        when(doseRepository.findByPrescriptionIdAndStatusIn(eq(1001L), anyList()))
                .thenReturn(List.of(openDose));

        ChangeDosageBody body = new ChangeDosageBody();
        body.setEffectiveFrom(Instant.now());
        CreatePrescriptionBody newPresc = new CreatePrescriptionBody();
        newPresc.setMedicationId(medId);
        newPresc.setDosage(new BigDecimal("10.0"));
        newPresc.setDosageUnit("mg");
        newPresc.setRoute("Oral");
        newPresc.setPrescriptionType("PERIODIC");
        FrequencyBody freq = new FrequencyBody();
        freq.setType("TIMED_SLOTS");
        freq.setTimedSlots(List.of("08:00"));
        newPresc.setFrequency(freq);
        newPresc.setMedicationSource("FACILITY");
        PrescriberBody prescriber = new PrescriberBody();
        prescriber.setType("INTERNAL");
        prescriber.setPrescriberId(101L);
        newPresc.setPrescriber(prescriber);
        newPresc.setStartedAt(Instant.now());
        body.setNewPrescription(newPresc);

        PrescriptionResponse resp = prescriptionService.changeDosage(Target.UNSPECIFIED, 1001L, body, 1L, true);

        assertThat(resp).isNotNull();
        assertThat(oldPrescription.getStatus()).isEqualTo(PrescriptionStatus.DISCONTINUED);
        assertThat(oldPrescription.getTerminationReason()).isEqualTo(TerminationReason.REPLACED);
        assertThat(openDose.getStatus()).isEqualTo(DoseStatus.CANCELLED);
        assertThat(openDose.getCancellationReason()).isEqualTo(DoseCancellationReason.REPLACED);
    }

    @Test
    @DisplayName("BR-M07-15: Ngừng đơn thay thế còn PENDING_EFFECTIVE thì xóa suspendedFrom ở đơn cũ")
    void cancelDosageChange_success() {
        Prescription oldPrescription =
                createSamplePrescription(1001L, PrescriptionStatus.ACTIVE, PrescriberType.INTERNAL);
        oldPrescription.scheduleSuspensionFrom(Instant.parse("2026-10-25T08:00:00Z"));
        when(prescriptionRepository.findById(1001L)).thenReturn(Optional.of(oldPrescription));

        Prescription replacementPrescription =
                createSamplePrescription(1002L, PrescriptionStatus.PENDING_EFFECTIVE, PrescriberType.INTERNAL);
        replacementPrescription.markReplaces(1001L);
        when(prescriptionRepository.findById(1002L)).thenReturn(Optional.of(replacementPrescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        DiscontinuePrescriptionBody body = new DiscontinuePrescriptionBody();
        body.setDiscontinueAt(Instant.now());

        PrescriptionResponse resp = prescriptionService.discontinue(Target.UNSPECIFIED, 1002L, body, 1L, true);

        assertThat(resp.status()).isEqualTo(PrescriptionStatus.DISCONTINUED.name());
        assertThat(oldPrescription.getSuspendedFrom()).isNull();
    }

    @Test
    @DisplayName("FR-015: Gia hạn đơn thuốc cập nhật ngày kết thúc mới")
    void extend_success() {
        Prescription p = createSamplePrescription(1004L, PrescriptionStatus.ACTIVE, PrescriberType.INTERNAL);
        when(prescriptionRepository.findById(1004L)).thenReturn(Optional.of(p));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtendPrescriptionBody body = new ExtendPrescriptionBody();
        body.setNewEndedAt(LocalDate.of(2026, 12, 31));
        body.setReason("Tiếp tục điều trị theo chỉ định");

        PrescriptionResponse resp = prescriptionService.extend(Target.UNSPECIFIED, 1004L, body, 1L, true);

        assertThat(resp.endedAt()).isEqualTo(LocalDate.of(2026, 12, 31));
    }
}
