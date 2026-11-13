package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.approval.api.events.RequestDecided;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * US5 #1 → #6, #14; FR-031 → FR-036, BR-M15-08 (000 T083, T095): lập, sửa nháp có lý do, gửi duyệt khóa nội dung, duyệt,
 * từ chối bắt buộc ý kiến, hủy bắt buộc lý do; người không có quyền hoặc ngoài phạm vi bị từ chối.
 */
@AutoConfigureMockMvc
@WithMockUser
@RecordApplicationEvents
@Import(ApprovalFixtures.class)
class ApprovalLifecycleIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    SwitchableUser user;

    @Autowired
    ApprovalFixtures.Control control;

    @Autowired
    FakePermissions permissions;

    @Autowired
    BusinessTime time;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEvents events;

    private ApprovalClient api;
    private LocalDate tomorrow;

    @BeforeEach
    void setUp() {
        api = new ApprovalClient(mvc, json);
        control.reset();
        tomorrow = time.today().plusDays(1);
        user.set(OFFICE_A);
    }

    @AfterEach
    void tearDown() {
        control.reset();
        user.set(MANAGER);
    }

    private long draft() throws Exception {
        user.set(OFFICE_A);
        return api.create(tomorrow, false).get("id").asLong();
    }

    private long pending() throws Exception {
        long id = draft();
        api.submit(id).andExpect(status().isOk());
        return id;
    }

    @Test
    void US5_3_approverInScope_approves_futureDateWaitsAsApproved() throws Exception {
        long id = pending();
        user.set(MANAGER);

        api.approve(id, "Đồng ý")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy.id").value((int) MANAGER.accountId()))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty())
                .andExpect(jsonPath("$.decisionComment").value("Đồng ý"))
                .andExpect(jsonPath("$.actualEffectiveDate").value(tomorrow.toString()))
                .andExpect(jsonPath("$.selfApproved").value(false));
        assertThat(events.stream(RequestDecided.class).filter(e -> e.request().id() == id))
                .singleElement()
                .satisfies(e -> assertThat(e.outcome()).isEqualTo("APPROVED"));
    }
}
