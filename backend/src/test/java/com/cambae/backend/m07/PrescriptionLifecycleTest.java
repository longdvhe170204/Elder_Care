package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.internal.domain.DoseCancellationReason;
import com.cambae.backend.m07.internal.domain.DoseStatus;
import com.cambae.backend.m07.internal.domain.FrequencyType;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.MedicationDose;
import com.cambae.backend.m07.internal.domain.PrescriberType;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionBasis;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import com.cambae.backend.m07.internal.domain.PrescriptionType;
import com.cambae.backend.m07.internal.domain.TerminationReason;
import com.cambae.backend.m07.internal.persistence.MedicationDoseRepository;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionBasisRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionIssueRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import com.cambae.backend.m07.internal.persistence.ResidentMedicationLock;
import com.cambae.backend.m07.internal.service.HealthAllergyPort;
import com.cambae.backend.m07.internal.service.PrescriptionCheckResult;
import com.cambae.backend.m07.internal.service.PrescriptionService;
import com.cambae.backend.m07.internal.service.PrescriptionValidator;
import com.cambae.backend.m07.internal.service.ReconciliationCheckPort;
import com.cambae.backend.m07.web.request.ChangeDosageBody;
import com.cambae.backend.m07.web.request.CreatePrescriptionBody;
import com.cambae.backend.m07.web.request.DiscontinuePrescriptionBody;
import com.cambae.backend.m07.web.request.ExtendPrescriptionBody;
import com.cambae.backend.m07.web.request.ExternalBasisBody;
import com.cambae.backend.m07.web.request.FrequencyBody;
import com.cambae.backend.m07.web.request.OrderPrescriptionBody;
import com.cambae.backend.m07.web.request.PrescriberBody;
import com.cambae.backend.m07.web.response.PrescriptionResponse;
import java.lang.reflect.Field;
import java.math.BigDecimal;
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
                reconciliationCheckPort);

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
                Set.of(101L));
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

        MedicationDose openDose = new MedicationDose(
                1001L,
                residentId,
                medId,
                Instant.parse("2026-10-24T15:00:00Z"),
                Instant.parse("2026-10-24T14:30:00Z"),
                Instant.parse("2026-10-24T15:30:00Z"),
                new BigDecimal("5.0"),
                "mg",
                MedicationSource.FACILITY,
                false,
                DoseStatus.UPCOMING,
                1L);
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

        PrescriptionResponse resp = prescriptionService.changeDosage(1001L, body, 1L, true);

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
        oldPrescription.setSuspendedFrom(Instant.parse("2026-10-25T08:00:00Z"));
        when(prescriptionRepository.findById(1001L)).thenReturn(Optional.of(oldPrescription));

        Prescription replacementPrescription =
                createSamplePrescription(1002L, PrescriptionStatus.PENDING_EFFECTIVE, PrescriberType.INTERNAL);
        replacementPrescription.setReplacesId(1001L);
        when(prescriptionRepository.findById(1002L)).thenReturn(Optional.of(replacementPrescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        DiscontinuePrescriptionBody body = new DiscontinuePrescriptionBody();
        body.setDiscontinueAt(Instant.now());

        PrescriptionResponse resp = prescriptionService.discontinue(1002L, body, 1L, true);

        assertThat(resp.trangThai()).isEqualTo(PrescriptionStatus.DISCONTINUED.name());
        assertThat(oldPrescription.getSuspendedFrom()).isNull();
    }

    @Test
    @DisplayName("BR-M07-16: Điều dưỡng sửa đơn bên ngoài không có căn cứ cơ sở kê thì bị chặn THIEU_CAN_CU_CO_SO_KE")
    void nurse_modifyingExternalWithoutBasis_rejected() {
        Prescription external = createSamplePrescription(1003L, PrescriptionStatus.ACTIVE, PrescriberType.EXTERNAL);
        when(prescriptionRepository.findById(1003L)).thenReturn(Optional.of(external));

        OrderPrescriptionBody body = new OrderPrescriptionBody();
        body.setExternalBasis(null); // Thiếu căn cứ

        assertThatThrownBy(() -> prescriptionService.suspend(1003L, body, 2L, false))
                .isInstanceOf(ConditionNotMetException.class)
                .satisfies(ex -> {
                    ConditionNotMetException cnme = (ConditionNotMetException) ex;
                    assertThat(cnme.unmetConditions()).anyMatch(c -> c.code().equals("CAN_CU_CO_SO_KE"));
                });
    }

    @Test
    @DisplayName("BR-M07-16: Điều dưỡng sửa đơn bên ngoài có căn cứ hợp lệ thì lưu căn cứ thành công")
    void nurse_modifyingExternalWithValidBasis_success() {
        Prescription external = createSamplePrescription(1003L, PrescriptionStatus.ACTIVE, PrescriberType.EXTERNAL);
        when(prescriptionRepository.findById(1003L)).thenReturn(Optional.of(external));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPrescriptionBody body = new OrderPrescriptionBody();
        ExternalBasisBody basis = new ExternalBasisBody();
        basis.setFacilityName("Bệnh viện Đa khoa");
        basis.setPrescriberName("Bác sĩ Trần");
        basis.setPrescribedDate(LocalDate.of(2026, 10, 24));
        basis.setBasisType("DON_MOI");
        basis.setDocumentFileId(999L);
        body.setExternalBasis(basis);

        PrescriptionResponse resp = prescriptionService.suspend(1003L, body, 2L, false);

        assertThat(resp.trangThai()).isEqualTo(PrescriptionStatus.SUSPENDED.name());
        verify(prescriptionBasisRepository).save(any(PrescriptionBasis.class));
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

        PrescriptionResponse resp = prescriptionService.extend(1004L, body, 1L, true);

        assertThat(resp.ngayKetThuc()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("FR-015: Gia hạn với ngày mới sớm hơn ngày cũ bị chặn")
    void extend_earlierDate_rejected() {
        Prescription p = createSamplePrescription(1004L, PrescriptionStatus.ACTIVE, PrescriberType.INTERNAL);
        when(prescriptionRepository.findById(1004L)).thenReturn(Optional.of(p));

        ExtendPrescriptionBody body = new ExtendPrescriptionBody();
        body.setNewEndedAt(LocalDate.of(2026, 11, 10)); // Ngày cũ là 2026-11-20
        body.setReason("Rút ngắn");

        assertThatThrownBy(() -> prescriptionService.extend(1004L, body, 1L, true))
                .isInstanceOf(ConditionNotMetException.class)
                .satisfies(ex -> {
                    ConditionNotMetException cnme = (ConditionNotMetException) ex;
                    assertThat(cnme.unmetConditions()).anyMatch(c -> c.code().equals("NGAY_KET_THUC_KHONG_HOP_LE"));
                });
    }
}
