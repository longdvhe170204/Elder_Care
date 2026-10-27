package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m01.api.spi.StatusTransitionParticipant;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder.Kind;
import com.cambae.backend.m02.internal.registration.domain.ObligationTrigger;
import com.cambae.backend.m02.internal.registration.domain.RegistrationType;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.m02.internal.registration.domain.SubmissionChannel;
import com.cambae.backend.m02.internal.registration.persistence.RegistrationObligationStore;
import com.cambae.backend.m02.internal.registration.service.RegistrationDeregistration;
import com.cambae.backend.m02.internal.registration.service.RegistrationOverdueReminders;
import com.cambae.backend.m02.internal.registration.service.ResidenceRegistrationService;
import com.cambae.backend.m02.internal.registration.service.ResidenceRegistrationService.Task;
import com.cambae.backend.m02.internal.registration.service.ResidenceRegistrationService.TaskType;
import com.cambae.backend.m15.account.api.dto.Target;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 018 T013 ({@code BR_M02_13_KhaiBaoXoaIT}; US4 #1 → #4, quickstart Q18, Q19; FR-005, FR-008, BR-M02-13, SC-004; research
 * Z-05): lệnh Kết thúc lưu trú, Ghi nhận qua đời thật của 001 đi qua bean tham gia của 018 (không điều kiện chặn); khai báo
 * đang xử lý thành Đã hủy, nghĩa vụ đang mở hủy; người có khai báo Đã xác nhận hoặc Hết hạn được nhắc khai báo xóa ngay và
 * mỗi CFG-M02-09; Hành chính ghi đã khai báo xóa kèm bằng chứng khi hồ sơ ở trạng thái cuối. Nhắc hằng ngày gọi thẳng
 * service vì job chưa gắn (BL-90).
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_13_DeregistrationIT extends DeathTestBase {

    @Autowired
    ResidenceRegistrationService registrations;

    @Autowired
    RegistrationDeregistration deregistration;

    @Autowired
    RegistrationOverdueReminders overdueReminders;

    @Autowired
    RegistrationObligationStore obligationStore;

    @Autowired
    List<StatusTransitionParticipant> participants;

    @Autowired
    TransactionTemplate transaction;

    // ---- Dữ liệu ----------------------------------------------------------------------------------------------------

    private long evidence() {
        return files.upload(
                "bang-chung.pdf",
                FileTypes.PDF,
                "%PDF-1.4 bang chung".getBytes(StandardCharsets.UTF_8),
                office.accountId());
    }

    /** Nghĩa vụ khai báo (bảng kỹ thuật) như listener Hoàn tất tiếp nhận ghi; hạn {@code dueAt}. */
    private void obligation(long residentId, Instant dueAt) {
        long contractId =
                jdbc.queryForObject("SELECT MAX(id) FROM contract WHERE resident_id = ?", Long.class, residentId);
        transaction.executeWithoutResult(s -> obligationStore.record(
                residentId, ObligationTrigger.ADMISSION_COMPLETED, time.now(), dueAt, contractId));
    }

    /** Khai báo Cần khai báo do Hành chính lập (Q-306). */
    private ResidenceRegistration toSubmit(long residentId) {
        obligation(residentId, time.now().plus(Duration.ofDays(30)));
        user.set(office.user());
        return registrations.createRegistration(ResidenceRegistrationService.targetForResident(residentId), residentId);
    }

    private void submit(ResidenceRegistration registration) {
        user.set(office.user());
        registrations.recordSubmitted(
                registrations.targetOf(registration.id()),
                registration.id(),
                new ResidenceRegistrationService.Submission(
                        today(), SubmissionChannel.IN_PERSON, "Công an phường X", null));
    }

    /** Khai báo Đã xác nhận (Đăng ký tạm trú của người nội trú dài hạn). */
    private ResidenceRegistration confirmed(long residentId) {
        ResidenceRegistration registration = toSubmit(residentId);
        assertThat(registration.type()).isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        submit(registration);
        registrations.recordAccepted(
                registrations.targetOf(registration.id()),
                registration.id(),
                new ResidenceRegistrationService.Acceptance(evidence(), today().plusYears(1)));
        return registration;
    }

    private void deregister(long id, Long evidenceFileId) {
        user.set(office.user());
        registrations.recordDeregistered(registrations.targetOf(id), id, evidenceFileId);
    }

    private String evidenceBody(Long evidenceFileId) {
        return evidenceFileId == null ? "{}" : "{\"evidenceFileId\":" + evidenceFileId + "}";
    }

    private String url(long id) {
        return "/api/v1/residence-registrations/" + id + "/record-deregistered";
    }

    private String statusOf(long id) {
        return jdbc.queryForObject("SELECT status FROM residence_registration WHERE id = ?", String.class, id);
    }

    private int registrationCount(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM residence_registration WHERE resident_id = ?", Integer.class, residentId);
    }

    private int reminderCount(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM registration_reminder WHERE type = 'DEREGISTER' AND resident_id = ?",
                Integer.class,
                residentId);
    }

    private List<ResidenceRegistrationReminder> deregisterReminders(long residentId) {
        return events.stream(ResidenceRegistrationReminder.class)
                .filter(event -> event.residentId() == residentId && event.kind() == Kind.DEREGISTER)
                .toList();
    }

    private int auditCount(String action, long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = ? AND entity_type = 'M02.RESIDENCE_REGISTRATION'"
                        + " AND entity_id = ?",
                Integer.class,
                action,
                String.valueOf(id));
    }

    private List<Task> tasksOf(long residentId) {
        user.set(office.user());
        return registrations.toDo(Target.UNSPECIFIED).stream()
                .filter(task -> task.residentId() == residentId)
                .toList();
    }

    private StatusTransitionParticipant registrationParticipant() {
        return participants.stream()
                .filter(p -> p.getClass().getSimpleName().equals("RegistrationStatusParticipant"))
                .findFirst()
                .orElseThrow();
    }

    /** Kết thúc lưu trú thật (hồ sơ kết thúc, bàn giao, đồng hồ trưa {@code d0}); trả người cao tuổi. */
    private long endedWith(java.util.function.LongFunction<ResidenceRegistration> registration) throws Exception {
        ReadyEnding ready = readyToEnd(d0);
        registration.apply(ready.residentId());
        endResidency(ready.residentId(), ready.endingId(), time.now());
        assertThat(residentStatus(ready.residentId())).isEqualTo(ResidentStatus.RESIDENCY_ENDED);
        return ready.residentId();
    }

    // ---- SC-004: không chặn ---------------------------------------------------------------------------------------

    @Test
    void SC_004_participantHasNoBlockingCondition_forTheThreeFinalCommands() throws Exception {
        StatusTransitionParticipant participant = registrationParticipant();
        assertThat(participant.interestedCommands())
                .containsExactlyInAnyOrder(
                        StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH, StatusCommand.CANCEL_ADMISSION);
        long residentId = inResidence();
        toSubmit(residentId);
        for (StatusCommand command : participant.interestedCommands()) {
            assertThat(participant.checkConditions(new com.cambae.backend.m01.api.dto.CommandContext(
                            residentId,
                            ResidentStatus.IN_RESIDENCE,
                            command,
                            user,
                            time.now(),
                            JSON.createObjectNode())))
                    .as("khai báo đang xử lý không chặn %s", command)
                    .isEmpty();
        }
    }

    // ---- Q18: Kết thúc lưu trú khi có khai báo Đã xác nhận -------------------------------------------------------

    @Test
    void US4_1_endResidencyWithConfirmedRegistration_isNotBlocked_andOfficeIsRemindedToDeregister() throws Exception {
        List<ResidenceRegistration> holder = new ArrayList<>();
        long residentId = endedWith(id -> {
            holder.add(confirmed(id));
            return holder.getFirst();
        });
        long registrationId = holder.getFirst().id();

        assertThat(statusOf(registrationId))
                .as("khai báo giữ Đã xác nhận tới khi ghi xóa")
                .isEqualTo("CONFIRMED");
        assertThat(deregisterReminders(residentId))
                .as("nhắc ngay khi lệnh thành công")
                .hasSize(1);
        assertThat(reminderCount(residentId)).isEqualTo(1);
        assertThat(tasksOf(residentId))
                .extracting(Task::taskType, Task::registrationId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(TaskType.TO_DEREGISTER, registrationId));
    }

    @Test
    void US4_2_recordDeregistered_withEvidence_whileTheProfileIsFinal_movesToDeregistered_andStopsReminders()
            throws Exception {
        List<ResidenceRegistration> holder = new ArrayList<>();
        long residentId = endedWith(id -> {
            holder.add(confirmed(id));
            return holder.getFirst();
        });
        long id = holder.getFirst().id();
        int rows = registrationCount(residentId);
        long file = evidence();

        postAs(office, url(id), evidenceBody(file)).andExpect(status().isOk());

        assertThat(statusOf(id)).isEqualTo("DEREGISTERED");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT deregistered_at, deregistered_by_id, deregistration_evidence_file_id, evidence_file_id"
                        + " FROM residence_registration WHERE id = ?",
                id);
        assertThat(row.get("deregistered_at")).isNotNull();
        assertThat(((Number) row.get("deregistered_by_id")).longValue()).isEqualTo(office.accountId());
        assertThat(((Number) row.get("deregistration_evidence_file_id")).longValue())
                .isEqualTo(file);
        assertThat(row.get("evidence_file_id"))
                .as("bằng chứng chấp nhận giữ nguyên")
                .isNotNull();
        assertThat(auditCount("M02_REGISTRATION_DEREGISTERED", id)).isEqualTo(1);
        assertThat(registrationCount(residentId))
                .as("không xóa, không thêm dòng")
                .isEqualTo(rows);
        assertThat(tasksOf(residentId)).as("hết việc cần khai báo xóa").isEmpty();

        clockAt(d0.plusDays(1), 9, 0);
        deregistration.remind(d0.plusDays(1));
        assertThat(reminderCount(residentId)).as("nhắc dừng").isEqualTo(1);

        postAs(office, url(id), evidenceBody(evidence()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
    }

    @Test
    void US4_2_evidenceIsRequired() throws Exception {
        List<ResidenceRegistration> holder = new ArrayList<>();
        endedWith(id -> {
            holder.add(confirmed(id));
            return holder.getFirst();
        });
        long id = holder.getFirst().id();

        postAs(office, url(id), evidenceBody(null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EVIDENCE_REQUIRED"));

        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    @Test
    void FR_008_residentStillInResidence_cannotBeDeregistered() throws Exception {
        long residentId = inResidence();
        long id = confirmed(residentId).id();

        postAs(office, url(id), evidenceBody(evidence()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENCY_NOT_ENDED"));

        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
        assertThat(tasksOf(residentId)).extracting(Task::taskType).doesNotContain(TaskType.TO_DEREGISTER);
    }

    @Test
    void FR_009_managerCannotRecordDeregistration() throws Exception {
        List<ResidenceRegistration> holder = new ArrayList<>();
        endedWith(id -> {
            holder.add(confirmed(id));
            return holder.getFirst();
        });
        long id = holder.getFirst().id();
        long file = evidence();

        postAs(users.staff(com.cambae.backend.common.Role.NURSING_HOME_MANAGER), url(id), evidenceBody(file))
                .andExpect(status().isForbidden());

        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    @Test
    void FR_008_expiredRegistration_alsoNeedsDeregistration_andCanBeDeregistered() throws Exception {
        long residentId = inResidence();
        long id = confirmed(residentId).id();
        jdbc.update("UPDATE residence_registration SET status = 'EXPIRED' WHERE id = ?", id);
        clockAt(d0, 9, 0);

        recordDeath(doctor, residentId, deathDetails(time.now(), null, null));

        assertThat(deregisterReminders(residentId)).hasSize(1);
        assertThat(tasksOf(residentId))
                .extracting(Task::taskType, Task::registrationId)
                .contains(org.assertj.core.groups.Tuple.tuple(TaskType.TO_DEREGISTER, id));

        deregister(id, evidence());

        assertThat(statusOf(id)).isEqualTo("DEREGISTERED");
    }

    // ---- Q19: qua đời khi khai báo đang xử lý ----------------------------------------------------------------------

    @Test
    void US4_3_deathWhileSubmitted_cancelsTheRegistration_keepsItsSubmission_andDoesNotRemindToDeregister()
            throws Exception {
        long residentId = inResidence();
        ResidenceRegistration registration = toSubmit(residentId);
        submit(registration);
        int rows = registrationCount(residentId);
        clockAt(d0, 9, 0);

        recordDeath(doctor, residentId, deathDetails(time.now(), null, null));

        assertThat(statusOf(registration.id())).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM registration_submission WHERE registration_id = ?",
                        Integer.class,
                        registration.id()))
                .as("lần nộp giữ nguyên")
                .isEqualTo(1);
        assertThat(auditCount("M02_REGISTRATION_CANCELLED", registration.id())).isEqualTo(1);
        assertThat(registrationCount(residentId)).isEqualTo(rows);
        assertThat(deregisterReminders(residentId))
                .as("chưa có khai báo Đã xác nhận")
                .isEmpty();
        assertThat(reminderCount(residentId)).isZero();
        assertThat(tasksOf(residentId)).isEmpty();

        deregistration.remind(d0.plusDays(1));
        assertThat(reminderCount(residentId)).isZero();
        BusinessException e = failure(() -> deregister(registration.id(), evidence()));
        assertThat(e.errorCode()).isEqualTo("STATE_CHANGED");
    }

    @Test
    void FR_005_endResidencyWhileToSubmit_cancelsIt_andOpenObligationsStopOverdueReminders() throws Exception {
        List<ResidenceRegistration> holder = new ArrayList<>();
        long residentId = endedWith(id -> {
            holder.add(toSubmit(id));
            // Nghĩa vụ thứ hai đã quá hạn, chưa có khai báo (mốc khác)
            jdbc.update(
                    "INSERT INTO registration_obligation (resident_id, trigger_type, trigger_at, due_at, source_id)"
                            + " VALUES (?, 'RENEWAL_ADDENDUM', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) - INTERVAL 5 DAY, ?)",
                    id,
                    999_000_000L + id);
            return holder.getFirst();
        });

        assertThat(statusOf(holder.getFirst().id())).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM registration_obligation WHERE resident_id = ? AND cancelled_at IS NULL",
                        Integer.class,
                        residentId))
                .as("nghĩa vụ đang mở hủy, dòng giữ lại")
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM registration_obligation WHERE resident_id = ?",
                        Integer.class,
                        residentId))
                .isEqualTo(2);
        overdueReminders.remind(d0.plusDays(1));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM registration_reminder WHERE type = 'OVERDUE' AND resident_id = ?",
                        Integer.class,
                        residentId))
                .isZero();
        assertThat(tasksOf(residentId)).isEmpty();
        assertThat(failure(() -> toSubmit(residentId)).errorCode())
                .as("không lập khai báo mới sau trạng thái cuối: nghĩa vụ đã hủy")
                .isEqualTo("NO_REGISTRATION_OBLIGATION");
    }

    // ---- BL-97 A: không lập khai báo gia hạn khi hồ sơ ở trạng thái cuối (BR-M01-05) ------------------------------

    private int auditCountOfResident(String action, long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = ? AND resident_id = ?",
                Integer.class,
                action,
                residentId);
    }

    @Test
    void BR_M01_05_renewalAfterEndResidency_isRejectedWithStateChanged_andChangesNothing() throws Exception {
        List<ResidenceRegistration> holder = new ArrayList<>();
        long residentId = endedWith(id -> {
            holder.add(confirmed(id));
            return holder.getFirst();
        });
        int rows = registrationCount(residentId);
        int obligationRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registration_obligation WHERE resident_id = ?", Integer.class, residentId);
        int created = auditCountOfResident("M02_REGISTRATION_CREATED", residentId);
        int reminders = reminderCount(residentId);

        postAs(office, "/api/v1/residents/" + residentId + "/residence-registrations", "{\"renewal\":true}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));

        assertThat(registrationCount(residentId)).as("không tạo khai báo").isEqualTo(rows);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM registration_obligation WHERE resident_id = ?",
                        Integer.class,
                        residentId))
                .as("không phát sinh nghĩa vụ")
                .isEqualTo(obligationRows);
        assertThat(auditCountOfResident("M02_REGISTRATION_CREATED", residentId))
                .as("không ghi nhật ký lập")
                .isEqualTo(created);
        assertThat(statusOf(holder.getFirst().id())).isEqualTo("CONFIRMED");
        assertThat(reminderCount(residentId)).isEqualTo(reminders);
    }

    @Test
    void BR_M01_05_renewalAfterDeath_isRejectedWithStateChanged() throws Exception {
        long residentId = inResidence();
        long id = confirmed(residentId).id();
        int rows = registrationCount(residentId);
        clockAt(d0, 9, 0);
        recordDeath(doctor, residentId, deathDetails(time.now(), null, null));
        user.set(office.user());

        BusinessException e = failure(() -> registrations.createRenewalRegistration(
                ResidenceRegistrationService.targetForResident(residentId), residentId));

        assertThat(e.errorCode()).isEqualTo("STATE_CHANGED");
        assertThat(registrationCount(residentId)).isEqualTo(rows);
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    // ---- BL-97 B: khai báo Hết hạn cũ và khai báo mới Đã xác nhận cùng lúc (FR-008, bảng 6.10) ---------------------

    @Test
    void FR_008_expiredAndNewConfirmed_eachNeedsDeregistration_oneReminderADay_andEachIsRecordedSeparately()
            throws Exception {
        List<Long> ids = new ArrayList<>();
        long residentId = endedWith(id -> {
            ResidenceRegistration old = confirmed(id);
            jdbc.update("UPDATE residence_registration SET status = 'EXPIRED' WHERE id = ?", old.id());
            // Nghĩa vụ mốc Hết hạn (nguồn là khai báo cũ) rồi khai báo mới được chấp nhận; khai báo Hết hạn giữ nguyên
            transaction.executeWithoutResult(s -> obligationStore.record(
                    id, ObligationTrigger.EXPIRED, time.now(), time.now().plus(Duration.ofDays(30)), old.id()));
            user.set(office.user());
            ResidenceRegistration next =
                    registrations.createRegistration(ResidenceRegistrationService.targetForResident(id), id);
            submit(next);
            registrations.recordAccepted(
                    registrations.targetOf(next.id()),
                    next.id(),
                    new ResidenceRegistrationService.Acceptance(evidence(), today().plusYears(1)));
            ids.add(old.id());
            ids.add(next.id());
            return next;
        });
        long expiredId = ids.get(0);
        long confirmedId = ids.get(1);
        assertThat(statusOf(expiredId)).isEqualTo("EXPIRED");
        assertThat(statusOf(confirmedId)).isEqualTo("CONFIRMED");

        assertThat(tasksOf(residentId))
                .extracting(Task::taskType, Task::registrationId)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(TaskType.TO_DEREGISTER, expiredId),
                        org.assertj.core.groups.Tuple.tuple(TaskType.TO_DEREGISTER, confirmedId));
        assertThat(deregisterReminders(residentId))
                .as("một nhắc cho người, không nhắc trùng theo khai báo")
                .hasSize(1);

        deregister(confirmedId, evidence());

        assertThat(statusOf(confirmedId)).isEqualTo("DEREGISTERED");
        assertThat(statusOf(expiredId)).as("không tự đánh dấu khai báo kia").isEqualTo("EXPIRED");
        assertThat(tasksOf(residentId)).extracting(Task::registrationId).containsExactly(expiredId);
        clockAt(d0.plusDays(1), 6, 0);
        deregistration.remind(d0.plusDays(1));
        assertThat(reminderCount(residentId))
                .as("còn khai báo Hết hạn nên vẫn nhắc, một lần mỗi ngày")
                .isEqualTo(2);

        deregister(expiredId, evidence());

        assertThat(statusOf(expiredId)).isEqualTo("DEREGISTERED");
        assertThat(tasksOf(residentId)).isEmpty();
        clockAt(d0.plusDays(2), 6, 0);
        deregistration.remind(d0.plusDays(2));
        assertThat(reminderCount(residentId))
                .as("hết khai báo cần xóa thì dừng nhắc")
                .isEqualTo(2);
        assertThat(registrationCount(residentId)).isEqualTo(2);
    }

    // ---- US4 #4: nhắc mỗi CFG-M02-09 --------------------------------------------------------------------------------

    @Test
    void US4_4_officeIsRemindedEachDay_onceADay_untilDeregistered() throws Exception {
        List<ResidenceRegistration> holder = new ArrayList<>();
        long residentId = endedWith(id -> {
            holder.add(confirmed(id));
            return holder.getFirst();
        });

        // Đếm theo người: dữ liệu của test khác cùng schema cũng có người cần khai báo xóa
        deregistration.remind(d0);
        assertThat(reminderCount(residentId))
                .as("ngày kết thúc đã nhắc kèm lệnh")
                .isEqualTo(1);
        clockAt(d0.plusDays(1), 6, 0);
        deregistration.remind(d0.plusDays(1));
        assertThat(reminderCount(residentId)).isEqualTo(2);
        deregistration.remind(d0.plusDays(1));
        assertThat(reminderCount(residentId)).as("chạy lại không nhắc trùng").isEqualTo(2);
        clockAt(d0.plusDays(2), 6, 0);
        deregistration.remind(d0.plusDays(2));
        assertThat(reminderCount(residentId)).isEqualTo(3);
        assertThat(deregisterReminders(residentId)).hasSize(3);
    }

    // ---- Phát lại, đồng thời ----------------------------------------------------------------------------------------

    @Test
    void replayingTheEffect_doesNotDuplicateCancellationsRemindersOrAudit() throws Exception {
        long withConfirmed = inResidence();
        long confirmedId = confirmed(withConfirmed).id();
        long withSubmitted = inResidence();
        ResidenceRegistration submitted = toSubmit(withSubmitted);
        submit(submitted);
        clockAt(d0, 9, 0);
        recordDeath(doctor, withConfirmed, deathDetails(time.now(), null, null));
        recordDeath(doctor, withSubmitted, deathDetails(time.now(), null, null));

        StatusTransitionParticipant participant = registrationParticipant();
        user.set(office.user());
        for (long residentId : List.of(withConfirmed, withSubmitted)) {
            transaction.executeWithoutResult(s -> participant.applyEffects(new TransitionResult(
                    residentId,
                    ResidentStatus.IN_RESIDENCE,
                    ResidentStatus.DECEASED,
                    StatusCommand.RECORD_DEATH,
                    0L,
                    time.now(),
                    JSON.createObjectNode())));
        }

        assertThat(statusOf(confirmedId)).isEqualTo("CONFIRMED");
        assertThat(reminderCount(withConfirmed)).isEqualTo(1);
        assertThat(deregisterReminders(withConfirmed)).hasSize(1);
        assertThat(statusOf(submitted.id())).isEqualTo("CANCELLED");
        assertThat(auditCount("M02_REGISTRATION_CANCELLED", submitted.id())).isEqualTo(1);
    }

    @Test
    void concurrency_twoSimultaneousDeregistrations_recordOne() throws Exception {
        List<ResidenceRegistration> holder = new ArrayList<>();
        endedWith(id -> {
            holder.add(confirmed(id));
            return holder.getFirst();
        });
        long id = holder.getFirst().id();
        long first = evidence();
        long second = evidence();
        user.set(office.user());
        Target target = registrations.targetOf(id);

        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<String> outcomes = new ArrayList<>();
        try {
            List<Future<String>> results = new ArrayList<>();
            for (long file : List.of(first, second)) {
                results.add(pool.submit(() -> {
                    start.await();
                    BusinessException e = failure(() -> registrations.recordDeregistered(target, id, file));
                    return e == null ? "OK" : e.errorCode();
                }));
            }
            for (Future<String> result : results) {
                outcomes.add(result.get());
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(outcomes).containsExactlyInAnyOrder("OK", "STATE_CHANGED");
        assertThat(statusOf(id)).isEqualTo("DEREGISTERED");
        assertThat(auditCount("M02_REGISTRATION_DEREGISTERED", id)).isEqualTo(1);
    }

    @Test
    void concurrency_submissionRacingTheFinalStatusEffect_neverLeavesAnInProgressRegistration() throws Exception {
        long residentId = inResidence();
        ResidenceRegistration registration = toSubmit(residentId);
        ResidenceRegistrationService.Submission submission = new ResidenceRegistrationService.Submission(
                today(), SubmissionChannel.IN_PERSON, "Công an phường X", null);
        user.set(office.user());
        Target target = registrations.targetOf(registration.id());
        StatusTransitionParticipant participant = registrationParticipant();
        // Luồng hai là tác động của lệnh Ghi nhận qua đời trong transaction riêng (người dùng của test dùng chung giữa
        // hai luồng, mà qua đời tại viện phải do Bác sĩ xác nhận)
        TransitionResult death = new TransitionResult(
                residentId,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.DECEASED,
                StatusCommand.RECORD_DEATH,
                0L,
                time.now(),
                JSON.createObjectNode());

        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<BusinessException> submitted = pool.submit(() -> {
                start.await();
                return failure(() -> registrations.recordSubmitted(target, registration.id(), submission));
            });
            Future<BusinessException> effect = pool.submit(() -> {
                start.await();
                return failure(() -> transaction.executeWithoutResult(s -> participant.applyEffects(death)));
            });
            assertThat(effect.get()).as("tác động không bị chặn (SC-004)").isNull();
            BusinessException submitFailure = submitted.get();
            if (submitFailure != null) {
                assertThat(submitFailure.errorCode()).isEqualTo("STATE_CHANGED");
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(statusOf(registration.id())).isEqualTo("CANCELLED");
        assertThat(auditCount("M02_REGISTRATION_CANCELLED", registration.id())).isEqualTo(1);
    }

    @Test
    void contract_unknownCommandStill422_butRecordDeregisteredIsImplemented() throws Exception {
        long residentId = inResidence();
        long id = confirmed(residentId).id();

        postAs(office, "/api/v1/residence-registrations/" + id + "/no-such-command", "{}")
                .andExpect(status().isUnprocessableContent());
        postAs(office, url(id), evidenceBody(evidence()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENCY_NOT_ENDED"));
    }
}
