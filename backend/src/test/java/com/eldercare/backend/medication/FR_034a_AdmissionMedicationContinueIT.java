package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.prescription.domain.CreatePrescriptionBody;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.domain.CreateReconciliationBody;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationItemBody;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationItemResponse;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationResponse;
import com.eldercare.backend.medication.internal.reconciliation.service.MedicationReconciliationService;
import com.eldercare.backend.support.RealCareFixture;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * FR-034a, FR-036, User Story 4 kịch bản 10 (Q-57, BF-01 bước 12 → 15): dòng "thuốc đang dùng khi tiếp nhận" chọn
 * Tiếp tục ghi kèm đơn (theo đơn bệnh viện, có cơ sở kê); xác nhận phiếu lập trước khi hồ sơ còn Đang tiếp nhận tạo đơn
 * Chờ hiệu lực dấu "chờ tiếp nhận", chưa có liều. Dòng Tiếp tục như vậy mà thiếu đơn thì Gửi bị chặn MISSING_ITEMS.
 */
class FR_034a_AdmissionMedicationContinueIT extends M07IntegrationTestBase {

    @Autowired
    private MedicationReconciliationService reconciliations;

    @Autowired
    private PrescriptionRepository prescriptions;

    @Autowired
    private MedicationRepository medications;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private TestUser secondDoctor;
    private long residentId;
    private Medication amlodipine;

    @BeforeEach
    void admissionInProgress() {
        RealCareFixture fixture = new RealCareFixture(jdbc, time);
        residentId = fixture.resident("ADMISSION_IN_PROGRESS", "LONG_TERM_RESIDENTIAL", Instant.now());
        jdbc.update(
                "INSERT INTO resident_module_state (resident_id, module, data, version, updated_at)"
                        + " VALUES (?, 'M07', JSON_OBJECT(), 0, UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE version = version",
                residentId);
        amlodipine = medications.save(new Medication(
                "Amlodipin " + TestUsers.nextId(), "5mg", "Viên", "Uống", "viên", 30, 30, false, Set.of()));
        secondDoctor = new TestUsers(jdbc, time).staff(Role.DOCTOR);
        licensedToPrescribe(secondDoctor);
    }

    private ReconciliationItemBody continueLine(CreatePrescriptionBody prescription) {
        ReconciliationItemBody line = new ReconciliationItemBody();
        line.setSource("ADMISSION_CURRENT_MEDICATION");
        line.setMedicationDescription("Amlodipin 5mg, 1 viên sáng");
        line.setDecision("CONTINUE");
        line.setNewPrescription(prescription);
        return line;
    }

    private CreatePrescriptionBody hospitalPrescription() throws Exception {
        return json.readValue(
                """
                {"medicationId": %d, "dosage": 1, "route": "Uống", "prescriptionType": "PERIODIC",
                 "frequency": {"type": "TIMED_SLOTS", "timedSlots": ["08:00"]},
                 "startedAt": "%s", "medicationSource": "FACILITY",
                 "prescriber": {"type": "EXTERNAL", "facilityName": "Bệnh viện Thử", "prescriberName": "BS Thử",
                                "prescribedAt": "%s"}}
                """.formatted(amlodipine.getId(), Instant.now(), time.today().minusDays(10)),
                CreatePrescriptionBody.class);
    }

    private long openTicket(List<ReconciliationItemBody> lines) {
        CreateReconciliationBody body = new CreateReconciliationBody();
        body.setReason("ADMISSION");
        long id = reconciliations
                .create(residentId, body, doctor.accountId(), true)
                .getId();
        reconciliations.updateItems(id, null, lines, doctor.accountId());
        return id;
    }

    @Test
    @DisplayName("FR-034a: Tiếp tục thuốc đang dùng khi tiếp nhận kèm đơn: xác nhận tạo đơn Chờ hiệu lực chờ tiếp nhận")
    void confirmingAdmissionTicketCreatesPendingAdmissionPrescriptionFromContinueLine() throws Exception {
        long id = openTicket(List.of(continueLine(hospitalPrescription())));
        reconciliations.submit(id, doctor.accountId());

        user.set(secondDoctor.user());
        ReconciliationResponse confirmed = reconciliations.confirm(id, secondDoctor.accountId(), true);

        List<Prescription> created =
                prescriptions.findByResidentIdAndStatusIn(residentId, List.of(PrescriptionStatus.values()));
        assertThat(created).hasSize(1);
        Prescription p = created.getFirst();
        assertThat(p.getStatus()).isEqualTo(PrescriptionStatus.PENDING_EFFECTIVE);
        assertThat(p.isPendingAdmission()).isTrue();
        assertThat(confirmed.getItems())
                .extracting(ReconciliationItemResponse::getCreatedPrescriptionId)
                .containsExactly(p.getId());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM medication_dose WHERE prescription_id = ?", Integer.class, p.getId()))
                .isZero();
    }

    @Test
    @DisplayName("FR-034a: dòng Tiếp tục thuốc đang dùng khi tiếp nhận mà thiếu đơn thì Gửi bị chặn MISSING_ITEMS")
    void submittingContinueLineWithoutPrescriptionIsBlocked() {
        long id = openTicket(List.of(continueLine(null)));

        assertThatThrownBy(() -> reconciliations.submit(id, doctor.accountId()))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("MISSING_ITEMS"));
    }
}
