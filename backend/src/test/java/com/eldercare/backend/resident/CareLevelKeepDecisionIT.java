package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.events.CareLevelChangeDeclinedLeave;
import com.eldercare.backend.resident.api.events.CareLevelChangeRequestReminder;
import com.eldercare.backend.resident.api.events.CareLevelKeepReviewDue;
import com.eldercare.backend.resident.api.spi.CareRiskAgreementPort;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.assessment.domain.CareLevelChangeDecision.Decision;
import com.eldercare.backend.resident.internal.assessment.service.CareLevelChangeDecisionService;
import com.eldercare.backend.resident.internal.assessment.service.CareLevelChangeRequestReminderService;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * 001 FR-036b (Q-423, R13; plan-bf08 Bước 6): gia đình không đồng ý đổi mức. Giữ mức cũ cần thỏa thuận lựa chọn có rủi ro
 * đang hiệu lực và ngày xem lại; có quyết định thì dừng nhắc lập yêu cầu đổi mức; tới ngày xem lại nhắc Bác sĩ, Hành
 * chính một lần; đánh giá lại mới thì nhắc lại. Rời viện thì báo Hành chính lập hồ sơ kết thúc lưu trú.
 */
@Import(CareLevelKeepDecisionIT.RiskFake.class)
class CareLevelKeepDecisionIT extends AssessmentTestBase {

    /** Thỏa thuận đang hiệu lực theo mã, do test đặt (bản thật ở 005). */
    @TestConfiguration
    static class RiskFake {
        static final Set<Long> ACTIVE = new HashSet<>();

        @Bean
        @Primary
        CareRiskAgreementPort fakeCareRiskAgreements() {
            return (residentId, agreementId, at) -> ACTIVE.contains(agreementId);
        }
    }

    @Autowired
    CareLevelChangeDecisionService decisions;

    @Autowired
    CareLevelChangeRequestReminderService reminders;

    @Autowired
    TestClockControl clock;

    @AfterEach
    void reset() {
        RiskFake.ACTIVE.clear();
        clock.reset();
    }

    /** Người đang lưu trú mức Cơ bản, vừa được đánh giá lại ra mức Thường xuyên; trả mã lần đánh giá lại. */
    private long reassessed(long residentId, CareLevel level) {
        return confirmed(residentId, AssessmentType.REASSESSMENT, level, "Đánh giá lại", null)
                .id();
    }

    private long basicResident() {
        long residentId = newResident();
        setStatus(residentId, "ADMISSION_IN_PROGRESS");
        confirmed(residentId, AssessmentType.INITIAL, CareLevel.BASIC, "Đánh giá đầu vào", null);
        setStatus(residentId, "IN_RESIDENCE");
        return residentId;
    }

    private long reminderCount(long residentId) {
        return events.stream(CareLevelChangeRequestReminder.class)
                .filter(e -> e.residentId() == residentId)
                .count();
    }

    private void runRemindersAfter(Duration d) {
        clock.advanceBy(d);
        reminders.run();
        decisions.remindReviews();
    }

    @Test
    void R13_keepCurrentLevel_stopsReminders_reviewRemindedOnce_newReassessmentRemindsAgain() {
        long residentId = basicResident();
        long assessmentId = reassessed(residentId, CareLevel.REGULAR);
        runRemindersAfter(Duration.ofDays(2));
        assertThat(reminderCount(residentId)).isEqualTo(1);

        RiskFake.ACTIVE.add(777L);
        user.set(office.user());
        decisions.record(
                ResidentProfileService.target(residentId),
                residentId,
                new CareLevelChangeDecisionService.Input(
                        assessmentId,
                        Decision.KEEP_CURRENT_LEVEL,
                        777L,
                        time.today().plusDays(5),
                        "Gia đình giữ mức"));

        runRemindersAfter(Duration.ofDays(2));
        assertThat(reminderCount(residentId)).as("dừng nhắc").isEqualTo(1);

        runRemindersAfter(Duration.ofDays(4));
        runRemindersAfter(Duration.ofDays(1));
        assertThat(events.stream(CareLevelKeepReviewDue.class).filter(e -> e.residentId() == residentId))
                .hasSize(1);

        reassessed(residentId, CareLevel.SPECIAL);
        runRemindersAfter(Duration.ofDays(2));
        assertThat(reminderCount(residentId))
                .as("đánh giá lại mới thì nhắc lại")
                .isEqualTo(2);
    }

    @Test
    void R13_keepWithoutActiveAgreement_409() {
        long residentId = basicResident();
        long assessmentId = reassessed(residentId, CareLevel.REGULAR);

        user.set(office.user());
        assertThatThrownBy(() -> decisions.record(
                        ResidentProfileService.target(residentId),
                        residentId,
                        new CareLevelChangeDecisionService.Input(
                                assessmentId,
                                Decision.KEEP_CURRENT_LEVEL,
                                999L,
                                time.today().plusDays(5),
                                null)))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("RISK_AGREEMENT_REQUIRED"));
    }

    @Test
    void R13_openChangeRequest_409() {
        long residentId = basicResident();
        long assessmentId = reassessed(residentId, CareLevel.REGULAR);
        changeRequests.open(residentId, 1L);

        user.set(office.user());
        assertThatThrownBy(() -> decisions.record(
                        ResidentProfileService.target(residentId),
                        residentId,
                        new CareLevelChangeDecisionService.Input(
                                assessmentId, Decision.LEAVE_FACILITY, null, null, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("CHANGE_REQUEST_EXISTS"));
    }

    @Test
    void R13_leaveFacility_stopsReminders_officeNotified() {
        long residentId = basicResident();
        long assessmentId = reassessed(residentId, CareLevel.REGULAR);

        user.set(office.user());
        decisions.record(
                ResidentProfileService.target(residentId),
                residentId,
                new CareLevelChangeDecisionService.Input(assessmentId, Decision.LEAVE_FACILITY, null, null, null));

        assertThat(events.stream(CareLevelChangeDeclinedLeave.class).filter(e -> e.residentId() == residentId))
                .hasSize(1);
        runRemindersAfter(Duration.ofDays(2));
        assertThat(reminderCount(residentId)).isZero();
    }
}
