package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.internal.domain.DoseStatus;
import com.cambae.backend.m07.internal.domain.FrequencyType;
import com.cambae.backend.m07.internal.domain.MedicationDose;
import com.cambae.backend.m07.internal.domain.MedicationReconciliation;
import com.cambae.backend.m07.internal.domain.PrescriberType;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import com.cambae.backend.m07.internal.domain.PrescriptionType;
import com.cambae.backend.m07.internal.domain.ReconciliationCorrectionRequest;
import com.cambae.backend.m07.internal.domain.ReconciliationDecision;
import com.cambae.backend.m07.internal.domain.ReconciliationItem;
import com.cambae.backend.m07.internal.domain.ReconciliationItemSource;
import com.cambae.backend.m07.internal.domain.ReconciliationReason;
import com.cambae.backend.m07.internal.domain.SuspensionReason;
import com.cambae.backend.m07.internal.persistence.MedicationDoseRepository;
import com.cambae.backend.m07.internal.persistence.MedicationReconciliationRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import com.cambae.backend.m07.internal.persistence.ReconciliationCorrectionRequestRepository;
import com.cambae.backend.m07.internal.persistence.ReconciliationItemRepository;
import com.cambae.backend.m07.internal.persistence.ResidentMedicationLock;
import com.cambae.backend.m07.internal.service.MedicationReconciliationService;
import com.cambae.backend.m07.internal.service.PrescriptionService;
import com.cambae.backend.m07.web.request.CreateReconciliationBody;
import com.cambae.backend.m07.web.request.ProposeCorrectionBody;
import com.cambae.backend.m07.web.response.ReconciliationCorrectionResponse;
import com.cambae.backend.m07.web.response.ReconciliationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * T034 & T060: Test đơn vị cho User Story 4 (Phase 6):
 * Đối chiếu thuốc khi tiếp nhận, trở về từ bệnh viện và theo yêu cầu chuyên môn.
 * (FR-034 -> FR-039, BR-M07-09, BR-M07-25, SC-014, Q-55, Q-340)
 */
class MedicationReconciliationTest {

    private MedicationReconciliationRepository reconciliationRepo;
    private ReconciliationItemRepository itemRepo;
    private ReconciliationCorrectionRequestRepository correctionRepo;
    private PrescriptionRepository prescriptionRepo;
    private MedicationDoseRepository doseRepo;
    private PrescriptionService prescriptionService;
    private ResidentMedicationLock residentMedicationLock;
    private ObjectMapper objectMapper;

    private MedicationReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationRepo = mock(MedicationReconciliationRepository.class);
        itemRepo = mock(ReconciliationItemRepository.class);
        correctionRepo = mock(ReconciliationCorrectionRequestRepository.class);
        prescriptionRepo = mock(PrescriptionRepository.class);
        doseRepo = mock(MedicationDoseRepository.class);
        prescriptionService = mock(PrescriptionService.class);
        residentMedicationLock = mock(ResidentMedicationLock.class);
        objectMapper = new ObjectMapper();

        reconciliationService = new MedicationReconciliationService(
                reconciliationRepo,
                itemRepo,
                correctionRepo,
                prescriptionRepo,
                doseRepo,
                prescriptionService,
                residentMedicationLock,
                objectMapper);
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

    private Prescription createSamplePrescription(Long id, Long residentId, PrescriptionStatus status) {
        Prescription p = new Prescription(
                residentId,
                50L,
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
                PrescriberType.INTERNAL,
                null,
                null,
                101L,
                null,
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

    private MedicationDose createSampleDose(Long id, Long prescriptionId, DoseStatus status) {
        MedicationDose d = new MedicationDose(
                prescriptionId,
                100L,
                50L,
                Instant.parse("2026-10-24T15:00:00Z"),
                Instant.parse("2026-10-24T14:30:00Z"),
                Instant.parse("2026-10-24T15:30:00Z"),
                new BigDecimal("5.0"),
                "mg",
                MedicationSource.FACILITY,
                false,
                status,
                1L);
        setId(d, id);
        return d;
    }

    @Test
    @DisplayName("T060: Lập phiếu theo yêu cầu chuyên môn thiếu lý do lập -> 422 INVALID_VALUE (FR-034b)")
    void testCreate_ProfessionalRequest_MissingReasonDetail_Throws422() {
        CreateReconciliationBody body = new CreateReconciliationBody();
        body.setReason("THEO_YEU_CAU_CHUYEN_MON");
        body.setReasonDetail(""); // Rỗng

        assertThatThrownBy(() -> reconciliationService.create(100L, body, 1L, true))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.errorCode()).isEqualTo("INVALID_VALUE");
                });
    }

    @Test
    @DisplayName(
            "T060: Lập phiếu theo yêu cầu chuyên môn thành công -> Mở, không tạm dừng đơn, không đặt hạn (FR-034b, BR-M07-25)")
    void testCreate_ProfessionalRequest_Success_NoDeadline_NoPrescriptionsSuspended() {
        CreateReconciliationBody body = new CreateReconciliationBody();
        body.setReason("THEO_YEU_CAU_CHUYEN_MON");
        body.setReasonDetail("Bác sĩ yêu cầu đánh giá lại tương tác thuốc");

        when(reconciliationRepo.findOpenOrPendingByResidentId(100L)).thenReturn(Optional.empty());
        when(reconciliationRepo.save(any(MedicationReconciliation.class))).thenAnswer(inv -> {
            MedicationReconciliation ticket = inv.getArgument(0);
            setId(ticket, 1L);
            return ticket;
        });

        ReconciliationResponse response = reconciliationService.create(100L, body, 1L, true);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getDeadlineAt()).isNull(); // Không có hạn
        assertThat(response.getReasonDetail()).isEqualTo("Bác sĩ yêu cầu đánh giá lại tương tác thuốc");

        // Không đơn nào bị tạm dừng
        verify(prescriptionRepo, never()).findByResidentIdAndStatusIn(any(), any());
    }

    @Test
    @DisplayName("T034: Lập phiếu Tiếp nhận -> Tạm dừng các đơn hiệu lực, đặt hạn đối chiếu 24 giờ (FR-034, BR-M07-09)")
    void testCreate_Admission_Success_SuspendsActivePrescriptions_Sets24hDeadline() {
        CreateReconciliationBody body = new CreateReconciliationBody();
        body.setReason("TIEP_NHAN");

        when(reconciliationRepo.findOpenOrPendingByResidentId(100L)).thenReturn(Optional.empty());

        Prescription activePrescription = createSamplePrescription(201L, 100L, PrescriptionStatus.ACTIVE);
        when(prescriptionRepo.findByResidentIdAndStatusIn(eq(100L), any())).thenReturn(List.of(activePrescription));

        when(reconciliationRepo.save(any(MedicationReconciliation.class))).thenAnswer(inv -> {
            MedicationReconciliation ticket = inv.getArgument(0);
            setId(ticket, 2L);
            return ticket;
        });

        ReconciliationResponse response = reconciliationService.create(100L, body, 1L, false);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getDeadlineAt()).isNotNull(); // Hạn 24h
        assertThat(response.getDeadlineAt()).isAfter(Instant.now().plus(23, ChronoUnit.HOURS));

        // Đơn hiệu lực đã bị tạm dừng chờ đối chiếu
        assertThat(activePrescription.getStatus()).isEqualTo(PrescriptionStatus.SUSPENDED);
        assertThat(activePrescription.getSuspensionReason()).isEqualTo(SuspensionReason.RECONCILIATION_PENDING);
        verify(prescriptionRepo).save(activePrescription);
    }

    @Test
    @DisplayName("T034: Đã có phiếu Mở hoặc Chờ xác nhận -> Chặn lập phiếu mới 409 DA_CO_PHIEU_MO (FR-034)")
    void testCreate_AlreadyHasOpenTicket_ThrowsConflict409() {
        MedicationReconciliation existing = new MedicationReconciliation(
                100L,
                ReconciliationReason.ADMISSION,
                null,
                Instant.now().plus(24, ChronoUnit.HOURS),
                1L,
                Instant.now());
        when(reconciliationRepo.findOpenOrPendingByResidentId(100L)).thenReturn(Optional.of(existing));

        CreateReconciliationBody body = new CreateReconciliationBody();
        body.setReason("TRO_VE_BENH_VIEN");

        assertThatThrownBy(() -> reconciliationService.create(100L, body, 2L, false))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("DA_CO_PHIEU_MO");
                });
    }

    @Test
    @DisplayName("T060: Gửi xác nhận khi còn đơn thuốc chưa có dòng đối chiếu -> 409 THIEU_DONG (FR-034, BR-M07-25)")
    void testSubmit_MissingLinesForExistingPrescription_ThrowsConflict409() {
        MedicationReconciliation ticket = new MedicationReconciliation(
                100L, ReconciliationReason.PROFESSIONAL_REQUEST, "Kiem tra", null, 1L, Instant.now());
        setId(ticket, 5L);
        when(reconciliationRepo.findById(5L)).thenReturn(Optional.of(ticket));

        // Người cao tuổi có đơn 201 và 202
        Prescription p1 = createSamplePrescription(201L, 100L, PrescriptionStatus.ACTIVE);
        Prescription p2 = createSamplePrescription(202L, 100L, PrescriptionStatus.ACTIVE);
        when(prescriptionRepo.findByResidentIdAndStatusIn(eq(100L), any())).thenReturn(List.of(p1, p2));

        // Phiếu chỉ mới có dòng cho đơn 201
        ReconciliationItem item1 = new ReconciliationItem(
                5L,
                1,
                ReconciliationItemSource.CURRENT_PRESCRIPTION,
                201L,
                null,
                "Thuoc A",
                ReconciliationDecision.CONTINUE,
                null,
                null,
                1L,
                Instant.now());
        when(itemRepo.findByReconciliationIdOrderByItemOrderAsc(5L)).thenReturn(List.of(item1));

        assertThatThrownBy(() -> reconciliationService.submit(5L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("THIEU_DONG");
                });
    }

    @Test
    @DisplayName("T034: Gửi xác nhận có dòng Ngừng nhưng thiếu lý do -> 409 THIEU_DONG (FR-034)")
    void testSubmit_DiscontinueWithoutReason_ThrowsConflict409() {
        MedicationReconciliation ticket = new MedicationReconciliation(
                100L,
                ReconciliationReason.ADMISSION,
                null,
                Instant.now().plus(24, ChronoUnit.HOURS),
                1L,
                Instant.now());
        setId(ticket, 6L);
        when(reconciliationRepo.findById(6L)).thenReturn(Optional.of(ticket));

        Prescription p1 = createSamplePrescription(201L, 100L, PrescriptionStatus.ACTIVE);
        when(prescriptionRepo.findByResidentIdAndStatusIn(eq(100L), any())).thenReturn(List.of(p1));

        // Dòng Ngừng thiếu lý do
        ReconciliationItem item1 = new ReconciliationItem(
                6L,
                1,
                ReconciliationItemSource.CURRENT_PRESCRIPTION,
                201L,
                null,
                "Thuoc A",
                ReconciliationDecision.DISCONTINUE,
                "",
                null,
                1L,
                Instant.now());
        when(itemRepo.findByReconciliationIdOrderByItemOrderAsc(6L)).thenReturn(List.of(item1));

        assertThatThrownBy(() -> reconciliationService.submit(6L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("THIEU_DONG");
                });
    }

    @Test
    @DisplayName(
            "T034 & T060: Quy tắc hai người: Người xác nhận trùng người gửi / lập / ghi dòng -> 409 QUY_TAC_HAI_NGUOI (SC-014, Q-55)")
    void testConfirm_TwoPersonRule_SameUser_ThrowsConflict409() {
        MedicationReconciliation ticket = new MedicationReconciliation(
                100L, ReconciliationReason.ADMISSION, null, Instant.now(), 10L, Instant.now());
        setId(ticket, 7L);
        ticket.submit(10L, Instant.now()); // Người gửi là ID = 10
        when(reconciliationRepo.findById(7L)).thenReturn(Optional.of(ticket));

        // Thử xác nhận bởi chính người gửi (ID = 10)
        assertThatThrownBy(() -> reconciliationService.confirm(7L, 10L, true))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("QUY_TAC_HAI_NGUOI");
                });

        // Người gửi ID = 10, người xác nhận ID = 20 nhưng ID 20 là người ghi dòng
        ticket.submit(10L, Instant.now());
        ReconciliationItem item = new ReconciliationItem(
                7L,
                1,
                ReconciliationItemSource.CURRENT_PRESCRIPTION,
                201L,
                null,
                "Thuoc A",
                ReconciliationDecision.CONTINUE,
                null,
                null,
                20L,
                Instant.now()); // Người ghi dòng là ID = 20
        when(itemRepo.findByReconciliationIdOrderByItemOrderAsc(7L)).thenReturn(List.of(item));

        assertThatThrownBy(() -> reconciliationService.confirm(7L, 20L, true))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("QUY_TAC_HAI_NGUOI");
                });
    }

    @Test
    @DisplayName(
            "T034: Xác nhận phiếu thành công bởi người khác: Áp quyết định Tiếp tục (khôi phục đơn) & Ngừng (hủy đơn và liều) (FR-036, BR-M07-09)")
    void testConfirm_Success_AppliesDecisions() {
        MedicationReconciliation ticket = new MedicationReconciliation(
                100L, ReconciliationReason.ADMISSION, null, Instant.now(), 10L, Instant.now());
        setId(ticket, 8L);
        ticket.submit(10L, Instant.now()); // Người gửi ID = 10
        when(reconciliationRepo.findById(8L)).thenReturn(Optional.of(ticket));
        when(reconciliationRepo.save(any())).thenReturn(ticket);

        // Đơn 1 đang Tạm dừng
        Prescription p1 = createSamplePrescription(201L, 100L, PrescriptionStatus.ACTIVE);
        p1.suspend(SuspensionReason.RECONCILIATION_PENDING);
        when(prescriptionRepo.findById(201L)).thenReturn(Optional.of(p1));

        // Đơn 2 đang Tạm dừng
        Prescription p2 = createSamplePrescription(202L, 100L, PrescriptionStatus.ACTIVE);
        p2.suspend(SuspensionReason.RECONCILIATION_PENDING);
        when(prescriptionRepo.findById(202L)).thenReturn(Optional.of(p2));

        // Dòng 1: Tiếp tục đơn 201; Dòng 2: Ngừng đơn 202 (Người ghi dòng ID = 11)
        ReconciliationItem item1 = new ReconciliationItem(
                8L,
                1,
                ReconciliationItemSource.CURRENT_PRESCRIPTION,
                201L,
                null,
                "Thuoc A",
                ReconciliationDecision.CONTINUE,
                null,
                null,
                11L,
                Instant.now());
        ReconciliationItem item2 = new ReconciliationItem(
                8L,
                2,
                ReconciliationItemSource.CURRENT_PRESCRIPTION,
                202L,
                null,
                "Thuoc B",
                ReconciliationDecision.DISCONTINUE,
                "Ngừng theo chỉ định",
                null,
                11L,
                Instant.now());
        when(itemRepo.findByReconciliationIdOrderByItemOrderAsc(8L)).thenReturn(List.of(item1, item2));

        // Liều chưa đóng của đơn 2
        MedicationDose openDose = createSampleDose(501L, 202L, DoseStatus.UPCOMING);
        when(doseRepo.findByPrescriptionIdAndStatusIn(eq(202L), any())).thenReturn(List.of(openDose));

        // Bác sĩ ID = 30 xác nhận (khác người gửi 10, khác người lập 10, khác người ghi dòng 11)
        ReconciliationResponse response = reconciliationService.confirm(8L, 30L, true);

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(ticket.getConfirmedById()).isEqualTo(30L);

        // Đơn 1 được khôi phục thành ACTIVE
        assertThat(p1.getStatus()).isEqualTo(PrescriptionStatus.ACTIVE);
        verify(prescriptionRepo).save(p1);

        // Đơn 2 được ngừng DISCONTINUED
        assertThat(p2.getStatus()).isEqualTo(PrescriptionStatus.DISCONTINUED);
        verify(prescriptionRepo).save(p2);

        // Liều chưa đóng của đơn 2 bị hủy
        assertThat(openDose.getStatus()).isEqualTo(DoseStatus.CANCELLED);
        verify(doseRepo).save(openDose);
    }

    @Test
    @DisplayName("T034: Đề nghị đính chính và xác nhận đính chính tuân thủ quy tắc hai người (FR-039, BR-M07-17)")
    void testCorrection_ProposeAndConfirm_TwoPersonRule() {
        MedicationReconciliation ticket = new MedicationReconciliation(
                100L, ReconciliationReason.ADMISSION, null, Instant.now(), 10L, Instant.now());
        setId(ticket, 9L);
        ticket.submit(10L, Instant.now());
        ticket.confirm(30L, Instant.now()); // Trạng thái CONFIRMED
        when(reconciliationRepo.findById(9L)).thenReturn(Optional.of(ticket));

        // 1. Người ngoài (ID = 99) đề nghị đính chính -> 403 FORBIDDEN
        ProposeCorrectionBody body = new ProposeCorrectionBody();
        body.setReason("Sửa nhầm mô tả");
        body.setCorrectedContent(Map.of("note", "Sua dung"));

        assertThatThrownBy(() -> reconciliationService.proposeCorrection(9L, body, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        // 2. Người lập (ID = 10) đề nghị đính chính -> Thành công
        when(correctionRepo.save(any(ReconciliationCorrectionRequest.class))).thenAnswer(inv -> {
            ReconciliationCorrectionRequest req = inv.getArgument(0);
            setId(req, 101L);
            return req;
        });

        ReconciliationCorrectionResponse propRes = reconciliationService.proposeCorrection(9L, body, 10L);
        assertThat(propRes.getId()).isEqualTo(101L);
        assertThat(propRes.getStatus()).isEqualTo("PENDING_CONFIRMATION");

        // 3. Người lập (ID = 10) tự xác nhận đề nghị đính chính -> 409 QUY_TAC_HAI_NGUOI
        ReconciliationCorrectionRequest req =
                new ReconciliationCorrectionRequest(9L, "{}", "Sua nham", 10L, Instant.now());
        setId(req, 101L);
        when(correctionRepo.findById(101L)).thenReturn(Optional.of(req));
        when(correctionRepo.save(any())).thenReturn(req);

        assertThatThrownBy(() -> reconciliationService.confirmCorrection(101L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.errorCode()).isEqualTo("QUY_TAC_HAI_NGUOI");
                });

        // 4. Người khác (ID = 30) xác nhận -> Thành công
        ReconciliationCorrectionResponse confRes = reconciliationService.confirmCorrection(101L, 30L);
        assertThat(confRes.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confRes.getConfirmedById()).isEqualTo(30L);
    }

    @Test
    @DisplayName("T034: Cổng ReconciliationCheckPort báo true khi có phiếu mở hoặc chờ xác nhận (FR-037)")
    void testReconciliationCheckPort_HasOpenTicket() {
        when(reconciliationRepo.findOpenOrPendingByResidentId(100L))
                .thenReturn(Optional.of(new MedicationReconciliation(
                        100L, ReconciliationReason.ADMISSION, null, null, 1L, Instant.now())));

        boolean hasOpen = reconciliationService.hasOpenOrPendingReconciliation(100L);
        assertThat(hasOpen).isTrue();
    }
}
