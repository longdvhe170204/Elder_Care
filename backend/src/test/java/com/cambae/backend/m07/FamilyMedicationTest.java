package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.api.events.FamilyMedicationRunningLow;
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
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

class FamilyMedicationTest {

    private FamilyMedicationBatchRepository batchRepo;
    private FamilyMedicationPrescriptionRepository batchPrescriptionRepo;
    private PrescriptionRepository prescriptionRepo;
    private MedicationRepository medicationRepo;
    private ResidentMedicationLock residentMedicationLock;
    private ApplicationEventPublisher events;

    private FamilyMedicationService familyMedicationService;

    @BeforeEach
    void setUp() {
        batchRepo = mock(FamilyMedicationBatchRepository.class);
        batchPrescriptionRepo = mock(FamilyMedicationPrescriptionRepository.class);
        prescriptionRepo = mock(PrescriptionRepository.class);
        medicationRepo = mock(MedicationRepository.class);
        residentMedicationLock = mock(ResidentMedicationLock.class);
        events = mock(ApplicationEventPublisher.class);

        familyMedicationService = new FamilyMedicationService(
                batchRepo,
                batchPrescriptionRepo,
                prescriptionRepo,
                medicationRepo,
                residentMedicationLock,
                events,
                new BusinessTime(Clock.systemUTC()));
    }

    private void setField(Object entity, String name, Object value) {
        try {
            Field f = entity.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(entity, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FamilyMedicationBatch createBatch(Long id, FamilyMedicationBatchStatus status) {
        FamilyMedicationBatch b = new FamilyMedicationBatch(
                100L,
                10L,
                "Donepezil",
                "Donepezil HCl",
                "5mg",
                new BigDecimal("30.0"),
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
        setField(b, "id", id);
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
        setField(p, "id", id);
        return p;
    }

    private static void assertBusiness(Throwable e, HttpStatus status, String code) {
        BusinessException be = (BusinessException) e;
        assertThat(be.status()).isEqualTo(status);
        assertThat(be.errorCode()).isEqualTo(code);
    }

    @Test
    @DisplayName(
            "FR-040, FR-045 (Q-393): Tiếp nhận lô -> Chờ đối chiếu, lưu số lượng lúc tiếp nhận, không có biến động")
    void receiveBatch_keepsInitialQuantityOnly() {
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
            setField(b, "id", 1L);
            return b;
        });

        FamilyMedicationBatchResponse response = familyMedicationService.receiveBatch(100L, body, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PENDING_RECONCILIATION");
        assertThat(response.getInitialQuantity()).isEqualByComparingTo("30.0");
        assertThat(response.getFinalQuantity()).isNull();
    }

    @Test
    @DisplayName("BR-M07-10: Xác nhận sử dụng bao bì không nhận diện được -> 422")
    void confirmUse_packagingNotRecognizable_422() {
        when(batchRepo.findById(1L))
                .thenReturn(Optional.of(createBatch(1L, FamilyMedicationBatchStatus.PENDING_RECONCILIATION)));
        ConfirmBatchUseBody body = new ConfirmBatchUseBody();
        body.setPrescriptionIds(List.of(101L));
        body.setPackagingRecognizable(false);

        assertThatThrownBy(() -> familyMedicationService.confirmUse(1L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertBusiness(e, HttpStatus.UNPROCESSABLE_ENTITY, "PACKAGING_NOT_RECOGNIZABLE"));
    }

    @Test
    @DisplayName("BR-M07-10: Xác nhận sử dụng thuốc đã hết hạn dùng -> 409 EXPIRED_MEDICATION")
    void confirmUse_expired_409() {
        FamilyMedicationBatch batch = createBatch(1L, FamilyMedicationBatchStatus.PENDING_RECONCILIATION);
        setField(batch, "expiryDate", LocalDate.now().minusDays(1));
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));
        ConfirmBatchUseBody body = new ConfirmBatchUseBody();
        body.setPrescriptionIds(List.of(101L));
        body.setPackagingRecognizable(true);

        assertThatThrownBy(() -> familyMedicationService.confirmUse(1L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertBusiness(e, HttpStatus.CONFLICT, "EXPIRED_MEDICATION"));
    }

    @Test
    @DisplayName("BR-M07-18: Xác nhận sử dụng thành công -> Được sử dụng và gắn đơn")
    void confirmUse_success() {
        FamilyMedicationBatch batch = createBatch(1L, FamilyMedicationBatchStatus.PENDING_RECONCILIATION);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));
        when(prescriptionRepo.findAllById(List.of(101L))).thenReturn(List.of(createPrescription(101L, 100L, 10L)));
        when(batchRepo.save(any(FamilyMedicationBatch.class))).thenReturn(batch);
        ConfirmBatchUseBody body = new ConfirmBatchUseBody();
        body.setPrescriptionIds(List.of(101L));
        body.setPackagingRecognizable(true);

        familyMedicationService.confirmUse(1L, body, 1L);

        assertThat(batch.getStatus()).isEqualTo(FamilyMedicationBatchStatus.IN_USE);
        verify(batchPrescriptionRepo).save(any(FamilyMedicationPrescription.class));
    }

    @Test
    @DisplayName("BR-M07-19 (Q-393): Hoàn trả ghi số lượng đếm thực tế, không cần bằng số lúc tiếp nhận")
    void returnBatch_recordsCountedQuantity() {
        FamilyMedicationBatch batch = createBatch(1L, FamilyMedicationBatchStatus.HOLD_ONLY);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));
        ReturnBatchBody body = new ReturnBatchBody();
        body.setQuantity(new BigDecimal("12.0"));
        body.setRecipientName("Người nhà A");
        body.setRelationship("Con gái");

        familyMedicationService.returnBatch(1L, body, 7L);

        assertThat(batch.getStatus()).isEqualTo(FamilyMedicationBatchStatus.RETURNED);
        assertThat(batch.getFinalQuantity()).isEqualByComparingTo("12.0");
        assertThat(batch.getClosedById()).isEqualTo(7L);
        assertThat(batch.getReturnRecipientName()).isEqualTo("Người nhà A");
    }

    @Test
    @DisplayName("BR-M07-19: Hoàn trả lô đang Được sử dụng -> 409 (phải Ngừng sử dụng trước)")
    void returnBatch_inUse_409() {
        when(batchRepo.findById(1L)).thenReturn(Optional.of(createBatch(1L, FamilyMedicationBatchStatus.IN_USE)));
        ReturnBatchBody body = new ReturnBatchBody();
        body.setQuantity(new BigDecimal("5"));
        body.setRecipientName("Người nhà A");

        assertThatThrownBy(() -> familyMedicationService.returnBatch(1L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertBusiness(e, HttpStatus.CONFLICT, "INVALID_STATE"));
    }

    @Test
    @DisplayName("BR-M07-19 (Q-393): Hủy theo yêu cầu gia đình ghi số lượng đếm thực tế và người chứng kiến")
    void disposeBatch_recordsCountedQuantityAndWitness() {
        FamilyMedicationBatch batch = createBatch(1L, FamilyMedicationBatchStatus.HOLD_ONLY);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));
        DisposeBatchBody body = new DisposeBatchBody();
        body.setQuantity(new BigDecimal("5.0"));
        body.setWitness("Y tá B");
        body.setReason("Gia đình yêu cầu tiêu hủy do đổi phác đồ");

        familyMedicationService.disposeBatch(1L, body, 1L);

        assertThat(batch.getStatus()).isEqualTo(FamilyMedicationBatchStatus.DISPOSED);
        assertThat(batch.getFinalQuantity()).isEqualByComparingTo("5.0");
        assertThat(batch.getWitness()).isEqualTo("Y tá B");
    }

    @Test
    @DisplayName("FR-041, FR-045 (Q-393): Ghi dùng hết -> Đã dùng hết, gỡ liên kết đơn")
    void markDepleted_inUse_becomesDepleted() {
        FamilyMedicationBatch batch = createBatch(1L, FamilyMedicationBatchStatus.IN_USE);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));
        FamilyMedicationPrescription link = new FamilyMedicationPrescription(1L, 101L, Instant.now());
        when(batchPrescriptionRepo.findByBatchIdAndBoundToIsNull(1L)).thenReturn(List.of(link));

        familyMedicationService.markDepleted(1L, 7L);

        assertThat(batch.getStatus()).isEqualTo(FamilyMedicationBatchStatus.DEPLETED);
        assertThat(batch.getClosedById()).isEqualTo(7L);
        assertThat(link.getBoundTo()).isNotNull();
    }

    @Test
    @DisplayName("FR-041 (Q-393): Ghi dùng hết khi lô chưa Được sử dụng -> 409")
    void markDepleted_notInUse_409() {
        when(batchRepo.findById(1L)).thenReturn(Optional.of(createBatch(1L, FamilyMedicationBatchStatus.HOLD_ONLY)));

        assertThatThrownBy(() -> familyMedicationService.markDepleted(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertBusiness(e, HttpStatus.CONFLICT, "INVALID_STATE"));
    }

    @Test
    @DisplayName("FR-046 (Q-393): Báo gia đình sắp hết -> một yêu cầu thông báo mức Trung bình cho người liên hệ chính,"
            + " người đại diện")
    void notifyRunningLow_publishesOneMediumNotification() {
        when(batchRepo.findById(1L)).thenReturn(Optional.of(createBatch(1L, FamilyMedicationBatchStatus.IN_USE)));

        familyMedicationService.notifyRunningLow(1L, "Còn khoảng 4 ngày", 7L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        FamilyMedicationRunningLow event = (FamilyMedicationRunningLow) captor.getValue();
        assertThat(event.note()).isEqualTo("Còn khoảng 4 ngày");
        NotificationRequest request = event.notificationRequests().getFirst();
        assertThat(event.notificationRequests()).hasSize(1);
        assertThat(request.level()).contains(NotificationLevel.MEDIUM);
        assertThat(request.recipientGroups())
                .containsExactlyInAnyOrder(
                        new RecipientGroup.PrimaryContact(100L), new RecipientGroup.Representative(100L));
    }

    @Test
    @DisplayName("FR-046 (Q-393): Báo gia đình sắp hết khi lô không Được sử dụng -> 409, không gửi")
    void notifyRunningLow_notInUse_409() {
        when(batchRepo.findById(1L)).thenReturn(Optional.of(createBatch(1L, FamilyMedicationBatchStatus.DEPLETED)));

        assertThatThrownBy(() -> familyMedicationService.notifyRunningLow(1L, null, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertBusiness(e, HttpStatus.CONFLICT, "INVALID_STATE"));
        verifyNoInteractions(events);
    }

    @Test
    @DisplayName("FR-044 (Q-393): Đơn còn lô Được sử dụng chưa hết hạn thì xác nhận liều được; hết lô thì không")
    void hasUsableBatch_onlyInUseUnexpiredBound() {
        FamilyMedicationBatch inUse = createBatch(1L, FamilyMedicationBatchStatus.IN_USE);
        FamilyMedicationBatch expired = createBatch(2L, FamilyMedicationBatchStatus.IN_USE);
        setField(expired, "expiryDate", LocalDate.now().minusDays(1));
        when(batchPrescriptionRepo.findByPrescriptionIdAndBoundToIsNull(101L))
                .thenReturn(List.of(
                        new FamilyMedicationPrescription(1L, 101L, Instant.now()),
                        new FamilyMedicationPrescription(2L, 101L, Instant.now())));
        when(batchRepo.findByResidentIdAndStatus(100L, FamilyMedicationBatchStatus.IN_USE))
                .thenReturn(List.of(inUse, expired));

        assertThat(familyMedicationService.hasUsableBatch(100L, 101L)).isTrue();

        when(batchRepo.findByResidentIdAndStatus(100L, FamilyMedicationBatchStatus.IN_USE))
                .thenReturn(List.of(expired));
        assertThat(familyMedicationService.hasUsableBatch(100L, 101L)).isFalse();
    }
}
