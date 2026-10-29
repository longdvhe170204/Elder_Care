package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.domain.PrescriberType;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.domain.PrnAdministration;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.persistence.PrnAdministrationRepository;
import com.eldercare.backend.medication.internal.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.internal.service.FamilyMedicationService;
import com.eldercare.backend.medication.internal.service.PrnMedicationService;
import com.eldercare.backend.medication.internal.service.request.RecordPrnBody;
import com.eldercare.backend.medication.internal.service.response.PrnAdministrationResponse;
import com.eldercare.backend.medication.internal.service.response.PrnHistoryResponse;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
                mock(FamilyMedicationService.class));
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

    @Test
    @DisplayName("T044: Ghi lần đầu thành công (08:00) (FR-024)")
    void testRecordPrn_FirstDose_Success() {
        Prescription p = createPrnPrescription(301L, 100L, 240, 4);
        when(prescriptionRepo.findById(301L)).thenReturn(Optional.of(p));

        RecordPrnBody body = new RecordPrnBody();
        body.setDosage(new BigDecimal("500.0"));
        body.setReason("Sốt 38.6 độ C");

        when(prnRepo.save(any(PrnAdministration.class))).thenAnswer(inv -> {
            PrnAdministration a = inv.getArgument(0);
            setId(a, 1L);
            return a;
        });

        PrnAdministrationResponse response = prnService.recordPrn(301L, body, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDosage()).isEqualTo(new BigDecimal("500.0"));
        assertThat(response.isExceededLimit()).isFalse();
        verify(prnRepo).save(any(PrnAdministration.class));
    }

    @Test
    @DisplayName("T044: Chưa đủ khoảng cách tối thiểu (cách 4h, thử lúc 3h) -> 409 VUOT_GIOI_HAN_PRN (BR-M07-04)")
    void testRecordPrn_MinimumIntervalNotMet_ThrowsConflict409() {
        Prescription p = createPrnPrescription(301L, 100L, 240, 4); // 240 phút = 4 giờ
        when(prescriptionRepo.findById(301L)).thenReturn(Optional.of(p));

        Instant baseTime = Instant.now();
        // Lần 1 lúc 08:00 (cách đây 3 tiếng)
        Instant dose1Time = baseTime.minus(Duration.ofHours(3));
        PrnAdministration dose1 = createAdminRecord(1L, 301L, dose1Time, new BigDecimal("500.0"));

        when(prnRepo.findInWindow(any(), any())).thenReturn(List.of(dose1));

        RecordPrnBody body = new RecordPrnBody();
        body.setDosage(new BigDecimal("500.0"));
        body.setReason("Còn đau đầu");
        body.setOfflineDeviceTime(baseTime); // Thử dùng lúc 11:00 (mới cách 3h)

        assertThatThrownBy(() -> prnService.recordPrn(301L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("PRN_LIMIT_EXCEEDED");
                });
    }

    @Test
    @DisplayName("T044: Đã đạt số lần tối đa trong 24h trượt (4 lần) -> 409 VUOT_GIOI_HAN_PRN (BR-M07-04)")
    void testRecordPrn_Rolling24hLimitReached_ThrowsConflict409() {
        Prescription p = createPrnPrescription(301L, 100L, 240, 4); // Tối đa 4 lần/24h
        when(prescriptionRepo.findById(301L)).thenReturn(Optional.of(p));

        Instant now = Instant.now();
        // 4 lần trước đó cách nhau 5 tiếng trong 24h: 20h trước, 15h trước, 10h trước, 5h trước
        PrnAdministration d1 = createAdminRecord(1L, 301L, now.minus(Duration.ofHours(20)), new BigDecimal("500.0"));
        PrnAdministration d2 = createAdminRecord(2L, 301L, now.minus(Duration.ofHours(15)), new BigDecimal("500.0"));
        PrnAdministration d3 = createAdminRecord(3L, 301L, now.minus(Duration.ofHours(10)), new BigDecimal("500.0"));
        PrnAdministration d4 = createAdminRecord(4L, 301L, now.minus(Duration.ofHours(5)), new BigDecimal("500.0"));

        when(prnRepo.findInWindow(any(), any())).thenReturn(List.of(d4, d3, d2, d1));

        RecordPrnBody body = new RecordPrnBody();
        body.setDosage(new BigDecimal("500.0"));
        body.setReason("Lần 5 đau lại");
        body.setOfflineDeviceTime(now);

        assertThatThrownBy(() -> prnService.recordPrn(301L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("PRN_LIMIT_EXCEEDED");
                });
    }

    @Test
    @DisplayName("T044: Ghi theo báo lại (FR-027) được lưu kể cả khi vượt giới hạn, gắn vuotGioiHan = true")
    void testRecordPrn_ReportedDose_AllowsSaveWithExceededLimit() {
        Prescription p = createPrnPrescription(301L, 100L, 240, 4);
        when(prescriptionRepo.findById(301L)).thenReturn(Optional.of(p));

        Instant now = Instant.now();
        PrnAdministration d1 = createAdminRecord(1L, 301L, now.minus(Duration.ofHours(1)), new BigDecimal("500.0"));
        when(prnRepo.findInWindow(any(), any())).thenReturn(List.of(d1));

        RecordPrnBody body = new RecordPrnBody();
        body.setDosage(new BigDecimal("500.0"));
        body.setReason("Báo lại đã uống");
        body.setReportedBy("Người nhà báo bệnh nhân tự uống thêm viên");
        body.setOfflineDeviceTime(now);

        when(prnRepo.save(any(PrnAdministration.class))).thenAnswer(inv -> {
            PrnAdministration a = inv.getArgument(0);
            setId(a, 2L);
            return a;
        });

        PrnAdministrationResponse response = prnService.recordPrn(301L, body, 1L);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.isExceededLimit()).isTrue(); // Vượt giới hạn nhưng vẫn được lưu
        verify(prnRepo).save(any(PrnAdministration.class));
    }

    @Test
    @DisplayName("T044: Tra lịch sử PRN trả về danh sách và thời điểm sớm nhất được dùng tiếp (FR-025)")
    void testGetPrnHistory() {
        Prescription p = createPrnPrescription(301L, 100L, 240, 4);
        when(prescriptionRepo.findById(301L)).thenReturn(Optional.of(p));

        Instant now = Instant.now();
        Instant lastAdmin = now.minus(Duration.ofHours(2));
        PrnAdministration d1 = createAdminRecord(1L, 301L, lastAdmin, new BigDecimal("500.0"));

        when(prnRepo.findByPrescriptionIdInOrderByAdministeredAtDesc(any())).thenReturn(List.of(d1));

        PrnHistoryResponse history = prnService.getPrnHistory(301L);

        assertThat(history.getPrescriptionId()).isEqualTo(301L);
        assertThat(history.getAdministrations()).hasSize(1);
        assertThat(history.isCanAdministerNow()).isFalse(); // Vì mới cách 2h, cần chờ thêm 2h nữa
        assertThat(history.getEarliestNextAllowedTime()).isEqualTo(lastAdmin.plus(Duration.ofMinutes(240)));
    }
}
