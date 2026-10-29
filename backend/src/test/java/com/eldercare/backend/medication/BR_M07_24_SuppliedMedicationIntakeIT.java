package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.events.SuppliedMedicationIntakeFailed;
import com.eldercare.backend.medication.api.spi.ProcurementMedicationLineView;
import com.eldercare.backend.medication.internal.domain.FrequencyType;
import com.eldercare.backend.medication.internal.domain.Medication;
import com.eldercare.backend.medication.internal.domain.PrescriberType;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.domain.SuppliedMedicationIntake;
import com.eldercare.backend.medication.internal.domain.SuppliedMedicationIntake.IntakeResult;
import com.eldercare.backend.medication.internal.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.service.SuppliedMedicationIntakeService;
import com.eldercare.backend.medication.internal.service.SuppliedMedicationIntakeService.RecordIntake;
import com.eldercare.backend.medication.web.SuppliedMedicationIntakeController;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import com.eldercare.backend.support.fake.medication.FakeM07Config;
import com.eldercare.backend.support.fake.medication.FakeProcurementMedicationPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * 006 User Story 9 (FR-054, FR-055, BR-M07-24, BR-M11-17): lần tiếp nhận thuốc cung ứng gắn dòng yêu cầu đã ghi đã mua;
 * Không đạt bắt buộc sai lệch và lý do, báo người lập và Phụ trách bộ phận; không đổi đơn, liều.
 */
@Import(FakeM07Config.class)
@RecordApplicationEvents
class BR_M07_24_SuppliedMedicationIntakeIT extends M07IntegrationTestBase {

    @Autowired
    SuppliedMedicationIntakeService service;

    @Autowired
    FakeProcurementMedicationPort procurement;

    @Autowired
    PrescriptionRepository prescriptionRepository;

    @Autowired
    MedicationRepository medicationRepository;

    @Autowired
    ApplicationEvents events;

    @Autowired
    SuppliedMedicationIntakeController controller;

    @Autowired
    FakeStaffProfiles staffProfiles;

    long residentId;
    long lineId;
    long requestId;
    Prescription prescription;

    @BeforeEach
    void line() {
        residentId = TestUsers.nextId();
        lineId = TestUsers.nextId();
        requestId = TestUsers.nextId();
        Medication med = medicationRepository.save(new Medication(
                "Amlodipin " + TestUsers.nextId(), "5mg", "Viên nén", "Uống", "viên", 30, 30, false, Set.of(201L)));
        prescription = prescriptionRepository.save(new Prescription(
                residentId,
                med.getId(),
                new BigDecimal("1"),
                "viên",
                "Uống",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\"]",
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
                null,
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                10L,
                Instant.now().minusSeconds(86400),
                Set.of(201L),
                Instant.now()));
        procurement.recordLine(new ProcurementMedicationLineView(
                lineId,
                requestId,
                residentId,
                prescription.getId(),
                "Amlodipin",
                "Amlodipine",
                "5mg",
                "Viên nén",
                new BigDecimal("30"),
                "viên",
                777L,
                55L));
        user.set(nurse.user());
        assignNurse(nurse, residentId);
    }

    private RecordIntake intake(long line, IntakeResult result, String discrepancy, String reason) {
        return new RecordIntake(
                line,
                "5mg",
                "Viên nén",
                new BigDecimal("30"),
                "viên",
                LocalDate.now().plusYears(1),
                "Nguyên vỉ",
                result,
                discrepancy,
                reason,
                "Nhân viên mua A");
    }

    @Test
    void US9_1_passed_isSavedOnTheLine_prescriptionUnchanged_noNotification() {
        SuppliedMedicationIntake saved =
                service.record(service.targetOfLine(lineId), intake(lineId, IntakeResult.PASSED, null, null));

        assertThat(saved.getProcurementLineId()).isEqualTo(lineId);
        assertThat(saved.getMedicationId()).isEqualTo(prescription.getMedicationId());
        assertThat(saved.getReceiverId()).isEqualTo(nurse.accountId());
        assertThat(prescriptionRepository
                        .findById(prescription.getId())
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(PrescriptionStatus.ACTIVE);
        assertThat(events.stream(SuppliedMedicationIntakeFailed.class)).isEmpty();
        assertThat(service.byProcurementRequest(requestId))
                .singleElement()
                .satisfies(r -> assertThat(r.passed()).isTrue());
    }

    @Test
    void US9_2_notPassed_requiresDiscrepancyAndReason_thenNotifiesCreatorAndDepartmentLead() {
        assertThatThrownBy(() -> service.record(
                        service.targetOfLine(lineId), intake(lineId, IntakeResult.NOT_PASSED, "thiếu 10 viên", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).errorCode()).isEqualTo("INVALID_VALUE"));

        SuppliedMedicationIntake saved = service.record(
                service.targetOfLine(lineId),
                intake(lineId, IntakeResult.NOT_PASSED, "thiếu 10 viên", "Nhà cung cấp giao thiếu"));

        assertThat(events.stream(SuppliedMedicationIntakeFailed.class))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.intakeId()).isEqualTo(saved.getId());
                    assertThat(e.creatorStaffId()).isEqualTo(777L);
                    assertThat(e.departmentId()).isEqualTo(55L);
                    assertThat(e.discrepancy()).isEqualTo("thiếu 10 viên");
                });

        // giao bù: lần tiếp nhận mới cho cùng dòng
        service.record(service.targetOfLine(lineId), intake(lineId, IntakeResult.PASSED, null, null));
        assertThat(service.byProcurementRequest(requestId)).hasSize(2);
    }

    @Test
    void FR_054_list_showsResultAndReceiverName() {
        staffProfiles.name(nurse.staffId(), "Điều dưỡng Hoa");
        service.record(service.targetOfLine(lineId), intake(lineId, IntakeResult.PASSED, null, null));

        assertThat(controller.list(requestId)).singleElement().satisfies(r -> {
            assertThat(r.result()).isEqualTo("PASSED");
            assertThat(r.receiverName()).isEqualTo("Điều dưỡng Hoa");
        });
    }

    @Test
    void US9_3_lineNotPurchased_isBlocked() {
        long notPurchased = TestUsers.nextId();

        assertThatThrownBy(() -> service.targetOfLine(notPurchased))
                .isInstanceOf(ConditionNotMetException.class)
                .satisfies(e -> assertThat(((ConditionNotMetException) e).unmetConditions())
                        .extracting(c -> c.code())
                        .containsExactly("REQUEST_NOT_PURCHASED"));
    }

    @Test
    void appendix27_doctorOnlyViews() {
        user.set(doctor.user());

        assertThatThrownBy(() ->
                        service.record(service.targetOfLine(lineId), intake(lineId, IntakeResult.PASSED, null, null)))
                .isInstanceOf(PermissionDeniedException.class);
    }
}
