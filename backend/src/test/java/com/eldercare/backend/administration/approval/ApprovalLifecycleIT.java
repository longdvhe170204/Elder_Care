package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER_2;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.NURSE;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_A;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.approval.api.events.RequestDecided;
import com.eldercare.backend.administration.approval.api.events.RequestSubmitted;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.LocalDate;
import java.util.List;
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
import tools.jackson.databind.JsonNode;
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

    private List<String> auditActions(long id) {
        return jdbc.queryForList(
                "SELECT action FROM audit_log WHERE entity_type = 'M15.APPROVAL_REQUEST' AND entity_id = ? ORDER BY id",
                String.class,
                String.valueOf(id));
    }

    @Test
    void US5_1_create_isDraft_withRequesterTypeContentDateReason() throws Exception {
        JsonNode created = api.create(tomorrow, false);

        assertThat(created.get("status").asString()).isEqualTo("DRAFT");
        assertThat(created.get("displayCode").asString()).matches("YC-\\d{4}-\\d{6}");
        assertThat(created.get("requester").get("id").asLong()).isEqualTo(OFFICE_A.accountId());
        assertThat(created.get("type").asString()).isEqualTo(ApprovalFixtures.TYPE);
        assertThat(created.get("requestedEffectiveDate").asString()).isEqualTo(tomorrow.toString());
        assertThat(created.get("reason").asString()).isEqualTo("Đề nghị mẫu");
        assertThat(created.get("content").has("key")).isTrue();
        mvc.perform(get(ApprovalClient.URL + "/" + created.get("id").asLong()))
                .andExpect(jsonPath("$.allowedCommands", hasItems("edit-content", "submit", "cancel")))
                .andExpect(jsonPath("$.displayCode", matchesPattern("YC-\\d{4}-\\d{6}")));
        assertThat(auditActions(created.get("id").asLong())).containsExactly("REQUEST_CREATED");
    }

    @Test
    void FR_033_draftEditable_withReason_thenLockedAfterSubmit() throws Exception {
        long id = draft();

        api.command(id, "edit-content", api.etag(id), "{\"content\":{\"key\":\"k2\"}}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        api.command(id, "edit-content", api.etag(id), "{\"content\":{\"key\":\"k2\"},\"reason\":\"Sửa khóa\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.key").value("k2"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        api.submit(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());
        api.command(id, "edit-content", api.etag(id), "{\"content\":{\"key\":\"k3\"},\"reason\":\"Sửa sau gửi\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE"));
        assertThat(auditActions(id)).containsExactly("REQUEST_CREATED", "REQUEST_DRAFT_EDITED", "REQUEST_SUBMITTED");
        assertThat(events.stream(RequestSubmitted.class).filter(e -> e.request().id() == id))
                .singleElement()
                .satisfies(e -> assertThat(e.approvers())
                        .contains(MANAGER.accountId(), MANAGER_2.accountId(), OFFICE_B.accountId()));
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

    @Test
    void US5_5_reject_requiresComment_andCreatesNoEffect() throws Exception {
        long id = pending();
        user.set(MANAGER);

        api.reject(id, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        api.reject(id, "Thiếu chứng từ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decisionComment").value("Thiếu chứng từ"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM approval_request WHERE id = ? AND result_type IS NOT NULL",
                        Integer.class,
                        id))
                .isZero();
    }

    @Test
    void BR_M15_08_cancel_requiresReason_requesterForDraft_approverForApproved() throws Exception {
        long draft = draft();
        api.cancel(draft, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        api.cancel(draft, "Không cần nữa")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.closingReason").value("Không cần nữa"));

        long approved = pending();
        user.set(MANAGER);
        api.approve(approved, "Đồng ý").andExpect(status().isOk());
        String etag = api.etag(approved);
        user.set(NURSE);
        api.command(approved, "cancel", etag, "{\"reason\":\"Thử hủy\"}").andExpect(status().isForbidden());
        user.set(MANAGER_2);
        api.cancel(approved, "Hủy trước ngày hiệu lực")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void US5_14_withoutPermissionOrOutOfScope_cannotCreateOrDecide() throws Exception {
        user.set(NURSE);
        api.createRaw("{\"type\":\"M15.SAMPLE\",\"content\":{\"key\":\"x\"},\"requestedEffectiveDate\":\"" + tomorrow
                        + "\",\"reason\":\"Thử\"}")
                .andExpect(status().isForbidden());

        long id = pending();
        String key = api.read(id).get("content").get("key").asString();
        permissions.outOfScope(ApprovalFixtures.TYPE, key);
        String etag = api.etag(id);
        user.set(MANAGER);
        api.command(id, "approve", etag, "{}").andExpect(status().isForbidden());
        user.set(OFFICE_A);
        mvc.perform(get(ApprovalClient.URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("approve"))));
    }

    @Test
    void inbox_mineAndAwaitingMyApproval() throws Exception {
        long id = pending();

        user.set(OFFICE_A);
        mvc.perform(get(ApprovalClient.URL).param("inbox", "MINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")]").exists());
        user.set(MANAGER);
        mvc.perform(get(ApprovalClient.URL)
                        .param("inbox", "AWAITING_MY_APPROVAL")
                        .param("type", "M15.SAMPLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")]").exists());
        mvc.perform(get(ApprovalClient.URL).param("inbox", "MINE"))
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")]").doesNotExist());
    }
}
