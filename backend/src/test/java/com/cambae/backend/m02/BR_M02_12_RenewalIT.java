package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder.Kind;
import com.cambae.backend.m02.internal.registration.domain.RegistrationType;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.m02.internal.registration.service.RegistrationExpiry;
import com.cambae.backend.m02.internal.registration.service.RegistrationRenewalReminders;
import com.cambae.backend.m02.internal.registration.service.ResidenceRegistrationService;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 018 T011 ({@code BR_M02_12_GiaHanIT}; US3 #1 → #3, #5, quickstart Q11 → Q13, Q15; FR-004 → FR-006, BR-M02-12, DBR-33,
 * Q-306): nhắc lập khai báo gia hạn mỗi ngày từ CFG-M02-13 trước ngày hết hạn tới khi có khai báo đang xử lý; lập khai báo
 * gia hạn (hạn = hết ngày hết hạn, chủ dự án chốt 2026-10-27), được chấp nhận thì khai báo cũ Được thay thế; qua ngày hết
 * hạn thì Hết hạn, báo Quản lý viện, nhắc Hành chính lập (không tự tạo); ngày hết hạn đã đính chính được dùng. Việc nhắc,
 * chuyển Hết hạn gọi thẳng service vì job chưa gắn (BL-90).
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(FakeFileConfig.class)
class BR_M02_12_RenewalIT extends RegistrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestClockControl clock;

    @Autowired
    FileApi files;

    @Autowired
    RegistrationRenewalReminders renewalReminders;

    @Autowired
    RegistrationExpiry expiry;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private long evidence() {
        return files.upload(
                "ket-qua.pdf", FileTypes.PDF, "%PDF-1.4 ket qua".getBytes(StandardCharsets.UTF_8), office.accountId());
    }

    /** Người nội trú dài hạn có khai báo Đăng ký tạm trú Đã xác nhận hết hạn {@code expiryDate}. */
    private ResidenceRegistration confirmedUntil(LocalDate expiryDate) {
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        ResidenceRegistration registration = createFor(residentId);
        confirm(registration, expiryDate, evidence());
        return registration;
    }

    private ResidenceRegistration renew(long residentId) {
        user.set(office.user());
        return registrations.createRenewalRegistration(
                ResidenceRegistrationService.targetForResident(residentId), residentId);
    }

    private List<ResidenceRegistrationReminder> reminders(long residentId) {
        return events.stream(ResidenceRegistrationReminder.class)
                .filter(event -> event.residentId() == residentId)
                .toList();
    }

    // ---- Q11: nhắc gia hạn ---------------------------------------------------------------------------------------

    @Test
    void Q11_renewalReminder_startsCfgM0213DaysBefore_repeatsDaily_andStopsOnceARenewalIsInProgress() {
        LocalDate expiryDate = today().plusDays(40);
        ResidenceRegistration registration = confirmedUntil(expiryDate);
        long residentId = registration.residentId();

        renewalReminders.remind(expiryDate.minusDays(31));
        renewalReminders.remind(expiryDate.minusDays(30));
        renewalReminders.remind(expiryDate.minusDays(30));
        renewalReminders.remind(expiryDate.minusDays(29));
        renew(residentId);
        renewalReminders.remind(expiryDate.minusDays(28));

        assertThat(reminders(residentId))
                .filteredOn(event -> event.kind() == Kind.RENEWAL)
                .extracting(ResidenceRegistrationReminder::on)
                .as("từ ngày còn 30 ngày (CFG-M02-13), mỗi ngày một lần, dừng khi có khai báo gia hạn đang xử lý")
                .containsExactly(expiryDate.minusDays(30), expiryDate.minusDays(29));
    }

    // ---- Q12: lập và xác nhận khai báo gia hạn ------------------------------------------------------------------

    @Test
    void Q12_aConfirmedRenewal_supersedesTheOldRegistration() {
        LocalDate expiryDate = today().plusDays(20);
        ResidenceRegistration old = confirmedUntil(expiryDate);

        ResidenceRegistration renewal = renew(old.residentId());

        assertThat(renewal.renewal()).isTrue();
        assertThat(renewal.type()).isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        assertThat(renewal.dueAt())
                .as("hạn = hết ngày hết hạn của khai báo đang Đã xác nhận")
                .isEqualTo(
                        expiryDate.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant());
        assertThat(jdbc.queryForObject(
                        "SELECT replaced_registration_id FROM residence_registration WHERE id = ?",
                        Long.class,
                        renewal.id()))
                .isEqualTo(old.id());
        assertThat(statusOf(old.id()))
                .as("chưa xác nhận thì khai báo cũ còn hiệu lực")
                .isEqualTo("CONFIRMED");

        confirm(renewal, expiryDate.plusYears(1), evidence());

        assertThat(statusOf(old.id())).isEqualTo("SUPERSEDED");
        assertThat(statusOf(renewal.id())).isEqualTo("CONFIRMED");
    }

    @Test
    void FR_005_aRenewal_needsAConfirmedTemporaryResidenceRegistration() {
        long residentId = readyWith(ResidenceType.SHORT_TERM_RESIDENTIAL, today().plusDays(13));
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        ResidenceRegistration stayNotification = createFor(residentId);
        confirm(stayNotification, null, evidence());

        BusinessException e = failure(() -> renew(residentId));

        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e.errorCode()).isEqualTo("NO_CONFIRMED_TEMPORARY_RESIDENCE_REGISTRATION");
    }

    @Test
    void Q15_withARenewalSubmitted_anotherRegistrationIsBlocked() {
        ResidenceRegistration old = confirmedUntil(today().plusDays(20));
        ResidenceRegistration renewal = renew(old.residentId());
        user.set(office.user());
        registrations.recordSubmitted(
                registrations.targetOf(renewal.id()),
                renewal.id(),
                new ResidenceRegistrationService.Submission(
                        today(),
                        com.cambae.backend.m02.internal.registration.domain.SubmissionChannel.IN_PERSON,
                        "Công an phường X",
                        null));

        assertThat(failure(() -> renew(old.residentId())).errorCode()).isEqualTo("REGISTRATION_IN_PROGRESS_EXISTS");
        assertThat(failure(() -> createFor(old.residentId())).errorCode()).isEqualTo("REGISTRATION_IN_PROGRESS_EXISTS");
    }

    @Test
    void Q12_theRenewalCommandIsReachableOverHttp() throws Exception {
        ResidenceRegistration old = confirmedUntil(today().plusDays(20));
        user.set(office.user());

        mvc.perform(post("/api/v1/residents/" + old.residentId() + "/residence-registrations")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"renewal\": true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.renewal").value(true))
                .andExpect(jsonPath("$.status").value("TO_SUBMIT"));
    }

    // ---- Q13: Hết hạn ---------------------------------------------------------------------------------------------

    @Test
    void Q13_pastTheExpiryDate_theRegistrationExpires_theManagerIsTold_andTheOfficeIsRemindedToCreate() {
        LocalDate expiryDate = today().plusDays(5);
        ResidenceRegistration old = confirmedUntil(expiryDate);
        long residentId = old.residentId();
        int before = registrationCount(residentId);

        expiry.expire(expiryDate);
        assertThat(statusOf(old.id())).as("trong ngày hết hạn còn hiệu lực").isEqualTo("CONFIRMED");

        clock.setTo(
                expiryDate.plusDays(1).atTime(6, 0).atZone(BusinessTime.ZONE).toInstant());
        expiry.expire(expiryDate.plusDays(1));
        expiry.expire(expiryDate.plusDays(1));

        assertThat(statusOf(old.id())).isEqualTo("EXPIRED");
        assertThat(registrationCount(residentId))
                .as("không tự tạo khai báo (Q-306)")
                .isEqualTo(before);
        Map<String, Object> obligation = obligations(residentId).getFirst();
        assertThat(obligation.get("trigger_type")).isEqualTo("EXPIRED");
        assertThat(((Number) obligation.get("source_id")).longValue()).isEqualTo(old.id());
        assertThat(dateOf(obligation.get("due_at"))).isEqualTo(expiryDate.plusDays(2));
        assertThat(reminders(residentId))
                .filteredOn(event -> event.kind() == Kind.EXPIRED_MANAGER || event.kind() == Kind.TO_CREATE)
                .extracting(ResidenceRegistrationReminder::kind)
                .as("báo Quản lý viện một lần, nhắc Hành chính lập")
                .containsExactlyInAnyOrder(Kind.EXPIRED_MANAGER, Kind.TO_CREATE);

        ResidenceRegistration next = createFor(residentId);
        assertThat(next.type()).isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        assertThat(next.renewal()).isFalse();
    }

    @Test
    void Q13_withARenewalInProgress_theOldOneExpires_butNoNewObligationIsRecorded() {
        LocalDate expiryDate = today().plusDays(5);
        ResidenceRegistration old = confirmedUntil(expiryDate);
        renew(old.residentId());
        int obligationsBefore = obligations(old.residentId()).size();

        expiry.expire(expiryDate.plusDays(1));

        assertThat(statusOf(old.id())).isEqualTo("EXPIRED");
        assertThat(obligations(old.residentId())).hasSize(obligationsBefore);
    }

    // ---- Ngày hết hạn đã đính chính (plan "PHASE 5 HANDOFF") ----------------------------------------------------

    @Test
    void Z_04_aCorrectedExpiryDate_drivesTheRenewalReminderAndTheExpiry() throws Exception {
        LocalDate recorded = today().plusDays(40);
        LocalDate corrected = recorded.plusDays(10);
        ResidenceRegistration registration = confirmedUntil(recorded);
        user.set(office.user());
        mvc.perform(post("/api/v1/corrections/M02.RESIDENCE_REGISTRATION/" + registration.id())
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"expiryDate\":\"%s\"},\"reason\":\"Ghi nhầm\"}"
                                        .formatted(corrected)))
                .andExpect(status().isCreated());
        mvc.perform(
                        post("/api/v1/corrections/M02.RESIDENCE_REGISTRATION/" + registration.id())
                                .header("Idempotency-Key", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"dossierCode\":\"G22.SUA\"},\"reason\":\"Ghi nhầm\"}"))
                .andExpect(status().isCreated());

        renewalReminders.remind(recorded.minusDays(30));
        renewalReminders.remind(corrected.minusDays(30));
        expiry.expire(recorded.plusDays(1));

        assertThat(reminders(registration.residentId()))
                .filteredOn(event -> event.kind() == Kind.RENEWAL)
                .extracting(ResidenceRegistrationReminder::on)
                .as("ngày hết hạn hiện hành theo đính chính, kể cả khi lần đính chính sau sửa trường khác")
                .containsExactly(corrected.minusDays(30));
        assertThat(statusOf(registration.id())).isEqualTo("CONFIRMED");
    }

    // ---- Danh sách cần làm --------------------------------------------------------------------------------------

    @Test
    void FR_011_toDo_listsRegistrationsExpiringSoon() throws Exception {
        LocalDate expiryDate = today().plusDays(10);
        ResidenceRegistration registration = confirmedUntil(expiryDate);
        clock.setTo(time.now().plus(Duration.ofHours(1)));
        user.set(office.user());

        mvc.perform(get("/api/v1/residence-registrations/to-do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.residentId == %d)].taskType".formatted(registration.residentId()))
                        .value("EXPIRING_SOON"))
                .andExpect(jsonPath("$[?(@.residentId == %d)].registrationId".formatted(registration.residentId()))
                        .value((int) registration.id()));
        assertThat(reminders(registration.residentId()))
                .extracting(ResidenceRegistrationReminder::kind, ResidenceRegistrationReminder::residentId)
                .doesNotContain(tuple(Kind.RENEWAL, registration.residentId()));
    }
}
