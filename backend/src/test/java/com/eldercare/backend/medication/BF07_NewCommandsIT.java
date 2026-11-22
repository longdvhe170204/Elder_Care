package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.medication.api.dto.AdministerDoseRequest;
import com.eldercare.backend.medication.api.dto.DoseMeasurement;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.dto.NursingShiftView;
import com.eldercare.backend.medication.api.events.PrnEffectivenessReminder;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.dose.persistence.TemporaryFloorNurseAssignmentRepository;
import com.eldercare.backend.medication.internal.dose.service.DoseAdministrationService;
import com.eldercare.backend.medication.internal.dose.service.DoseWorkflowService;
import com.eldercare.backend.medication.internal.dose.service.PrnEffectivenessService;
import com.eldercare.backend.medication.internal.dose.service.TemporaryNurseAssignmentService;
import com.eldercare.backend.medication.internal.family.domain.ConfirmBatchUseBody;
import com.eldercare.backend.medication.internal.family.domain.DisposeBatchBody;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatchResponse;
import com.eldercare.backend.medication.internal.family.domain.ReceiveBatchBody;
import com.eldercare.backend.medication.internal.family.service.FamilyMedicationService;
import com.eldercare.backend.medication.internal.prescription.domain.ExternalBasisBody;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyType;
import com.eldercare.backend.medication.internal.prescription.domain.OrderPrescriptionBody;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionResponse;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.domain.SuspensionReason;
import com.eldercare.backend.medication.internal.prescription.domain.TimeCriticality;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.prescription.service.PrescriptionService;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.medication.FakeM07Config;
import com.eldercare.backend.support.fake.medication.FakeNursingShifts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Sửa BF-07 (Q-427 → Q-437): các lệnh mới chạy trên MySQL thật — Nhận liều, Hoãn, chỉ số trước khi dùng, Tiếp tục tạm,
 * hiệu quả và nhắc PRN, Điều dưỡng phụ trách tạm, kiểm tra tiếp nhận và Hủy do hết hạn của thuốc gia đình gửi.
 */
@Import(FakeM07Config.class)
@RecordApplicationEvents
class BF07_NewCommandsIT extends M07IntegrationTestBase {

    @Autowired
    PrescriptionService prescriptionService;

    @Autowired
    PrescriptionRepository prescriptionRepository;

    @Autowired
    MedicationRepository medicationRepository;

    @Autowired
    MedicationDoseRepository doseRepository;

    @Autowired
    DoseResultRepository doseResultRepository;

    @Autowired
    DoseAdministrationService administration;

    @Autowired
    DoseWorkflowService workflow;

    @Autowired
    PrnEffectivenessService prnEffectiveness;

    @Autowired
    TemporaryFloorNurseAssignmentRepository assignmentRepository;

    @Autowired
    FamilyMedicationService familyMedication;

    @Autowired
    ApplicationEvents events;

    private final long residentId = TestUsers.nextId();
    private Long medicationId;
    private Instant now;

    @BeforeEach
    void setUp() {
        jdbc.update(
                "INSERT INTO resident_module_state (resident_id, module, data, version, updated_at)"
                        + " VALUES (?, 'M07', JSON_OBJECT(), 0, UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE version = version",
                residentId);
        Medication medication = new Medication(
                "Insulin " + TestUsers.nextId(), "100 IU/ml", "Tiêm", "Injection", "IU", 30, 30, false, Set.of(103L));
        medicationRepository.save(medication);
        medicationId = medication.getId();
        now = time.now();
    }

    private Prescription prescription(
            PrescriptionType type, TimeCriticality criticality, String measurementsJson, PrescriberType prescriber) {
        Prescription p = new Prescription(
                residentId,
                medicationId,
                new BigDecimal("10"),
                "IU",
                "Injection",
                type,
                type == PrescriptionType.PERIODIC ? FrequencyType.TIMED_SLOTS : null,
                type == PrescriptionType.PERIODIC ? "[\"08:00\"]" : null,
                null,
                null,
                null,
                type == PrescriptionType.PRN ? 60 : null,
                type == PrescriptionType.PRN ? 3 : null,
                now.minus(1, ChronoUnit.DAYS),
                null,
                MedicationSource.FACILITY,
                prescriber,
                prescriber == PrescriberType.EXTERNAL ? "Bệnh viện A" : null,
                prescriber == PrescriberType.EXTERNAL ? "BS Hùng" : null,
                10L,
                prescriber == PrescriberType.EXTERNAL ? LocalDate.now().minusDays(1) : null,
                null,
                "Trước ăn",
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                10L,
                now.minus(1, ChronoUnit.DAYS),
                Set.of(103L),
                now);
        p.applyBf07(criticality, measurementsJson, type == PrescriptionType.PRN ? "đau" : null);
        return prescriptionRepository.save(p);
    }

    private MedicationDose openDose(Prescription prescription, Instant scheduled) {
        MedicationDose dose = new MedicationDose(
                prescription.getId(),
                residentId,
                medicationId,
                scheduled,
                scheduled.minus(30, ChronoUnit.MINUTES),
                scheduled.plus(30, ChronoUnit.MINUTES),
                new BigDecimal("10"),
                "IU",
                MedicationSource.FACILITY,
                false,
                DoseStatus.DUE,
                1L,
                now);
        dose.markDue(now);
        return doseRepository.save(dose);
    }

    private static AdministerDoseRequest administer(Instant at, List<DoseMeasurement> measurements) {
        return new AdministerDoseRequest(at, null, null, null, null, null, null, null, false, null, measurements);
    }

    @Test
    void claim_blocksOtherNurseUntilReleased_thenAdministerClearsClaim() {
        Prescription p = prescription(PrescriptionType.PERIODIC, TimeCriticality.NORMAL, null, PrescriberType.INTERNAL);
        Instant scheduled = now.minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        MedicationDose dose = openDose(p, scheduled);
        String key = MedicationScheduleCalculator.doseKey(p.getId(), scheduled);
        long nurseA = TestUsers.nextId();
        long nurseB = TestUsers.nextId();

        workflow.claim(key, nurseA, now);
        BusinessException blocked = catchThrowableOfType(
                BusinessException.class,
                () -> administration.administerDose(key, administer(now, null), nurseB, null, now));
        assertThat(blocked.errorCode()).isEqualTo("DOSE_CLAIMED");
        assertThat(blocked.status()).isEqualTo(HttpStatus.CONFLICT);

        // Người khác cũng không nhận được khi lượt nhận còn hạn
        assertThat(catchThrowableOfType(BusinessException.class, () -> workflow.claim(key, nurseB, now))
                        .errorCode())
                .isEqualTo("DOSE_CLAIMED");

        // Hết hạn (CFG-M07-08, 15 phút) thì người khác ghi được
        administration.administerDose(key, administer(now, null), nurseB, null, now.plus(20, ChronoUnit.MINUTES));
        MedicationDose closed = doseRepository.findById(dose.getId()).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(DoseStatus.ADMINISTERED);
        assertThat(closed.getClaimedById()).isNull();
    }

    @Test
    void postpone_requiresReason_keepsStatusAndAllowsLaterAdminister() {
        Prescription p = prescription(PrescriptionType.PERIODIC, TimeCriticality.NORMAL, null, PrescriberType.INTERNAL);
        Instant scheduled = now.minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        MedicationDose dose = openDose(p, scheduled);
        String key = MedicationScheduleCalculator.doseKey(p.getId(), scheduled);
        long nurse = TestUsers.nextId();

        assertThat(catchThrowableOfType(ConditionNotMetException.class, () -> workflow.postpone(key, " ", nurse, now)))
                .isNotNull();

        workflow.postpone(key, "Đang ăn chưa xong", nurse, now);
        MedicationDose postponed = doseRepository.findById(dose.getId()).orElseThrow();
        assertThat(postponed.getStatus()).isEqualTo(DoseStatus.DUE);
        assertThat(postponed.getPostponedAt()).isNotNull();
        assertThat(postponed.getPostponedReason()).isEqualTo("Đang ăn chưa xong");

        administration.administerDose(key, administer(now, null), nurse, null, now);
        assertThat(doseRepository.findById(dose.getId()).orElseThrow().getStatus())
                .isEqualTo(DoseStatus.ADMINISTERED);
    }

    @Test
    void requiredMeasurement_blocksAdministerUntilGiven_andIsStoredWithResult() {
        Prescription p = prescription(
                PrescriptionType.PERIODIC, TimeCriticality.CRITICAL, "[\"BLOOD_GLUCOSE\"]", PrescriberType.INTERNAL);
        Instant scheduled = now.minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        MedicationDose dose = openDose(p, scheduled);
        String key = MedicationScheduleCalculator.doseKey(p.getId(), scheduled);
        long nurse = TestUsers.nextId();

        assertThat(catchThrowableOfType(
                        ConditionNotMetException.class,
                        () -> administration.administerDose(key, administer(now, null), nurse, null, now)))
                .isNotNull();
        assertThat(doseRepository.findById(dose.getId()).orElseThrow().getStatus())
                .isEqualTo(DoseStatus.DUE);

        administration.administerDose(
                key,
                administer(now, List.of(new DoseMeasurement("BLOOD_GLUCOSE", new BigDecimal("6.4")))),
                nurse,
                null,
                now);
        String stored = jdbc.queryForObject(
                "SELECT pre_dose_measurements FROM dose_result WHERE dose_id = ?", String.class, dose.getId());
        assertThat(stored).contains("BLOOD_GLUCOSE").contains("6.4");
    }

    @Test
    void resumeTemporarily_onlyCriticalWaitingReconciliation_withDischargePaper() {
        Prescription critical =
                prescription(PrescriptionType.PERIODIC, TimeCriticality.CRITICAL, null, PrescriberType.INTERNAL);
        critical.suspend(SuspensionReason.RECONCILIATION_PENDING);
        prescriptionRepository.save(critical);

        OrderPrescriptionBody wrongBasis = order("PHONE_ORDER");
        assertThat(catchThrowableOfType(
                        ConditionNotMetException.class,
                        () -> prescriptionService.resumeTemporarily(
                                prescriptionService.targetOf(critical.getId()),
                                critical.getId(),
                                wrongBasis,
                                doctor.user().accountId(),
                                true)))
                .isNotNull();

        PrescriptionResponse resumed = prescriptionService.resumeTemporarily(
                prescriptionService.targetOf(critical.getId()),
                critical.getId(),
                order("DISCHARGE_PAPER"),
                doctor.user().accountId(),
                true);
        assertThat(resumed.status()).isEqualTo("ACTIVE");
        assertThat(resumed.tempResumedAt()).isNotNull();
        assertThat(resumed.tempResumeBasis()).contains("Bệnh viện Đa khoa");

        Prescription normal =
                prescription(PrescriptionType.PERIODIC, TimeCriticality.NORMAL, null, PrescriberType.INTERNAL);
        normal.suspend(SuspensionReason.RECONCILIATION_PENDING);
        prescriptionRepository.save(normal);
        assertThat(catchThrowableOfType(
                        ConditionNotMetException.class,
                        () -> prescriptionService.resumeTemporarily(
                                prescriptionService.targetOf(normal.getId()),
                                normal.getId(),
                                order("DISCHARGE_PAPER"),
                                doctor.user().accountId(),
                                true)))
                .isNotNull();
    }

    private static OrderPrescriptionBody order(String basisType) {
        ExternalBasisBody basis = new ExternalBasisBody();
        basis.setFacilityName("Bệnh viện Đa khoa");
        basis.setPrescriberName("BS Lan");
        basis.setPrescribedDate(LocalDate.now().minusDays(1));
        basis.setBasisType(basisType);
        OrderPrescriptionBody body = new OrderPrescriptionBody();
        body.setReason("Về từ bệnh viện, thuốc không thể trễ");
        body.setExternalBasis(basis);
        return body;
    }

    @Test
    void prnEffectiveness_recordedOnce_andReminderSentOnceAfter60Minutes() {
        Prescription p = prescription(PrescriptionType.PRN, TimeCriticality.NORMAL, null, PrescriberType.INTERNAL);
        Instant used = now.minus(2, ChronoUnit.HOURS);
        jdbc.update(
                "INSERT INTO dose_result (kind, prescription_id, resident_id, result_type, administered_at, recorded_at,"
                        + " recorder_id, dosage, dosage_unit, exceeded_limit, reason) VALUES ('PRN', ?, ?, 'ADMINISTERED',"
                        + " ?, ?, ?, 1, 'IU', FALSE, 'đau')",
                p.getId(),
                residentId,
                java.sql.Timestamp.from(used),
                java.sql.Timestamp.from(used),
                nurse.user().accountId());
        Long prnId = jdbc.queryForObject(
                "SELECT id FROM dose_result WHERE kind = 'PRN' AND prescription_id = ?", Long.class, p.getId());

        prnEffectiveness.remind(now);
        prnEffectiveness.remind(now.plus(5, ChronoUnit.MINUTES));
        assertThat(events.stream(PrnEffectivenessReminder.class)
                        .filter(e -> e.prnAdministrationId() == prnId)
                        .count())
                .isEqualTo(1);

        prnEffectiveness.record(
                p.getId(), prnId, "Hết đau sau 30 phút", nurse.user().accountId());
        assertThat(catchThrowableOfType(
                                BusinessException.class,
                                () -> prnEffectiveness.record(
                                        p.getId(),
                                        prnId,
                                        "lần hai",
                                        nurse.user().accountId()))
                        .errorCode())
                .isEqualTo("EFFECTIVENESS_ALREADY_RECORDED");
        assertThat(prnEffectiveness.byAdministration(List.of(prnId))).containsKey(prnId);
    }

    @Test
    void temporaryNurse_onlyWhenFloorHasNoNurse_andNurseIsOnShiftElsewhere() {
        FakeNursingShifts shifts = new FakeNursingShifts();
        long floor = TestUsers.nextId();
        long otherFloor = TestUsers.nextId();
        long lender = TestUsers.nextId();
        shifts.shift(
                new NursingShiftView(TestUsers.nextId(), floor, now.minusSeconds(3600), now.plusSeconds(3600)),
                List.of());
        shifts.shift(
                new NursingShiftView(TestUsers.nextId(), otherFloor, now.minusSeconds(3600), now.plusSeconds(7200)),
                List.of(lender));
        TemporaryNurseAssignmentService service = new TemporaryNurseAssignmentService(assignmentRepository, shifts);
        long manager = TestUsers.nextId();

        var assigned = service.assign(floor, lender, manager, now);
        assertThat(assigned.getExpiresAt()).isEqualTo(now.plusSeconds(3600));
        assertThat(service.hasActive(floor, now)).isTrue();
        assertThat(service.hasActive(floor, now.plusSeconds(3601))).isFalse();

        assertThat(catchThrowableOfType(
                                BusinessException.class, () -> service.assign(floor, TestUsers.nextId(), manager, now))
                        .errorCode())
                .isEqualTo("NURSE_NOT_ON_SHIFT");
        assertThat(catchThrowableOfType(BusinessException.class, () -> service.assign(otherFloor, lender, manager, now))
                        .errorCode())
                .isEqualTo("FLOOR_HAS_NURSE");
    }

    private ReceiveBatchBody batch(boolean nameOk, LocalDate expiry) {
        ReceiveBatchBody body = new ReceiveBatchBody();
        body.setCustomName("Paracetamol gia đình");
        body.setCustomConcentration("500 mg");
        body.setInitialQuantity(new BigDecimal("20"));
        body.setUnit("viên");
        body.setExpiryDate(expiry);
        body.setPackagingCondition("Còn nguyên");
        body.setDelivererType("FAMILY_MEMBER");
        body.setDelivererName("Con gái");
        body.setLabelOk(true);
        body.setExpiryOk(true);
        body.setPackagingOk(true);
        body.setNameOk(nameOk);
        return body;
    }

    @Test
    void familyMedication_failedIntakeCheckBlocksUse_andExpiredDisposeNeedsPastExpiry() {
        long actor = nurse.user().accountId();
        FamilyMedicationBatchResponse failed = familyMedication.receiveBatch(
                residentId, batch(false, LocalDate.now().plusMonths(6)), actor);
        assertThat(failed.getNameOk()).isFalse();

        ConfirmBatchUseBody use = new ConfirmBatchUseBody();
        use.setPrescriptionIds(List.of(1L));
        use.setPackagingRecognizable(true);
        assertThat(catchThrowableOfType(
                                BusinessException.class, () -> familyMedication.confirmUse(failed.getId(), use, actor))
                        .errorCode())
                .isEqualTo("INTAKE_CHECK_FAILED");

        DisposeBatchBody dispose = new DisposeBatchBody();
        dispose.setKind("EXPIRED");
        dispose.setQuantity(new BigDecimal("20"));
        dispose.setWitness("Điều dưỡng Mai");
        dispose.setReason("Thuốc hết hạn dùng");
        assertThat(catchThrowableOfType(
                                BusinessException.class,
                                () -> familyMedication.disposeBatch(failed.getId(), dispose, actor))
                        .errorCode())
                .isEqualTo("NOT_EXPIRED");

        FamilyMedicationBatchResponse expired = familyMedication.receiveBatch(
                residentId, batch(true, LocalDate.now().minusDays(2)), actor);
        familyMedication.disposeBatch(expired.getId(), dispose, actor);
        FamilyMedicationBatchResponse after = familyMedication.getBatch(expired.getId());
        assertThat(after.getStatus()).isEqualTo("DISPOSED");
        assertThat(after.getDisposeKind()).isEqualTo("EXPIRED");
        assertThat(after.getWitness()).isEqualTo("Điều dưỡng Mai");
    }
}
