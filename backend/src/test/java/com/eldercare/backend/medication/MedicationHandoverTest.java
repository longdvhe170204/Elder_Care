package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.spi.TripMedicationPort;
import com.eldercare.backend.medication.internal.domain.DoseStatus;
import com.eldercare.backend.medication.internal.domain.FrequencyType;
import com.eldercare.backend.medication.internal.domain.MedicationDose;
import com.eldercare.backend.medication.internal.domain.MedicationHandover;
import com.eldercare.backend.medication.internal.domain.MedicationHandoverDose;
import com.eldercare.backend.medication.internal.domain.MedicationHandoverItem;
import com.eldercare.backend.medication.internal.domain.MedicationReturn;
import com.eldercare.backend.medication.internal.domain.NoMedicationOrder;
import com.eldercare.backend.medication.internal.domain.PrescriberType;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.domain.RecipientType;
import com.eldercare.backend.medication.internal.persistence.MedicationAbsenceIntervalRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationHandoverDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationHandoverItemRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationReturnItemRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationReturnRepository;
import com.eldercare.backend.medication.internal.persistence.NoMedicationOrderRepository;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.internal.service.MedicationHandoverService;
import com.eldercare.backend.medication.internal.service.MedicationScheduleCalculator;
import com.eldercare.backend.medication.internal.service.request.CreateHandoverBody;
import com.eldercare.backend.medication.internal.service.request.HandoverItemBody;
import com.eldercare.backend.medication.internal.service.request.NoMedicationOrderBody;
import com.eldercare.backend.medication.internal.service.request.ReturnHandoverBody;
import com.eldercare.backend.medication.internal.service.request.ReturnItemBody;
import com.eldercare.backend.medication.internal.service.response.HandoverResponse;
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
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;

/**
 * T039: Test đơn vị cho User Story 5 (Phase 7):
 * Liều khi vắng mặt: Tạm dừng hoặc Mang theo, Giao thuốc, Nhận lại, Báo lại, Lệnh không mang thuốc.
 * (FR-021, FR-027, FR-047, FR-048 -> FR-048b, BR-M07-03)
 */
class MedicationHandoverTest {

    private MedicationAbsenceIntervalRepository absenceRepo;
    private MedicationHandoverRepository handoverRepo;
    private MedicationHandoverItemRepository handoverItemRepo;
    private MedicationHandoverDoseRepository handoverDoseRepo;
    private MedicationReturnRepository returnRepo;
    private MedicationReturnItemRepository returnItemRepo;
    private NoMedicationOrderRepository noMedOrderRepo;
    private PrescriptionRepository prescriptionRepo;
    private MedicationRepository medicationRepo;
    private MedicationDoseRepository doseRepo;
    private ResidentMedicationLock residentMedicationLock;

    private MedicationHandoverService handoverService;

    @BeforeEach
    void setUp() {
        absenceRepo = mock(MedicationAbsenceIntervalRepository.class);
        handoverRepo = mock(MedicationHandoverRepository.class);
        handoverItemRepo = mock(MedicationHandoverItemRepository.class);
        handoverDoseRepo = mock(MedicationHandoverDoseRepository.class);
        returnRepo = mock(MedicationReturnRepository.class);
        returnItemRepo = mock(MedicationReturnItemRepository.class);
        noMedOrderRepo = mock(NoMedicationOrderRepository.class);
        prescriptionRepo = mock(PrescriptionRepository.class);
        medicationRepo = mock(MedicationRepository.class);
        doseRepo = mock(MedicationDoseRepository.class);
        residentMedicationLock = mock(ResidentMedicationLock.class);

        handoverService = new MedicationHandoverService(
                absenceRepo,
                handoverRepo,
                handoverItemRepo,
                handoverDoseRepo,
                returnRepo,
                returnItemRepo,
                noMedOrderRepo,
                prescriptionRepo,
                medicationRepo,
                doseRepo,
                residentMedicationLock,
                new BusinessTime(Clock.systemUTC()),
                new MedicationScheduleCalculator(M07TestParameters.defaults()),
                new StaticListableBeanFactory().getBeanProvider(TripMedicationPort.class));
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

    private Prescription createSamplePrescription(Long id, Long residentId) {
        Prescription p = new Prescription(
                residentId,
                50L,
                new BigDecimal("5.0"),
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
                Instant.parse("2026-10-20T00:00:00Z"),
                LocalDate.of(2026, 11, 20),
                MedicationSource.FACILITY,
                PrescriberType.INTERNAL,
                null,
                null,
                101L,
                null,
                null,
                "Uống sau ăn",
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

    private MedicationDose createSampleDose(Long id, Long prescriptionId, DoseStatus status) {
        MedicationDose d = new MedicationDose(
                prescriptionId,
                100L,
                50L,
                Instant.parse("2026-10-24T15:00:00Z"),
                Instant.parse("2026-10-24T14:30:00Z"),
                Instant.parse("2026-10-24T15:30:00Z"),
                new BigDecimal("5.0"),
                "viên",
                MedicationSource.FACILITY,
                false,
                status,
                1L,
                Instant.now());
        setId(d, id);
        return d;
    }

    @Test
    @DisplayName("T039: Giao thuốc cho Người thân nhưng thiếu tên hoặc mối quan hệ -> 422 INVALID_VALUE")
    void testCreateHandover_RelativeWithoutNameOrRelationship_Throws422() {
        CreateHandoverBody body = new CreateHandoverBody();
        body.setRecipientType("RELATIVE");
        body.setRecipientName("");
        body.setRelationship("");

        assertThatThrownBy(() -> handoverService.createHandover(100L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.errorCode()).isEqualTo("INVALID_VALUE");
                });
    }

    @Test
    @DisplayName("T039: Giao thuốc mang theo thành công: Liều chuyển sang MANG_THEO (DoseStatus.LEAVE_MEDICATION)")
    void testCreateHandover_Success_TransitionsDosesToLeaveMedication() {
        CreateHandoverBody body = new CreateHandoverBody();
        body.setRecipientType("STAFF");
        body.setRecipientName("Điều dưỡng A");

        HandoverItemBody item = new HandoverItemBody();
        item.setPrescriptionId(201L);
        item.setQuantity(new BigDecimal("4.0"));
        body.setItems(List.of(item));

        Prescription p = createSamplePrescription(201L, 100L);
        when(prescriptionRepo.findById(201L)).thenReturn(Optional.of(p));

        when(handoverRepo.save(any(MedicationHandover.class))).thenAnswer(inv -> {
            MedicationHandover h = inv.getArgument(0);
            setId(h, 1L);
            return h;
        });

        when(handoverItemRepo.save(any(MedicationHandoverItem.class))).thenAnswer(inv -> {
            MedicationHandoverItem it = inv.getArgument(0);
            setId(it, 10L);
            return it;
        });

        MedicationDose openDose = createSampleDose(501L, 201L, DoseStatus.UPCOMING);
        when(doseRepo.findByPrescriptionIdAndStatusIn(eq(201L), any())).thenReturn(List.of(openDose));

        HandoverResponse response = handoverService.createHandover(100L, body, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("HANDED_OVER");

        // Liều chưa uống được chuyển sang MANG_THEO
        assertThat(openDose.getStatus()).isEqualTo(DoseStatus.LEAVE_MEDICATION);
        verify(doseRepo).save(openDose);
        verify(handoverDoseRepo).save(any(MedicationHandoverDose.class));
    }

    @Test
    @DisplayName("T039: Nhận lại thuốc mang theo vượt quá số lượng đã giao -> 422 SO_LUONG_KHONG_HOP_LE (FR-048a)")
    void testReturnHandover_ExceededHandedOverQuantity_Throws422() {
        MedicationHandover handover = new MedicationHandover(
                100L, null, null, RecipientType.STAFF, null, "A", null, 1L, Instant.now(), Instant.now());
        setId(handover, 10L);
        when(handoverRepo.findById(10L)).thenReturn(Optional.of(handover));

        // Đã giao 5 viên
        MedicationHandoverItem handedItem =
                new MedicationHandoverItem(10L, 201L, new BigDecimal("5.0"), null, "FACILITY");
        when(handoverItemRepo.findByHandoverId(10L)).thenReturn(List.of(handedItem));

        // Nhận lại 6 viên (vượt quá)
        ReturnHandoverBody body = new ReturnHandoverBody();
        ReturnItemBody returnItem = new ReturnItemBody();
        returnItem.setPrescriptionId(201L);
        returnItem.setQuantity(new BigDecimal("6.0"));
        body.setItems(List.of(returnItem));

        assertThatThrownBy(() -> handoverService.returnHandover(10L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.errorCode()).isEqualTo("INVALID_QUANTITY");
                });
    }

    @Test
    @DisplayName("T039: Nhận lại thuốc tổng số lượng = 0 bắt buộc có lý do (FR-048a)")
    void testReturnHandover_ZeroQuantityWithoutReason_Throws422() {
        MedicationHandover handover = new MedicationHandover(
                100L, null, null, RecipientType.STAFF, null, "A", null, 1L, Instant.now(), Instant.now());
        setId(handover, 11L);
        when(handoverRepo.findById(11L)).thenReturn(Optional.of(handover));

        MedicationHandoverItem handedItem =
                new MedicationHandoverItem(11L, 201L, new BigDecimal("5.0"), null, "FACILITY");
        when(handoverItemRepo.findByHandoverId(11L)).thenReturn(List.of(handedItem));

        // Nhận lại 0 viên không lý do
        ReturnHandoverBody body = new ReturnHandoverBody();
        ReturnItemBody returnItem = new ReturnItemBody();
        returnItem.setPrescriptionId(201L);
        returnItem.setQuantity(BigDecimal.ZERO);
        body.setItems(List.of(returnItem));
        body.setReason("");

        assertThatThrownBy(() -> handoverService.returnHandover(11L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.errorCode()).isEqualTo("INVALID_VALUE");
                });
    }

    @Test
    @DisplayName("T039: Nhận lại thuốc thành công -> Trạng thái lần giao chuyển DA_NHAN_LAI (HandoverStatus.RETURNED)")
    void testReturnHandover_Success() {
        MedicationHandover handover = new MedicationHandover(
                100L, null, null, RecipientType.STAFF, null, "A", null, 1L, Instant.now(), Instant.now());
        setId(handover, 12L);
        when(handoverRepo.findById(12L)).thenReturn(Optional.of(handover));
        when(handoverRepo.save(any())).thenReturn(handover);

        MedicationHandoverItem handedItem =
                new MedicationHandoverItem(12L, 201L, new BigDecimal("5.0"), null, "FACILITY");
        when(handoverItemRepo.findByHandoverId(12L)).thenReturn(List.of(handedItem));

        ReturnHandoverBody body = new ReturnHandoverBody();
        ReturnItemBody returnItem = new ReturnItemBody();
        returnItem.setPrescriptionId(201L);
        returnItem.setQuantity(new BigDecimal("2.0")); // Còn dư 2 viên nhận lại
        body.setItems(List.of(returnItem));
        body.setReason("Đã dùng 3 viên khi đi dã ngoại");

        when(returnRepo.save(any(MedicationReturn.class))).thenAnswer(inv -> {
            MedicationReturn r = inv.getArgument(0);
            setId(r, 99L);
            return r;
        });

        HandoverResponse response = handoverService.returnHandover(12L, body, 1L);

        assertThat(response.getStatus()).isEqualTo("RETURNED");
        verify(handoverRepo).save(handover);
        verify(returnRepo).save(any(MedicationReturn.class));
    }

    @Test
    @DisplayName("T039: Đã có lệnh không mang thuốc cho chuyến đi -> Chặn tạo thêm 409 DA_TON_TAI (FR-048b)")
    void testOrderNoMedication_DuplicateTrip_ThrowsConflict409() {
        when(noMedOrderRepo.findByResidentIdAndTripId(100L, 555L))
                .thenReturn(Optional.of(new NoMedicationOrder(100L, 555L, "Da co", 1L, Instant.now())));

        NoMedicationOrderBody body = new NoMedicationOrderBody();
        body.setTripId(555L);
        body.setReason("Chuyến đi ngắn");

        assertThatThrownBy(() -> handoverService.orderNoMedication(100L, body, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("DA_TON_TAI");
                });
    }
}
