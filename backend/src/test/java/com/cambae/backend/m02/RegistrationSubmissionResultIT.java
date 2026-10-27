package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder.Kind;
import com.cambae.backend.m02.internal.registration.domain.RegistrationStatus;
import com.cambae.backend.m02.internal.registration.domain.RegistrationType;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.m02.internal.registration.domain.SubmissionChannel;
import com.cambae.backend.m02.internal.registration.service.ResidenceRegistrationService;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 018 T009 ({@code KhaiBaoNopKetQuaIT}; US2, quickstart Q7 → Q10; FR-002 → FR-005, FR-009; DBR-33; research Z-04): Hành
 * chính ghi đã nộp (dừng nhắc quá hạn), ghi được chấp nhận (bằng chứng; Đăng ký tạm trú có ngày hết hạn; khai báo Đã xác
 * nhận cũ Được thay thế) hoặc bị từ chối (về Cần khai báo, hạn mới ở khai báo = lúc ghi + CFG-M02-12, hạn gốc của nghĩa vụ
 * giữ nguyên, nhắc quá hạn theo hạn mới, Quản lý viện tối đa một lần mỗi nghĩa vụ); lịch sử lần nộp chỉ thêm; kết quả đặt
 * một lần; đính chính {@code M02.RESIDENCE_REGISTRATION}.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(FakeFileConfig.class)
class RegistrationSubmissionResultIT extends RegistrationTestBase {

    private static final String CORRECTIONS = "/api/v1/corrections/M02.RESIDENCE_REGISTRATION/";

    @Autowired
    MockMvc mvc;

    @Autowired
    TestClockControl clock;

    @Autowired
    FileApi files;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    // ---- Dựng dữ liệu -------------------------------------------------------------------------------------------

    /** Người nội trú dài hạn đã Hoàn tất tiếp nhận, có nghĩa vụ và khai báo Đăng ký tạm trú ở Cần khai báo. */
    private ResidenceRegistration created() {
        return createdFor(ResidenceType.LONG_TERM_RESIDENTIAL, null);
    }

    private ResidenceRegistration createdFor(ResidenceType type, LocalDate end) {
        long residentId = readyWith(type, end);
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        return create(residentId);
    }

    private ResidenceRegistration create(long residentId) {
        user.set(office.user());
        return registrations.createRegistration(ResidenceRegistrationService.targetForResident(residentId), residentId);
    }

    private ResidenceRegistration reload(ResidenceRegistration registration) {
        user.set(office.user());
        return registrations
                .ofResident(
                        ResidenceRegistrationService.targetForResident(registration.residentId()),
                        registration.residentId())
                .stream()
                .filter(r -> r.id() == registration.id())
                .findFirst()
                .orElseThrow();
    }

    private long evidence() {
        return files.upload(
                "ket-qua.pdf",
                FileTypes.PDF,
                "%PDF-1.4 ket qua khai bao".getBytes(StandardCharsets.UTF_8),
                office.accountId());
    }

    private ResultActions command(TestUser who, long id, String command, String body) throws Exception {
        return command(who, id, command, body, UUID.randomUUID());
    }

    private ResultActions command(TestUser who, long id, String command, String body, UUID key) throws Exception {
        user.set(who.user());
        return mvc.perform(post("/api/v1/residence-registrations/" + id + "/" + command)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String submittedBody(LocalDate date) {
        return """
                {"submittedDate":"%s","channel":"PUBLIC_SERVICE_PORTAL","receivingAuthority":"Công an phường X",\
                "dossierCode":"G22.%s"}""".formatted(date, UUID.randomUUID().toString().substring(0, 8));
    }

    private static String acceptedBody(Long evidenceFileId, LocalDate expiryDate) {
        StringBuilder body = new StringBuilder("{");
        if (evidenceFileId != null) {
            body.append("\"evidenceFileId\":").append(evidenceFileId);
        }
        if (expiryDate != null) {
            body.append(evidenceFileId != null ? "," : "")
                    .append("\"expiryDate\":\"")
                    .append(expiryDate)
                    .append('"');
        }
        return body.append('}').toString();
    }

    private void submit(ResidenceRegistration registration) throws Exception {
        command(office, registration.id(), "record-submitted", submittedBody(today()))
                .andExpect(status().isOk());
    }

    private void reject(ResidenceRegistration registration, String reason) throws Exception {
        command(office, registration.id(), "record-rejected", "{\"reason\":\"" + reason + "\"}")
                .andExpect(status().isOk());
    }

    private void accept(ResidenceRegistration registration) throws Exception {
        command(office, registration.id(), "record-accepted", acceptedBody(evidence(), today().plusYears(1)))
                .andExpect(status().isOk());
    }

    private List<Map<String, Object>> submissions(long registrationId) {
        return jdbc.queryForList(
                "SELECT * FROM registration_submission WHERE registration_id = ? ORDER BY id", registrationId);
    }

    private Map<String, Object> row(long registrationId) {
        return jdbc.queryForMap("SELECT * FROM residence_registration WHERE id = ?", registrationId);
    }

    private List<ResidenceRegistrationReminder> reminders(long residentId) {
        return events.stream(ResidenceRegistrationReminder.class)
                .filter(event -> event.residentId() == residentId)
                .toList();
    }

    private static Instant utc(Object dateTime) {
        return ((LocalDateTime) dateTime).toInstant(ZoneOffset.UTC);
    }

    private void clockAt(LocalDate day, int hour) {
        clock.setTo(day.atTime(hour, 0)
                .atZone(com.cambae.backend.common.time.BusinessTime.ZONE)
                .toInstant());
    }

    // ---- Q7: ghi đã nộp, ghi được chấp nhận ------------------------------------------------------------------------

    @Test
    void Q7_recordingSubmitted_movesToSubmitted_keepsTheSubmission_andStopsOverdueReminders() throws Exception {
        ResidenceRegistration registration = created();
        LocalDate due = dateOf(obligations(registration.residentId()).getFirst().get("due_at"));

        command(office, registration.id(), "record-submitted", submittedBody(today()))
                .andExpect(status().isOk());

        assertThat(reload(registration).status()).isEqualTo(RegistrationStatus.SUBMITTED);
        List<Map<String, Object>> rows = submissions(registration.id());
        assertThat(rows).hasSize(1);
        Map<String, Object> submission = rows.getFirst();
        assertThat(submission.get("result")).isEqualTo("PENDING");
        assertThat(submission.get("channel")).isEqualTo(SubmissionChannel.PUBLIC_SERVICE_PORTAL.name());
        assertThat(submission.get("receiving_authority")).isEqualTo("Công an phường X");
        assertThat(((Number) submission.get("recorded_by")).longValue()).isEqualTo(office.accountId());
        assertThat(row(registration.id()).get("current_submission_id")).isEqualTo(submission.get("id"));
        assertThat(obligations(registration.residentId()).getFirst().get("submitted"))
                .isEqualTo(true);

        overdueReminders.remind(due.plusDays(1));
        assertThat(reminders(registration.residentId()))
                .as("đã ghi nộp thì dừng nhắc quá hạn (US2 #1)")
                .isEmpty();
        mvc.perform(get("/api/v1/residence-registrations/to-do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.residentId == %d)]".formatted(registration.residentId()))
                        .isEmpty());
    }

    @Test
    void Q7_recordingSubmitted_withoutDateChannelOrAuthority_isRejected() {
        ResidenceRegistration registration = created();
        user.set(office.user());

        assertThat(failure(() -> registrations.recordSubmitted(
                        registrations.targetOf(registration.id()),
                        registration.id(),
                        new ResidenceRegistrationService.Submission(today(), SubmissionChannel.IN_PERSON, " ", null))))
                .isInstanceOf(InvalidValueException.class);
        assertThat(failure(() -> registrations.recordSubmitted(
                        registrations.targetOf(registration.id()),
                        registration.id(),
                        new ResidenceRegistrationService.Submission(null, SubmissionChannel.IN_PERSON, "CA", null))))
                .isInstanceOf(InvalidValueException.class);
        assertThat(reload(registration).status()).isEqualTo(RegistrationStatus.TO_SUBMIT);
        assertThat(submissions(registration.id())).isEmpty();
    }

    @Test
    void Q7_acceptingATemporaryResidenceRegistration_needsEvidenceAndExpiryDate() throws Exception {
        ResidenceRegistration registration = created();
        assertThat(registration.type()).isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        submit(registration);

        command(office, registration.id(), "record-accepted", acceptedBody(evidence(), null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EXPIRY_DATE_REQUIRED"));
        command(office, registration.id(), "record-accepted", acceptedBody(null, today().plusYears(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EVIDENCE_REQUIRED"));
        assertThat(reload(registration).status()).isEqualTo(RegistrationStatus.SUBMITTED);

        long file = evidence();
        LocalDate expiry = LocalDate.of(2027, 9, 30);
        command(office, registration.id(), "record-accepted", acceptedBody(file, expiry))
                .andExpect(status().isOk());

        ResidenceRegistration after = reload(registration);
        assertThat(after.status()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(after.expiryDate()).isEqualTo(expiry);
        assertThat(((Number) row(registration.id()).get("evidence_file_id")).longValue())
                .isEqualTo(file);
        assertThat(submissions(registration.id()))
                .extracting(s -> s.get("result"))
                .containsExactly("ACCEPTED");
        assertThat(jdbc.queryForMap("SELECT owner_type, owner_id FROM file WHERE id = ?", file))
                .as("bằng chứng gắn vào khai báo")
                .containsEntry("owner_type", "M02.REGISTRATION_EVIDENCE")
                .containsEntry("owner_id", registration.id());
    }

    @Test
    void Q7_acceptingAStayNotification_needsNoExpiryDate() throws Exception {
        ResidenceRegistration registration = createdFor(ResidenceType.SHORT_TERM_RESIDENTIAL, today().plusDays(13));
        assertThat(registration.type()).isEqualTo(RegistrationType.STAY_NOTIFICATION);
        submit(registration);

        command(office, registration.id(), "record-accepted", acceptedBody(evidence(), null))
                .andExpect(status().isOk());

        ResidenceRegistration after = reload(registration);
        assertThat(after.status()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(after.expiryDate()).isNull();
    }

    // ---- Q8: chưa ghi nộp -------------------------------------------------------------------------------------

    @Test
    void Q8_recordingAResultBeforeSubmission_isBlocked() throws Exception {
        ResidenceRegistration registration = created();

        command(office, registration.id(), "record-accepted", acceptedBody(evidence(), today().plusYears(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_SUBMITTED"));
        command(office, registration.id(), "record-rejected", "{\"reason\":\"thiếu giấy tờ\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_SUBMITTED"));

        assertThat(reload(registration).status()).isEqualTo(RegistrationStatus.TO_SUBMIT);
        assertThat(submissions(registration.id())).isEmpty();
    }

    @Test
    void FR_005_aResultIsRecordedOnce() throws Exception {
        ResidenceRegistration registration = created();
        submit(registration);
        accept(registration);

        command(office, registration.id(), "record-rejected", "{\"reason\":\"ghi nhầm\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        command(office, registration.id(), "record-accepted", acceptedBody(evidence(), today().plusYears(2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        command(office, registration.id(), "record-submitted", submittedBody(today()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));

        assertThat(submissions(registration.id()))
                .extracting(s -> s.get("result"))
                .containsExactly("ACCEPTED");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE registration_submission SET result = 'REJECTED', rejection_reason = 'x'"
                                + " WHERE registration_id = ?",
                        registration.id()))
                .as("kết quả đặt một lần cả ở tầng dữ liệu")
                .hasMessageContaining("set once");
    }

    // ---- Q9: bị từ chối ---------------------------------------------------------------------------------------

    @Test
    void Q9_rejecting_returnsToToSubmit_withANewDueDate_andKeepsTheObligationDueDate() throws Exception {
        ResidenceRegistration registration = created();
        Map<String, Object> obligationBefore =
                obligations(registration.residentId()).getFirst();
        submit(registration);
        clockAt(today().plusDays(3), 10);
        Instant before = time.now();

        reject(registration, "thiếu giấy tờ");

        Instant after = time.now();
        ResidenceRegistration rejected = reload(registration);
        assertThat(rejected.status()).isEqualTo(RegistrationStatus.TO_SUBMIT);
        assertThat(rejected.dueAt())
                .as("hạn mới = thời điểm ghi từ chối + CFG-M02-12 (1 ngày)")
                .isBetween(before.plus(Duration.ofDays(1)), after.plus(Duration.ofDays(1)));
        Map<String, Object> obligation = obligations(registration.residentId()).getFirst();
        assertThat(obligation.get("due_at"))
                .as("hạn gốc của nghĩa vụ giữ nguyên")
                .isEqualTo(obligationBefore.get("due_at"));
        assertThat(obligation.get("submitted")).as("không còn lần nộp đang chờ").isEqualTo(false);
        assertThat(registrationCount(registration.residentId()))
                .as("tiếp tục khai báo hiện tại, không lập khai báo mới")
                .isEqualTo(1);
        assertThat(submissions(registration.id()))
                .extracting(s -> s.get("result"), s -> s.get("rejection_reason"))
                .containsExactly(tuple("REJECTED", "thiếu giấy tờ"));
    }

    @Test
    void Q9_rejectingWithoutAReason_isRejected() throws Exception {
        ResidenceRegistration registration = created();
        submit(registration);
        user.set(office.user());

        assertThat(failure(() -> registrations.recordRejected(
                        registrations.targetOf(registration.id()), registration.id(), "  ")))
                .isInstanceOf(InvalidValueException.class);
        assertThat(reload(registration).status()).isEqualTo(RegistrationStatus.SUBMITTED);
    }

    @Test
    void Q9_resubmittingAfterRejections_addsNewSubmissions_andKeepsTheHistory() throws Exception {
        ResidenceRegistration registration = created();
        submit(registration);
        reject(registration, "thiếu giấy tờ");
        submit(registration);
        reject(registration, "sai địa chỉ");
        submit(registration);
        List<Map<String, Object>> beforeAccept = submissions(registration.id());

        accept(registration);

        List<Map<String, Object>> rows = submissions(registration.id());
        assertThat(rows)
                .extracting(s -> s.get("result"), s -> s.get("rejection_reason"))
                .containsExactly(
                        tuple("REJECTED", "thiếu giấy tờ"), tuple("REJECTED", "sai địa chỉ"), tuple("ACCEPTED", null));
        for (int i = 0; i < 2; i++) {
            assertThat(rows.get(i)).as("lần nộp cũ không đổi").isEqualTo(beforeAccept.get(i));
        }
        assertThat(row(registration.id()).get("current_submission_id"))
                .isEqualTo(rows.get(2).get("id"));
        assertThat(reload(registration).status()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(registrationCount(registration.residentId())).isEqualTo(1);
    }

    @Test
    void Q9_afterARejection_overdueRemindersUseTheNewDueDate_andTheManagerIsToldOnce() throws Exception {
        ResidenceRegistration registration = created();
        long residentId = registration.residentId();
        LocalDate originalDue = dateOf(obligations(residentId).getFirst().get("due_at"));
        submit(registration);
        LocalDate rejectedOn = originalDue.plusDays(2);
        clockAt(rejectedOn, 10);
        reject(registration, "thiếu giấy tờ");

        overdueReminders.remind(rejectedOn.plusDays(1));
        assertThat(reminders(residentId))
                .as("hạn gốc đã qua nhưng hạn đang có hiệu lực là hạn mới của khai báo (lúc ghi + 1 ngày)")
                .isEmpty();
        clockAt(rejectedOn.plusDays(1), 12);
        mvc.perform(get("/api/v1/residence-registrations/to-do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.residentId == %d)]".formatted(residentId))
                        .isEmpty());

        overdueReminders.remind(rejectedOn.plusDays(2));
        overdueReminders.remind(rejectedOn.plusDays(2));
        overdueReminders.remind(rejectedOn.plusDays(3));

        assertThat(reminders(residentId))
                .extracting(ResidenceRegistrationReminder::kind, ResidenceRegistrationReminder::on)
                .containsExactly(
                        tuple(Kind.OVERDUE, rejectedOn.plusDays(2)),
                        tuple(Kind.OVERDUE_MANAGER, rejectedOn.plusDays(2)),
                        tuple(Kind.OVERDUE, rejectedOn.plusDays(3)));
        clockAt(rejectedOn.plusDays(3), 12);
        mvc.perform(get("/api/v1/residence-registrations/to-do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.residentId == %d)].taskType".formatted(residentId))
                        .value("OVERDUE"))
                .andExpect(jsonPath("$[?(@.residentId == %d)].dueAt".formatted(residentId))
                        .value(reload(registration).dueAt().toString()));
    }

    @Test
    void Q9_aManagerAlreadyToldBeforeTheRejection_isNotToldAgain() throws Exception {
        ResidenceRegistration registration = created();
        long residentId = registration.residentId();
        LocalDate originalDue = dateOf(obligations(residentId).getFirst().get("due_at"));
        overdueReminders.remind(originalDue.plusDays(1));
        Object notifiedAt = obligations(residentId).getFirst().get("manager_notified_at");
        assertThat(notifiedAt).isNotNull();

        submit(registration);
        LocalDate rejectedOn = originalDue.plusDays(2);
        clockAt(rejectedOn, 10);
        reject(registration, "thiếu giấy tờ");
        overdueReminders.remind(rejectedOn.plusDays(2));

        assertThat(reminders(residentId))
                .extracting(ResidenceRegistrationReminder::kind, ResidenceRegistrationReminder::on)
                .containsExactly(
                        tuple(Kind.OVERDUE, originalDue.plusDays(1)),
                        tuple(Kind.OVERDUE_MANAGER, originalDue.plusDays(1)),
                        tuple(Kind.OVERDUE, rejectedOn.plusDays(2)));
        assertThat(obligations(residentId).getFirst().get("manager_notified_at"))
                .as("không đặt lại khi bị từ chối")
                .isEqualTo(notifiedAt);
    }

    // ---- Thay thế khai báo Đã xác nhận cũ (FR-005, DBR-33) ------------------------------------------------------

    @Test
    void FR_005_acceptingANewRegistration_supersedesTheConfirmedOne_inTheSameTransaction() throws Exception {
        ResidenceRegistration first = created();
        long residentId = first.residentId();
        submit(first);
        accept(first);
        jdbc.update(
                "INSERT INTO registration_obligation (resident_id, trigger_type, trigger_at, due_at, source_id)"
                        + " VALUES (?, 'RENEWAL_ADDENDUM', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) + INTERVAL 1 DAY, ?)",
                residentId,
                first.id());
        ResidenceRegistration second = create(residentId);
        assertThat(row(second.id()).get("replaced_registration_id")).isEqualTo(first.id());
        submit(second);
        assertThat(reload(first).status())
                .as("chưa chấp nhận thì khai báo cũ còn hiệu lực")
                .isEqualTo(RegistrationStatus.CONFIRMED);

        accept(second);

        assertThat(reload(first).status()).isEqualTo(RegistrationStatus.SUPERSEDED);
        assertThat(reload(second).status()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM residence_registration WHERE resident_id = ? AND status = 'CONFIRMED'",
                        Integer.class,
                        residentId))
                .isEqualTo(1);
    }

    // ---- Đồng thời, chống lặp ---------------------------------------------------------------------------------

    private List<Integer> concurrently(Callable<Integer> call) throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Integer> statuses = new ArrayList<>();
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return call.call();
                }));
            }
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
        } finally {
            pool.shutdownNow();
        }
        return statuses;
    }

    private Integer serviceCall(Runnable command) {
        BusinessException e = failure(command);
        if (e == null) {
            return 200;
        }
        // Lệnh thua thấy trạng thái lệnh thắng đã ghi: Đã xác nhận (STATE_CHANGED) hoặc về Cần khai báo (NOT_SUBMITTED)
        assertThat(e.errorCode()).isIn("STATE_CHANGED", "NOT_SUBMITTED");
        return e.status().value();
    }

    @Test
    void concurrency_twoSimultaneousSubmissions_recordOne() throws Exception {
        ResidenceRegistration registration = created();
        user.set(office.user());
        ResidenceRegistrationService.Submission submission = new ResidenceRegistrationService.Submission(
                today(), SubmissionChannel.IN_PERSON, "Công an phường X", null);

        List<Integer> statuses = concurrently(() -> serviceCall(() -> registrations.recordSubmitted(
                registrations.targetOf(registration.id()), registration.id(), submission)));

        assertThat(statuses).containsExactlyInAnyOrder(200, HttpStatus.CONFLICT.value());
        assertThat(submissions(registration.id())).hasSize(1);
    }

    @Test
    void concurrency_acceptAndRejectAtTheSameTime_onlyOneResultIsRecorded() throws Exception {
        ResidenceRegistration registration = created();
        submit(registration);
        user.set(office.user());
        long file = evidence();
        ResidenceRegistrationService.Acceptance acceptance =
                new ResidenceRegistrationService.Acceptance(file, today().plusYears(1));
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Integer> statuses = new ArrayList<>();
        try {
            Future<Integer> accepted = pool.submit(() -> {
                start.await();
                return serviceCall(() -> registrations.recordAccepted(
                        registrations.targetOf(registration.id()), registration.id(), acceptance));
            });
            Future<Integer> rejected = pool.submit(() -> {
                start.await();
                return serviceCall(() -> registrations.recordRejected(
                        registrations.targetOf(registration.id()), registration.id(), "thiếu giấy tờ"));
            });
            statuses.add(accepted.get());
            statuses.add(rejected.get());
        } finally {
            pool.shutdownNow();
        }

        assertThat(statuses).containsExactlyInAnyOrder(200, HttpStatus.CONFLICT.value());
        assertThat(submissions(registration.id())).hasSize(1);
        assertThat(submissions(registration.id()).getFirst().get("result")).isIn("ACCEPTED", "REJECTED");
    }

    @Test
    void idempotency_theSameKeyTwice_recordsOneSubmission() throws Exception {
        ResidenceRegistration registration = created();
        UUID key = UUID.randomUUID();
        String body = submittedBody(today());

        command(office, registration.id(), "record-submitted", body, key).andExpect(status().isOk());
        command(office, registration.id(), "record-submitted", body, key).andExpect(status().isOk());

        assertThat(submissions(registration.id())).hasSize(1);
    }

    // ---- Q10: quyền -------------------------------------------------------------------------------------------

    @Test
    void Q10_nurseFloorManagerAccountantAndManager_cannotRecord() throws Exception {
        ResidenceRegistration registration = created();
        List<TestUser> others = List.of(
                users.staff(Role.NURSING_STAFF),
                users.staff(Role.FLOOR_MANAGER),
                users.staff(Role.ACCOUNTANT),
                manager);

        for (TestUser who : others) {
            command(who, registration.id(), "record-submitted", submittedBody(today()))
                    .andExpect(status().isForbidden());
        }
        submit(registration);
        for (TestUser who : others) {
            command(who, registration.id(), "record-rejected", "{\"reason\":\"x\"}")
                    .andExpect(status().isForbidden());
            command(who, registration.id(), "record-accepted", acceptedBody(evidence(), today().plusYears(1)))
                    .andExpect(status().isForbidden());
        }

        assertThat(reload(registration).status()).isEqualTo(RegistrationStatus.SUBMITTED);
        assertThat(submissions(registration.id())).hasSize(1);
    }

    // ---- Đính chính M02.RESIDENCE_REGISTRATION (research Z-04, FR-001) --------------------------------------------

    private ResultActions correct(long id, String content) throws Exception {
        user.set(office.user());
        return mvc.perform(post(CORRECTIONS + id)
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":%s,\"reason\":\"Ghi nhầm\"}"
                        .formatted(content)));
    }

    private void confirmLocal(ResidenceRegistration registration) {
        user.set(office.user());
        registrations.confirmLocalPermanentResident(
                registrations.targetOf(registration.id()), registration.id(), "CCCD: thường trú phường Bến Nghé");
    }

    @Test
    void FR_001_aMistakenLocalConfirmation_isRemovedByCorrection_whileToSubmit() throws Exception {
        ResidenceRegistration registration = created();
        confirmLocal(registration);

        correct(registration.id(), "{\"localPermanentResident\":false}").andExpect(status().isCreated());

        ResidenceRegistration after = reload(registration);
        assertThat(after.localPermanentResident()).isFalse();
        assertThat(after.type())
                .as("loại tính lại theo thời gian lưu trú dự kiến (FR-001)")
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        assertThat(after.dueAt()).isEqualTo(registration.dueAt());
        assertThat(jdbc.queryForObject(
                        "SELECT local_permanent_resident FROM registration_resident WHERE resident_id = ?",
                        Boolean.class,
                        registration.residentId()))
                .isFalse();
    }

    @Test
    void FR_001_theLocalConfirmation_cannotBeRemovedAfterSubmission() throws Exception {
        ResidenceRegistration registration = created();
        confirmLocal(registration);
        submit(registration);

        correct(registration.id(), "{\"localPermanentResident\":false}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        assertThat(reload(registration).localPermanentResident()).isTrue();
    }

    @Test
    void Z_04_submissionContent_isCorrectedInTheChain_andTheRecordedRowIsUnchanged() throws Exception {
        ResidenceRegistration registration = created();
        submit(registration);
        Map<String, Object> recorded = submissions(registration.id()).getFirst();

        correct(registration.id(), "{\"dossierCode\":\"G22.SUA\"}").andExpect(status().isCreated());

        assertThat(submissions(registration.id()).getFirst()).isEqualTo(recorded);
        mvc.perform(get(CORRECTIONS + registration.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentContent.dossierCode").value("G22.SUA"))
                .andExpect(jsonPath("$.originalContent.dossierCode").value(recorded.get("dossier_code")));
        correct(registration.id(), "{\"status\":\"CONFIRMED\"}").andExpect(status().isUnprocessableContent());
    }

    @Test
    void Z_04_aRegistrationWithNothingRecorded_cannotBeCorrected() throws Exception {
        ResidenceRegistration registration = created();

        correct(registration.id(), "{\"dossierCode\":\"G22.SUA\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RECORD_NOT_CONFIRMED"));
    }
}
