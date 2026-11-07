package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.ResidenceRegistrationStatisticsApi;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.events.ContractStatusChanged;
import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder;
import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder.Kind;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationType;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.admission.internal.registration.service.RegistrationExpiry;
import com.eldercare.backend.admission.internal.registration.service.ResidenceRegistrationService;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sửa theo rà soát luồng nghiệp vụ 2026-11-06 (ra-soat-018.md P1a, P2, B1, N5): khai báo gia hạn đã lập mà chưa nộp vào
 * danh sách cần làm, nhắc quá hạn, số liệu cho 016 (FR-002, FR-011); hợp đồng Chấm dứt sớm không làm hỏng tính loại khai
 * báo (Q-241); hợp đồng nội trú mới nối tiếp của người chỉ có Thông báo lưu trú được nhắc như phụ lục gia hạn
 * (BR-M02-14); không báo Quản lý viện Hết hạn của người đã ở trạng thái cuối (BR-M02-12).
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(FakeFileConfig.class)
class RegistrationReviewIT extends RegistrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestClockControl clock;

    @Autowired
    FileApi files;

    @Autowired
    RegistrationExpiry expiry;

    @Autowired
    ResidenceRegistrationStatisticsApi statistics;

    @Autowired
    ApplicationEventPublisher publisher;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private long evidence() {
        return files.upload(
                "ket-qua.pdf", FileTypes.PDF, "%PDF-1.4 ket qua".getBytes(StandardCharsets.UTF_8), office.accountId());
    }

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

    private List<ResidenceRegistrationReminder> reminders(long residentId, Kind kind) {
        return events.stream(ResidenceRegistrationReminder.class)
                .filter(event -> event.residentId() == residentId && event.kind() == kind)
                .toList();
    }

    private void atNoon(LocalDate day) {
        clock.setTo(day.atTime(6, 0).atZone(BusinessTime.ZONE).toInstant());
    }

    // ---- P2: khai báo gia hạn đã lập mà chưa nộp -----------------------------------------------------------------

    @Test
    void FR_011_aRenewalInProgress_isInTheToDoList_countsInTheSummary_andBecomesOverdueAfterItsDueDate()
            throws Exception {
        LocalDate expiryDate = today().plusDays(20);
        ResidenceRegistration old = confirmedUntil(expiryDate);
        long residentId = old.residentId();
        ResidenceRegistration renewal = renew(residentId);

        mvc.perform(get("/api/v1/residence-registrations/to-do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.residentId == %d)].taskType".formatted(residentId))
                        .value("EXPIRING_SOON"))
                .andExpect(jsonPath("$[?(@.residentId == %d)].registrationId".formatted(residentId))
                        .value((int) renewal.id()));
        int toCreateBefore = statistics.summary().toCreate();

        atNoon(expiryDate.plusDays(2));
        overdueReminders.remind(expiryDate.plusDays(2));
        mvc.perform(get("/api/v1/residence-registrations/to-do"))
                .andExpect(jsonPath("$[?(@.residentId == %d)].taskType".formatted(residentId))
                        .value("OVERDUE"));
        assertThat(statistics.summary().overdue()).isGreaterThanOrEqualTo(1);
        assertThat(toCreateBefore).as("gia hạn chưa quá hạn tính vào cần lập").isGreaterThanOrEqualTo(1);
        assertThat(reminders(residentId, Kind.OVERDUE)).hasSize(1);
        assertThat(reminders(residentId, Kind.OVERDUE_MANAGER)).hasSize(1);

        atNoon(expiryDate.plusDays(3));
        overdueReminders.remind(expiryDate.plusDays(3));
        overdueReminders.remind(expiryDate.plusDays(3));
        assertThat(reminders(residentId, Kind.OVERDUE))
                .as("Hành chính nhắc mỗi ngày, mỗi ngày một lần")
                .hasSize(2);
        assertThat(reminders(residentId, Kind.OVERDUE_MANAGER))
                .as("Quản lý viện một lần")
                .hasSize(1);
    }

    @Test
    void FR_002_aSubmittedRenewal_isNotOverdue() {
        LocalDate expiryDate = today().plusDays(5);
        ResidenceRegistration old = confirmedUntil(expiryDate);
        ResidenceRegistration renewal = renew(old.residentId());
        user.set(office.user());
        registrations.recordSubmitted(
                registrations.targetOf(renewal.id()),
                renewal.id(),
                new ResidenceRegistrationService.Submission(
                        today(),
                        com.eldercare.backend.admission.internal.registration.domain.SubmissionChannel.IN_PERSON,
                        "Công an phường X",
                        null));

        atNoon(expiryDate.plusDays(3));
        overdueReminders.remind(expiryDate.plusDays(3));

        assertThat(reminders(old.residentId(), Kind.OVERDUE)).isEmpty();
    }

    // ---- P1a: hợp đồng Chấm dứt sớm ------------------------------------------------------------------------------

    @Test
    void Q_241_aTerminatedContractOverlappedByTheNextOne_doesNotBreakTheType() {
        long residentId = admissionResident();
        confirmedAssessment(residentId, com.eldercare.backend.resident.api.dto.CareLevel.BASIC);
        long terminated = contract(
                residentId,
                ContractStatus.ACTIVE,
                ResidenceType.LONG_TERM_RESIDENTIAL,
                today().minusDays(60),
                today().plusDays(300));
        jdbc.update(
                "UPDATE contract SET status = 'TERMINATED', termination_reason = 'Chuyển hợp đồng',"
                        + " terminated_on = ? WHERE id = ?",
                today().minusDays(6),
                terminated);
        contract(
                residentId,
                ContractStatus.ACTIVE,
                ResidenceType.SHORT_TERM_RESIDENTIAL,
                today().minusDays(5),
                today().plusDays(10));
        completeAdmission(residentId);
        awaitObligations(residentId, 1);

        ResidenceRegistration created = createFor(residentId);

        assertThat(created.type())
                .as("hợp đồng cũ chấm dứt khi hợp đồng mới bắt đầu: chuỗi liền nhau, 71 ngày")
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    // ---- B1: hợp đồng nội trú mới nối tiếp ------------------------------------------------------------------------

    @Test
    void BR_M02_14_aConsecutiveResidentialContract_ofSomeoneWithOnlyAStayNotification_remindsLikeAnExtension() {
        long residentId = readyWith(ResidenceType.SHORT_TERM_RESIDENTIAL, today().plusDays(13));
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        ResidenceRegistration notice = createFor(residentId);
        confirm(notice, null, evidence());
        long next = contract(
                residentId,
                ContractStatus.ACTIVE,
                ResidenceType.SHORT_TERM_RESIDENTIAL,
                today().plusDays(14),
                today().plusDays(60));

        transaction.executeWithoutResult(status -> publisher.publishEvent(new ContractStatusChanged(
                next, residentId, ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE, null)));

        Map<String, Object> obligation = awaitObligations(residentId, 2).getFirst();
        assertThat(obligation.get("trigger_type")).isEqualTo("RENEWAL_ADDENDUM");
        assertThat(((Number) obligation.get("source_id")).longValue()).isEqualTo(next);
        assertThat(createFor(residentId).type())
                .as("60 ngày liền chuỗi: Đăng ký tạm trú")
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    @Test
    void BR_M02_14_aNonConsecutiveResidentialContract_recordsNothing() {
        long residentId = readyWith(ResidenceType.SHORT_TERM_RESIDENTIAL, today().plusDays(13));
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        confirm(createFor(residentId), null, evidence());
        long next = contract(
                residentId,
                ContractStatus.ACTIVE,
                ResidenceType.SHORT_TERM_RESIDENTIAL,
                today().plusDays(20),
                today().plusDays(40));

        transaction.executeWithoutResult(status -> publisher.publishEvent(new ContractStatusChanged(
                next, residentId, ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE, null)));
        // Mốc chắc chắn sau sự kiện trên: một người nội trú khác vào ở
        long other = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(other);
        awaitObligations(other, 1);

        assertThat(obligations(residentId))
                .as("có khoảng trống: không phải gia hạn")
                .hasSize(1);
    }

    // ---- N5: người đã ở trạng thái cuối ---------------------------------------------------------------------------

    @Test
    void BR_M02_12_expiryOfSomeoneWhoEndedResidency_doesNotTellTheManager_butStillExpires() {
        LocalDate expiryDate = today().plusDays(5);
        ResidenceRegistration old = confirmedUntil(expiryDate);
        long residentId = old.residentId();
        jdbc.update("UPDATE resident SET status = 'RESIDENCY_ENDED' WHERE id = ?", residentId);

        expiry.expire(expiryDate.plusDays(1));

        assertThat(statusOf(old.id())).as("vẫn Hết hạn để còn khai báo xóa").isEqualTo("EXPIRED");
        assertThat(reminders(residentId, Kind.EXPIRED_MANAGER))
                .as("không báo Quản lý viện cho người đã Kết thúc lưu trú")
                .isEmpty();
    }
}
