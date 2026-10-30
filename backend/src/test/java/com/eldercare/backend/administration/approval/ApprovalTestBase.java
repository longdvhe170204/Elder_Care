package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_A;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.SCHEDULER;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.approval.scheduling.ApplyDueRequestsJob;
import com.eldercare.backend.administration.approval.scheduling.RemindPendingRequestsJob;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Dựng yêu cầu mẫu qua API, chạy job bằng Bộ lập lịch, đồng hồ giả lập (000 T084 → T088, T105). */
@AutoConfigureMockMvc
@WithMockUser
@RecordApplicationEvents
@Import(ApprovalFixtures.class)
abstract class ApprovalTestBase extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    SwitchableUser user;

    @Autowired
    ApprovalFixtures.Control control;

    @Autowired
    TestClockControl clock;

    @Autowired
    BusinessTime time;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEvents events;

    @Autowired
    ApplyDueRequestsJob applyJob;

    @Autowired
    RemindPendingRequestsJob remindJob;

    ApprovalClient api;

    @BeforeEach
    void setUpApproval() {
        api = new ApprovalClient(mvc, json);
        control.reset();
    }

    @AfterEach
    void tearDownApproval() {
        control.reset();
        clock.reset();
        user.set(MANAGER);
    }

    /** Hành chính lập và gửi duyệt; trả id. */
    long pending(LocalDate effectiveDate) throws Exception {
        user.set(OFFICE_A);
        long id = api.create(effectiveDate, false).get("id").asLong();
        api.submit(id).andExpect(status().isOk());
        return id;
    }

    /** Lập, gửi, Quản lý viện duyệt; trả id. */
    long approved(LocalDate effectiveDate) throws Exception {
        long id = pending(effectiveDate);
        user.set(MANAGER);
        api.approve(id, "Đồng ý").andExpect(status().isOk());
        return id;
    }

    JsonNode read(long id) throws Exception {
        user.set(MANAGER);
        return api.read(id);
    }

    /** Đặt đồng hồ tới 00:10 giờ Việt Nam của ngày đã cho. */
    void moveTo(LocalDate date) {
        clock.setTo(date.atTime(LocalTime.of(0, 10)).atZone(BusinessTime.ZONE).toInstant());
    }

    String runApplyJob() {
        user.set(SCHEDULER);
        try {
            return applyJob.run();
        } finally {
            user.set(MANAGER);
        }
    }

    String runRemindJob() {
        user.set(SCHEDULER);
        try {
            return remindJob.run();
        } finally {
            user.set(MANAGER);
        }
    }

    int effects(long id) throws Exception {
        String key = read(id).get("content").get("key").asString();
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM sample_catalog WHERE name = ?", Integer.class, "Tác động " + key);
    }

    int results(long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE id = ? AND result_type IS NOT NULL", Integer.class, id);
    }

    String lastAudit(long id, String action, String column) {
        return jdbc.queryForObject(
                "SELECT " + column + " FROM audit_log WHERE entity_type = 'M15.APPROVAL_REQUEST' AND entity_id = ?"
                        + " AND action = ? ORDER BY id DESC LIMIT 1",
                String.class,
                String.valueOf(id),
                action);
    }
}
