package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.internal.domain.*;
import com.cambae.backend.m07.internal.persistence.*;
import com.cambae.backend.m07.internal.service.FamilyMedicationService;
import com.cambae.backend.m07.internal.service.request.*;
import com.cambae.backend.m07.internal.service.response.FamilyMedicationBatchResponse;
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
import org.springframework.http.HttpStatus;

class FamilyMedicationTest {

    private FamilyMedicationBatchRepository batchRepo;
    private FamilyMedicationPrescriptionRepository batchPrescriptionRepo;
    private FamilyMedicationMovementRepository movementRepo;
    private PrescriptionRepository prescriptionRepo;
    private ResidentMedicationLock residentMedicationLock;

    private FamilyMedicationService familyMedicationService;

    @BeforeEach
    void setUp() {
        batchRepo = mock(FamilyMedicationBatchRepository.class);
        batchPrescriptionRepo = mock(FamilyMedicationPrescriptionRepository.class);
        movementRepo = mock(FamilyMedicationMovementRepository.class);
        prescriptionRepo = mock(PrescriptionRepository.class);
        residentMedicationLock = mock(ResidentMedicationLock.class);

        familyMedicationService = new FamilyMedicationService(
                batchRepo,
                batchPrescriptionRepo,
                movementRepo,
                prescriptionRepo,
                residentMedicationLock,
                new BusinessTime(Clock.systemUTC()));
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

    private FamilyMedicationBatch createBatch(Long id, BigDecimal remaining, FamilyMedicationBatchStatus status) {
        FamilyMedicationBatch b = new FamilyMedicationBatch(
                100L,
                10L,
                "Donepezil",
                "Donepezil HCl",
                "5mg",
                new BigDecimal("30.0"),
                remaining,
                "viên",
                LocalDate.now().plusMonths(6),
                "Nguyên vẹn",
                true,
                null,
                DelivererType.FAMILY_MEMBER,
                200L,
                "Con trai",
                "Con",
                null,
                1L,
                Instant.now(),
                status,
                null,
                Instant.now());
        setId(b, id);
        return b;
    }

    private Prescription createPrescription(Long id, Long residentId, Long medicationId) {
        Prescription p = new Prescription(
                residentId,
                medicationId,
                new BigDecimal("1.0"),
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
                LocalDate.now().plusMonths(1),
                MedicationSource.FAMILY_SENT,
                PrescriberType.INTERNAL,
                null,
                null,
                1L,
                null,
                null,
                null,
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                1L,
                Instant.now(),
                Set.of(medicationId),
                Instant.now());
        setId(p, id);
        return p;
    }

    @Test
    @DisplayName(
            "T047: Tiếp nhận lô thuốc gia đình gửi thành công -> PENDING_RECONCILIATION và ghi RECEIPT movement (FR-040, FR-041)")
    void testReceiveBatch_Success() {
        ReceiveBatchBody body = new ReceiveBatchBody();
        body.setMedicationId(10L);
        body.setInitialQuantity(new BigDecimal("30.0"));
        body.setUnit("viên");
        body.setExpiryDate(LocalDate.now().plusMonths(6));
        body.setPackagingCondition("Hộp nguyên tem");
        body.setHasPrescription(true);
        body.setDelivererType("FAMILY_MEMBER");
        body.setDelivererName("Nguyễn Văn Con");
        body.setRelationship("Con");

        when(batchRepo.save(any(FamilyMedicationBatch.class))).thenAnswer(inv -> {
            FamilyMedicationBatch b = inv.getArgument(0);
            setId(b, 1L);
            return b;
        });

        FamilyMedicationBatchResponse response = familyMedicationService.receiveBatch(100L, body, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PENDING_RECONCILIATION");
        assertThat(response.getRemainingQuantity()).isEqualByComparingTo("30.0");
        verify(movementRepo).save(any(FamilyMedicationMovement.class));
    }

    @Test
    @DisplayName(
            "BR-M07-10: Lô thuốc ở Chờ đối chiếu (PENDING_RECONCILIATION) -> Bị chặn không cho dùng liều (LO_CHUA_DOI_CHIEU)")
    void testRecordUsage_PendingReconciliation_ThrowsConflict409() {
        FamilyMedicationBatch batch =
                createBatch(1L, new BigDecimal("30.0"), FamilyMedicationBatchStatus.PENDING_RECONCILIATION);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> familyMedicationService.recordUsage(
                        1L, new BigDecimal("1.0"), MovementType.DOSE_USAGE, "DOSE", 101L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("BATCH_NOT_RECONCILED");
                });
    }

    @Test
    @DisplayName("BR-M07-10: Xác nhận sử dụng bao bì không nhận diện được -> 422 BAO_BI_KHONG_NHAN_DIEN_DUOC")
    void testConfirmUse_PackagingNotRecognizable_Throws422() {
        FamilyMedicationBatch batch =
                createBatch(1L, new BigDecimal("30.0"), FamilyMedicationBatchStatus.PENDING_RECONCILIATION);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));

        ConfirmBatchUseBody body = new ConfirmBatchUseBody();
        body.setPrescriptionIds(List.of(101L));
        body.setPackagingRecognizable(false);

        assertThatThrownBy(() -> familyMedicationService.confirmUse(1L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.errorCode()).isEqualTo("PACKAGING_NOT_RECOGNIZABLE");
                });
    }

    @Test
    @DisplayName("BR-M07-10: Xác nhận sử dụng thuốc đã hết hạn dùng -> 409 THUOC_DA_HET_HAN")
    void testConfirmUse_ExpiredMedication_ThrowsConflict409() {
        FamilyMedicationBatch batch =
                createBatch(1L, new BigDecimal("30.0"), FamilyMedicationBatchStatus.PENDING_RECONCILIATION);
        // Hết hạn từ hôm qua
        Field expiryField;
        try {
            expiryField = FamilyMedicationBatch.class.getDeclaredField("expiryDate");
            expiryField.setAccessible(true);
            expiryField.set(batch, LocalDate.now().minusDays(1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));

        ConfirmBatchUseBody body = new ConfirmBatchUseBody();
        body.setPrescriptionIds(List.of(101L));
        body.setPackagingRecognizable(true);

        assertThatThrownBy(() -> familyMedicationService.confirmUse(1L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("EXPIRED_MEDICATION");
                });
    }

    @Test
    @DisplayName("BR-M07-18: Xác nhận sử dụng thành công -> Chuyển IN_USE và gắn đơn thuốc")
    void testConfirmUse_Success() {
        FamilyMedicationBatch batch =
                createBatch(1L, new BigDecimal("30.0"), FamilyMedicationBatchStatus.PENDING_RECONCILIATION);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));

        Prescription p = createPrescription(101L, 100L, 10L);
        when(prescriptionRepo.findAllById(List.of(101L))).thenReturn(List.of(p));

        ConfirmBatchUseBody body = new ConfirmBatchUseBody();
        body.setPrescriptionIds(List.of(101L));
        body.setPackagingRecognizable(true);

        when(batchRepo.save(any(FamilyMedicationBatch.class))).thenReturn(batch);

        FamilyMedicationBatchResponse response = familyMedicationService.confirmUse(1L, body, 1L);

        assertThat(batch.getStatus()).isEqualTo(FamilyMedicationBatchStatus.IN_USE);
        verify(batchPrescriptionRepo).save(any(FamilyMedicationPrescription.class));
    }

    @Test
    @DisplayName("BR-M07-19: Hoàn trả thuốc số lượng khác số lượng còn -> 409 SO_LUONG_KHAC_SO_CON")
    void testReturnBatch_MismatchQuantity_ThrowsConflict409() {
        FamilyMedicationBatch batch = createBatch(1L, new BigDecimal("25.0"), FamilyMedicationBatchStatus.HOLD_ONLY);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));

        ReturnBatchBody body = new ReturnBatchBody();
        body.setQuantity(new BigDecimal("20.0")); // Khác 25.0
        body.setRecipientName("Người nhà A");

        assertThatThrownBy(() -> familyMedicationService.returnBatch(1L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("QUANTITY_DIFFERS_FROM_REMAINING");
                });
    }

    @Test
    @DisplayName("BR-M07-19: Hoàn trả thuốc thành công -> Chuyển RETURNED và số lượng còn về 0")
    void testReturnBatch_Success() {
        FamilyMedicationBatch batch = createBatch(1L, new BigDecimal("25.0"), FamilyMedicationBatchStatus.HOLD_ONLY);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));

        ReturnBatchBody body = new ReturnBatchBody();
        body.setQuantity(new BigDecimal("25.0"));
        body.setRecipientName("Người nhà A");
        body.setRelationship("Con gái");

        familyMedicationService.returnBatch(1L, body, 1L);

        assertThat(batch.getStatus()).isEqualTo(FamilyMedicationBatchStatus.RETURNED);
        assertThat(batch.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(movementRepo).save(any(FamilyMedicationMovement.class));
    }

    @Test
    @DisplayName("BR-M07-19: Hủy thuốc theo yêu cầu gia đình số lượng khác số còn -> 409 SO_LUONG_KHAC_SO_CON")
    void testDisposeBatch_MismatchQuantity_ThrowsConflict409() {
        FamilyMedicationBatch batch = createBatch(1L, new BigDecimal("10.0"), FamilyMedicationBatchStatus.HOLD_ONLY);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));

        DisposeBatchBody body = new DisposeBatchBody();
        body.setQuantity(new BigDecimal("5.0")); // Khác 10.0
        body.setWitness("Y tá B");
        body.setReason("Gia đình yêu cầu tiêu hủy do đổi phác đồ");

        assertThatThrownBy(() -> familyMedicationService.disposeBatch(1L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("QUANTITY_DIFFERS_FROM_REMAINING");
                });
    }

    @Test
    @DisplayName("BR-M07-11: Trừ thuốc khi dùng liều: nếu số lượng về 0 -> Tự động chuyển sang DEPLETED")
    void testRecordUsage_DepletedWhenZero() {
        FamilyMedicationBatch batch = createBatch(1L, new BigDecimal("2.0"), FamilyMedicationBatchStatus.IN_USE);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));

        familyMedicationService.recordUsage(1L, new BigDecimal("2.0"), MovementType.DOSE_USAGE, "DOSE", 501L, 1L);

        assertThat(batch.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(batch.getStatus()).isEqualTo(FamilyMedicationBatchStatus.DEPLETED);
    }
}
