package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.dose.domain.PrnAdministration;
import com.eldercare.backend.medication.internal.dose.domain.RecordPrnBody;
import com.eldercare.backend.medication.internal.dose.persistence.PrnAdministrationRepository;
import com.eldercare.backend.medication.internal.dose.service.PrnMedicationService;
import com.eldercare.backend.medication.internal.family.service.FamilyMedicationService;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ResidentMedicationLock;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

/**
 * T044: Test đơn vị cho User Story 6 (Phase 8):
 * Thuốc khi cần (PRN) có giới hạn khoảng cách và số lần trong 24 giờ trượt.
 * (FR-024, FR-025, FR-025a, BR-M07-04)
 */
class BR_M07_04_PrnLimitTest {

    private PrnAdministrationRepository prnRepo;
    private PrescriptionRepository prescriptionRepo;
    private ResidentMedicationLock residentMedicationLock;

    private PrnMedicationService prnService;

    @BeforeEach
    void setUp() {
        prnRepo = mock(PrnAdministrationRepository.class);
        prescriptionRepo = mock(PrescriptionRepository.class);
        residentMedicationLock = mock(ResidentMedicationLock.class);

        prnService = new PrnMedicationService(
                prnRepo,
                prescriptionRepo,
                residentMedicationLock,
                new BusinessTime(Clock.systemUTC()),
                mock(FamilyMedicationService.class),
                mock(ApplicationEventPublisher.class),
                mock(ResidentLocationApi.class));
    }

    private void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Prescription createPrnPrescription(
            Long id, Long residentId, Integer minIntervalMinutes, Integer maxDosesPer24h) {
        Prescription p = new Prescription(
                residentId,
                50L,
                new BigDecimal("500.0"), // 500mg
                "mg",
                "Oral",
                PrescriptionType.PRN,
                null,
                null,
                null,
                null,
                null,
                minIntervalMinutes, // ví dụ: 240 phút (4 giờ)
                maxDosesPer24h, // ví dụ: 4 lần/24h
                Instant.parse("2026-10-20T00:00:00Z"),
                LocalDate.of(2026, 11, 20),
                MedicationSource.FACILITY,
                PrescriberType.INTERNAL,
                null,
                null,
                101L,
                null,
                null,
                "Uống khi đau đầu hoặc sốt >= 38.5 độ C",
                PrescriptionStatus.ACTIVE,
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

    private PrnAdministration createAdminRecord(Long id, Long prescriptionId, Instant time, BigDecimal dosage) {
        PrnAdministration a = new PrnAdministration(
                prescriptionId,
                100L,
                time,
                dosage,
                "mg",
                "Sốt cao 38.8 độ C",
                1L,
                null,
                false,
                null,
                null,
                null,
                false,
                Instant.now());
        setId(a, id);
        return a;
    }

    @Test
    @DisplayName("T044: Ghi PRN thiếu lý do -> 422 INVALID_VALUE (FR-024)")
    void testRecordPrn_MissingReason_Throws422() {
        Prescription p = createPrnPrescription(301L, 100L, 240, 4);
        when(prescriptionRepo.findById(301L)).thenReturn(Optional.of(p));

        RecordPrnBody body = new RecordPrnBody();
        body.setDosage(new BigDecimal("500.0"));
        body.setReason(""); // Rỗng

        assertThatThrownBy(() -> prnService.recordPrn(301L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.errorCode()).isEqualTo("INVALID_VALUE");
                });
    }

    @Test
    @DisplayName("T044: Ghi PRN vượt quá liều tối đa một lần trong đơn -> 422 VUOT_LIEU_DON (FR-024)")
    void testRecordPrn_DosageExceedsSingleDoseLimit_Throws422() {
        Prescription p = createPrnPrescription(301L, 100L, 240, 4); // Liều đơn là 500mg
        when(prescriptionRepo.findById(301L)).thenReturn(Optional.of(p));

        RecordPrnBody body = new RecordPrnBody();
        body.setDosage(new BigDecimal("1000.0")); // Dùng 1000mg
        body.setReason("Đau đầu nhiều");

        assertThatThrownBy(() -> prnService.recordPrn(301L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.errorCode()).isEqualTo("EXCEEDS_PRESCRIBED_DOSE");
                });
    }
}
