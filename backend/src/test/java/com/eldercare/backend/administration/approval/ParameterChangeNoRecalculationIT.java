package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.approval.api.events.ApproverReminderDue;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * FR-017 (000 T079): đổi tham số không tính lại kết quả đã có. Yêu cầu đã được nhắc theo CFG-M15-05 cũ (48 giờ); đổi
 * CFG-M15-05 thành 72 giờ thì mốc nhắc đã ghi, nhật ký nhắc và trạng thái yêu cầu giữ nguyên, không nhắc lại.
 *
 * <p>Yêu cầu lập ở thời điểm 49 giờ trước; mọi lần đổi tham số ở giờ thật, để lịch sử tham số của schema test dùng chung
 * vẫn đúng thứ tự thời gian.
 */
class ParameterChangeNoRecalculationIT extends ApprovalTestBase {

    private static final String CODE = "CFG-M15-05";
    private static final String URL = "/api/v1/parameters/" + CODE;

    private void changeParameter(String body) throws Exception {
        user.set(MANAGER);
        String etag = mvc.perform(get(URL)).andReturn().getResponse().getHeader("ETag");
        mvc.perform(post(URL + "/change-value")
                        .with(csrf())
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @AfterEach
    void restoreDefault() throws Exception {
        clock.reset();
        changeParameter("{\"restoreDefault\":true,\"reason\":\"khôi phục sau test FR-017\"}");
    }

    private Map<String, Object> reminderState(long id) {
        return jdbc.queryForMap(
                "SELECT status, version, reminded_at, escalated_at FROM approval_request WHERE id = ?", id);
    }

    private int reminderLogs(long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M15.APPROVAL_REQUEST' AND entity_id = ?"
                        + " AND action = 'REQUEST_REMINDED'",
                Integer.class,
                String.valueOf(id));
    }

    @Test
    void FR_017_changingReminderMark_keepsExistingReminder_noRecalculation() throws Exception {
        clock.reset();
        clock.setTo(time.now().minus(Duration.ofHours(49)));
        long id = pending(time.today().plusDays(30));
        clock.reset();
        runRemindJob();
        assertThat(reminderLogs(id)).isEqualTo(1);
        Map<String, Object> before = reminderState(id);
        assertThat(before.get("reminded_at")).isNotNull();

        changeParameter("{\"value\":{\"value\":72,\"unit\":\"HOURS\"},\"reason\":\"Giãn mốc nhắc\"}");
        runRemindJob();

        assertThat(reminderState(id)).isEqualTo(before);
        assertThat(reminderLogs(id)).isEqualTo(1);
        assertThat(lastAudit(id, "REQUEST_REMINDED", "JSON_UNQUOTE(JSON_EXTRACT(basis, '$.ruleCode'))"))
                .isEqualTo("CFG-M15-05");
        assertThat(events.stream(ApproverReminderDue.class)
                        .filter(e -> e.request().id() == id))
                .hasSize(1);
    }
}
