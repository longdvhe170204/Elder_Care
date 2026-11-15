package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationType;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.admission.internal.registration.service.RegistrationExpiry;
import com.eldercare.backend.admission.internal.registration.service.RegistrationRenewalReminders;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
class BR_12_RenewalIT extends RegistrationTestBase {

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
        assertThat(RegistrationSql.replacedRegistrationId(jdbc, renewal.id())).isEqualTo(old.id());
        assertThat(statusOf(old.id()))
                .as("chưa xác nhận thì khai báo cũ còn hiệu lực")
                .isEqualTo("CONFIRMED");

        confirm(renewal, expiryDate.plusYears(1), evidence());

        assertThat(statusOf(old.id())).isEqualTo("SUPERSEDED");
        assertThat(statusOf(renewal.id())).isEqualTo("CONFIRMED");
    }

    // ---- Q13: Hết hạn ---------------------------------------------------------------------------------------------

    // ---- Ngày hết hạn đã đính chính (plan "PHASE 5 HANDOFF") ----------------------------------------------------

    // ---- Danh sách cần làm --------------------------------------------------------------------------------------
}
