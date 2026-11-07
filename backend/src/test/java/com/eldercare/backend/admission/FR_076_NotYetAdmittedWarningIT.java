package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.M02Reminder;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.CareLevel;
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
            statement.executeUpdate("DELETE FROM reminder_marker WHERE kind IN ('M02.DAILY_RUN', 'M02.REMINDER')");
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
}
