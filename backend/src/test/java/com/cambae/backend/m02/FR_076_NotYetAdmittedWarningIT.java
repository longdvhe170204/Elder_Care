package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.events.M02Reminder;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 004 T014 (US1; FR-076, FR-074): người cao tuổi nội trú chưa Hoàn tất tiếp nhận sau ngày bắt đầu hợp đồng thì danh sách
 * điều kiện cảnh báo "phí đang tính cho ngày chưa vào ở" và việc nhắc hằng ngày (mức Nhẹ, chỉ Hành chính) gửi một lần
 * mỗi ngày; dừng khi Hoàn tất tiếp nhận, Hủy tiếp nhận hoặc ngày bắt đầu không còn ở quá khứ.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_076_NotYetAdmittedWarningIT extends M02TestBase {

    private static final String TYPE = "NOT_YET_ADMITTED";
    private static final String WARNING = "NOT_YET_ADMITTED_FEE_CHARGED";

    @Autowired
    MockMvc mvc;

    @Autowired
    TestClockControl clock;

    @Autowired
    ApplicationEvents applicationEvents;

    @BeforeEach
    @AfterEach
    void cleanReminderState() throws SQLException {
        clock.reset();
        try (Connection connection = adminConnection();
                var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM daily_run WHERE task = 'REMINDERS'");
            statement.executeUpdate("DELETE FROM m02_reminder WHERE type = '" + TYPE + "'");
        }
    }

    private long application(long residentId) throws Exception {
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());
        return applicationIdOf(residentId);
    }

    private boolean warns(long applicationId) throws Exception {
        String json = mvc.perform(get("/api/v1/admission-applications/" + applicationId + "/admission-conditions"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return StreamSupport.stream(
                        JsonMapper.builder()
                                .build()
                                .readTree(json)
                                .get("warnings")
                                .spliterator(),
                        false)
                .anyMatch(warning -> WARNING.equals(warning.get("code").asString()));
    }

    private List<M02Reminder> remindersOf(long applicationId) {
        return applicationEvents.stream(M02Reminder.class)
                .filter(reminder -> TYPE.equals(reminder.type()) && reminder.subjectId() == applicationId)
                .toList();
    }

    @Test
    void FR_076_contractStartedInThePast_warnsOnTheConditionList() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        confirmedAssessment(residentId, CareLevel.BASIC);

        contracts.active(residentId, CareLevel.BASIC, today().minusDays(2));
        assertThat(warns(id)).isTrue();

        contracts.reset();
        contracts.active(residentId, CareLevel.BASIC, today());
        assertThat(warns(id)).as("đúng ngày bắt đầu chưa là 'sau ngày bắt đầu'").isFalse();

        contracts.reset();
        contracts.active(residentId, CareLevel.BASIC, today().plusDays(1));
        assertThat(warns(id)).isFalse();
    }

    @Test
    void FR_076_dayCareDoesNotWarn() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        contracts.contract(
                residentId, ContractStatus.ACTIVE, ResidenceType.DAY_CARE, CareLevel.BASIC, today().minusDays(2));

        assertThat(warns(id)).isFalse();
    }

    @Test
    void FR_076_dailyRun_remindsOncePerDay_untilTheStartDateIsNoLongerInThePast() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        contracts.active(residentId, CareLevel.BASIC, today().minusDays(2));

        dailyRunner.catchUp();
        assertThat(remindersOf(id)).hasSize(1);
        assertThat(remindersOf(id).get(0).on()).isEqualTo(today());

        dailyRunner.catchUp();
        assertThat(remindersOf(id)).as("cùng ngày không nhắc lại").hasSize(1);

        clock.advanceBy(Duration.ofDays(1));
        dailyRunner.catchUp();
        assertThat(remindersOf(id)).hasSize(2);

        // Phụ lục đổi ngày bắt đầu sang tương lai: dừng nhắc
        contracts.reset();
        contracts.active(residentId, CareLevel.BASIC, today().plusDays(5));
        clock.advanceBy(Duration.ofDays(1));
        dailyRunner.catchUp();
        assertThat(remindersOf(id)).hasSize(2);
    }

    @Test
    void FR_076_remindersStopAfterCompletionOrCancellation() throws Exception {
        long completed = admissionResident();
        long completedApplication = application(completed);
        confirmedAssessment(completed, CareLevel.BASIC);
        contracts.active(completed, CareLevel.BASIC, today().minusDays(1));
        user.set(office.user());
        statusService.completeAdmission(
                target(completed), new Reason("Vào ở muộn"), JsonNodeFactory.instance.objectNode());

        long cancelled = admissionResident();
        long cancelledApplication = application(cancelled);
        contracts.active(cancelled, CareLevel.BASIC, today().minusDays(1));
        user.set(office.user());
        statusService.cancelAdmission(
                target(cancelled), new Reason("Gia đình không đồng ý"), JsonNodeFactory.instance.objectNode());

        dailyRunner.catchUp();

        assertThat(remindersOf(completedApplication)).isEmpty();
        assertThat(remindersOf(cancelledApplication)).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM admission_application WHERE id = ?", String.class, cancelledApplication))
                .isEqualTo("CANCELLED");
    }

    @Test
    void FR_076_reminderIsLowLevelAndForOfficeOnly() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        contracts.active(residentId, CareLevel.BASIC, today().minusDays(2));

        dailyRunner.catchUp();

        var requests = remindersOf(id).get(0).notificationRequests();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).level()).contains(NotificationLevel.LOW);
        assertThat(requests.get(0).recipientGroups())
                .containsExactly(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
    }
}
