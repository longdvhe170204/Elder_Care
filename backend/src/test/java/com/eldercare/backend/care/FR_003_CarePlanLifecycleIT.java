package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.care.internal.scheduling.service.CareDailyWork;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.fake.administration.*;
import com.eldercare.backend.support.fake.resident.*;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** T011: HTTP thật, quyền thật, MySQL; chỉ dùng fake nguồn do chủ module công bố. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@org.springframework.test.context.event.RecordApplicationEvents
@Import({SwitchableUserConfig.class, FakeM15Config.class, FakeM01Config.class})
class FR_003_CarePlanLifecycleIT extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeResidents residents;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    JsonMapper json;

    @Autowired
    org.springframework.beans.factory.ObjectProvider<CareDailyWork> dailyWork;

    @Autowired
    org.springframework.context.ApplicationContext context;

    @Autowired
    org.springframework.test.context.event.ApplicationEvents recorded;

    @Autowired
    com.eldercare.backend.administration.parameter.api.ParameterApi parameters;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    com.eldercare.backend.care.api.CarePlanReviewApi reviewApi;

    long residentId, taskTypeId;
    long floorAreaId;
    TestUsers.TestUser nurse, doctor;

    @BeforeEach
    void setup() {
        residentId = TestUsers.nextId();
        floorAreaId = TestUsers.nextId();
        bedAllocations.allocate(
                residentId,
                new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floorAreaId));
        residents.resident(residentId, "[Họ tên kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        var users = new TestUsers(jdbc, time);
        nurse = users.staff(Role.NURSING_STAFF);
        doctor = users.staff(Role.DOCTOR);
        assignments.assign(
                nurse.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        residentId,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
        user.set(nurse.user());
        taskTypeId = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                taskTypeId,
                "Loại kiểm thử " + taskTypeId,
                "{\"group\":\"OTHER\",\"professional\":false,\"resultType\":\"PERFORMED\",\"defaultRole\":\"CAREGIVER\",\"defaultImportance\":\"NORMAL\",\"billable\":false,\"certificates\":[]}");
    }

    private MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, String body) {
        return request.header("Idempotency-Key", "plan-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private long create() throws Exception {
        var result = mvc.perform(
                        write(post("/api/v1/residents/" + residentId + "/care-plans"), "{\"copyCurrent\":false}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    private String items() {
        return "[{\"taskTypeId\":" + taskTypeId
                + ",\"content\":\"Nội dung kiểm thử\",\"frequencyType\":\"ONCE_PER_SHIFT\",\"performerRole\":\"CAREGIVER\",\"resultType\":\"PERFORMED\",\"importance\":\"NORMAL\",\"billable\":false}]";
    }

    private String etag(long id) throws Exception {
        return mvc.perform(get("/api/v1/care-plans/" + id))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
    }

    private void submit(long id) throws Exception {
        submit(id, "{}");
    }

    private void submit(long id, String body) throws Exception {
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", etag(id)), items()))
                .andExpect(status().isOk());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/submit"), body)).andExpect(status().isOk());
    }

    private long createCopy() throws Exception {
        var result = mvc.perform(write(post("/api/v1/residents/" + residentId + "/care-plans"), "{}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    private void approveAsDoctor(long id) throws Exception {
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/approve"), "{}")).andExpect(status().isOk());
        user.set(nurse.user());
    }

    /** Kế hoạch Hiệu lực đầu tiên của người cao tuổi, lập và duyệt qua API. */
    private long effectivePlan() throws Exception {
        long id = create();
        submit(id);
        approveAsDoctor(id);
        return id;
    }

    private String column(String name, long id) {
        return jdbc.queryForObject("SELECT " + name + " FROM care_plan WHERE id=?", String.class, id);
    }

    private com.eldercare.backend.care.internal.careplan.service.CarePlanActivation activation() {
        return context.getBean(com.eldercare.backend.care.internal.careplan.service.CarePlanActivation.class);
    }

    @Test
    void FR_003_createPreventsSecondOpenRevisionAndEmptySubmission() throws Exception {
        long id = create();
        mvc.perform(write(post("/api/v1/residents/" + residentId + "/care-plans"), "{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("OPEN_REVISION_EXISTS"));
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/submit"), "{}")).andExpect(status().isConflict());
    }

    @Test
    void Q447_firstApprovalTakesEffectToday() throws Exception {
        long id = create();
        submit(id);
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", etag(id)), items()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REVISION_LOCKED"));
        approveAsDoctor(id);
        assertThat(column("status", id)).isEqualTo("EFFECTIVE");
        assertThat(column("effective_from", id)).isEqualTo(time.today().toString());
        assertThat(column("self_approved", id)).isEqualTo("0");
        mvc.perform(get("/api/v1/residents/" + residentId + "/care-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].noEffectivePlan").value(false));
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.CarePlanChanged.class)
                        .filter(e -> e.residentId() == residentId))
                .hasSize(1);
    }

    @Test
    void Q447_selfApprovalOfFirstPlanNeedsReason() throws Exception {
        var both = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF, Role.DOCTOR);
        assignments.assign(
                both.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        residentId,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
        user.set(both.user());
        long id = create();
        submit(id);
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/approve"), "{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/approve"), "{\"reason\":\"Tự duyệt khi Bác sĩ vắng\"}"))
                .andExpect(status().isOk());
        assertThat(column("status", id)).isEqualTo("EFFECTIVE");
        assertThat(column("self_approved", id)).isEqualTo("1");
    }

    @Test
    void Q447_revisionApprovedStartsTomorrowAndKeepsCurrentPlan() throws Exception {
        long current = effectivePlan();
        long revision = createCopy();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM care_plan_item WHERE care_plan_id=?", Integer.class, revision))
                .isEqualTo(1);
        mvc.perform(write(post("/api/v1/care-plans/" + revision + "/submit"), "{}"))
                .andExpect(status().isUnprocessableContent());
        submit(revision, "{\"changeReason\":\"Sau ngã cần xoay trở dày hơn\"}");
        approveAsDoctor(revision);
        assertThat(column("status", revision)).isEqualTo("PENDING_EFFECT");
        assertThat(column("effective_from", revision))
                .isEqualTo(time.today().plusDays(1).toString());
        assertThat(column("status", current)).isEqualTo("EFFECTIVE");
    }

    @Test
    void Q447_secondApprovalSameDayStartsTomorrowAndActivationKeepsPeriodValid() throws Exception {
        long current = effectivePlan();
        long revision = createCopy();
        submit(revision, "{\"changeReason\":\"Đổi giờ uống nước\"}");
        approveAsDoctor(revision);
        activation().run(time.today().plusDays(1), time.now());
        assertThat(column("status", current)).isEqualTo("EXPIRED");
        assertThat(column("effective_to", current)).isEqualTo(time.today().toString());
        assertThat(column("status", revision)).isEqualTo("EFFECTIVE");
    }

    @Test
    void Q447_editWhilePendingEffectSupersedesIt() throws Exception {
        effectivePlan();
        long first = createCopy();
        submit(first, "{\"changeReason\":\"Lần sửa thứ nhất\"}");
        approveAsDoctor(first);
        long second = createCopy();
        assertThat(jdbc.queryForObject("SELECT previous_plan_id FROM care_plan WHERE id=?", Long.class, second))
                .isEqualTo(first);
        submit(second, "{\"changeReason\":\"Lần sửa thứ hai\"}");
        approveAsDoctor(second);
        assertThat(column("status", first)).isEqualTo("CANCELLED");
        assertThat(column("end_reason", first)).isEqualTo("SUPERSEDED");
        assertThat(column("status", second)).isEqualTo("PENDING_EFFECT");
    }

    @Test
    void Q447_approveCancelsStalePendingEffectEvenWithoutEffective() throws Exception {
        var now = java.time.LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE);
        jdbc.update(
                "INSERT INTO care_plan(resident_id,revision,status,effective_from,created_by,created_at,approved_by,approved_at)"
                        + " VALUES(?,1,'PENDING_EFFECT',?,?,?,?,?)",
                residentId,
                time.today().plusDays(30),
                nurse.accountId(),
                now,
                doctor.accountId(),
                now);
        long stale = jdbc.queryForObject(
                "SELECT id FROM care_plan WHERE resident_id=? AND revision=1", Long.class, residentId);
        long id = createCopy();
        submit(id, "{\"changeReason\":\"Lập lại theo luật mới\"}");
        approveAsDoctor(id);
        assertThat(column("status", id)).isEqualTo("EFFECTIVE");
        assertThat(column("effective_from", id)).isEqualTo(time.today().toString());
        assertThat(column("status", stale)).isEqualTo("CANCELLED");
    }

    @Test
    void Q447_retractOnlyByCreator() throws Exception {
        long id = create();
        submit(id);
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/retract"), "{}")).andExpect(status().isForbidden());
        user.set(nurse.user());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/retract"), "{}")).andExpect(status().isOk());
        assertThat(column("status", id)).isEqualTo("DRAFT");
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", etag(id)), items()))
                .andExpect(status().isOk());
    }

    @Test
    void Q447_withdrawEndpointIsGone() throws Exception {
        long id = create();
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/withdraw-approval"), "{\"reason\":\"Cần xem lại\"}"))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions dismiss(String body) throws Exception {
        return mvc.perform(write(post("/api/v1/residents/" + residentId + "/care-plan-reviews/dismiss"), body));
    }

    @Test
    void Q448_dismissNeedsReasonClosesOpenReviewsThenNothingLeft() throws Exception {
        reviewApi.fallIncidentRecorded(residentId, TestUsers.nextId());

        dismiss("{}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        dismiss("{\"reason\":\"Đã xem, kế hoạch hiện hành vẫn phù hợp\"}").andExpect(status().isOk());
        dismiss("{\"reason\":\"Lần hai\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NO_OPEN_REVIEW"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'CARE_PLAN_REVIEW_DISMISSED' AND entity_id = ?",
                        Integer.class,
                        residentId))
                .isEqualTo(1);
    }

    @Test
    void Q448_approvedPlanCreatedAfterRaiseClosesReview() throws Exception {
        reviewApi.fallIncidentRecorded(residentId, TestUsers.nextId());

        effectivePlan();

        dismiss("{\"reason\":\"Không còn gì để xem\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NO_OPEN_REVIEW"));
    }

    @Test
    void Q448_approvingDraftCreatedBeforeIncidentKeepsReviewOpen() throws Exception {
        long id = create();
        reviewApi.fallIncidentRecorded(residentId, TestUsers.nextId());
        submit(id);
        approveAsDoctor(id);

        dismiss("{\"reason\":\"Bản vừa duyệt lập trước khi ngã, đã xem lại\"}").andExpect(status().isOk());
    }

    @Test
    void Q448_dismissOutsideScopeForbidden() throws Exception {
        reviewApi.fallIncidentRecorded(residentId, TestUsers.nextId());
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());

        // Ngoài phạm vi: không lộ hồ sơ, như các lệnh khác
        dismiss("{\"reason\":\"Không thuộc phạm vi\"}").andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind = 'M04.CARE_PLAN_REVIEW_DISMISSED' AND entity_id = ?",
                        Integer.class,
                        residentId))
                .isZero();
    }

    private org.springframework.test.web.servlet.ResultActions queue() throws Exception {
        return mvc.perform(get("/api/v1/care-plan-reviews/queue")).andExpect(status().isOk());
    }

    private String row() {
        return "$[?(@.residentId==" + residentId + ")]";
    }

    @Test
    void Q448_fallPutsResidentInQueueUntilRevisedPlanIsApproved() throws Exception {
        long incident = TestUsers.nextId();
        reviewApi.fallIncidentRecorded(residentId, incident);

        queue().andExpect(jsonPath(row() + ".reasons[0].source").value("M05.INCIDENT"))
                .andExpect(jsonPath(row() + ".reasons[0].reference").value(Long.toString(incident)))
                .andExpect(jsonPath(row() + ".profileCode").value("NCT-2026-" + residentId))
                .andExpect(jsonPath(row() + ".draftId").value(org.hamcrest.Matchers.contains((Object) null)));
        mvc.perform(get("/api/v1/residents/" + residentId + "/care-plan-reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("M05.INCIDENT"));

        long id = create();
        queue().andExpect(jsonPath(row() + ".draftId").value(org.hamcrest.Matchers.contains((int) id)));
        submit(id);
        approveAsDoctor(id);

        queue().andExpect(jsonPath(row()).isEmpty());
        mvc.perform(get("/api/v1/residents/" + residentId + "/care-plan-reviews"))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void Q448_returnedDraftIsQueuedAsReturned() throws Exception {
        long id = create();
        submit(id);
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/return"), "{\"reason\":\"Thiếu mục uống nước\"}"))
                .andExpect(status().isOk());
        user.set(nurse.user());

        queue().andExpect(jsonPath(row() + ".draftReturned").value(org.hamcrest.Matchers.contains(true)))
                .andExpect(jsonPath(row() + ".reasons[*]").isEmpty());
    }

    @Test
    void Q448_queueHidesResidentsOutsideScope() throws Exception {
        reviewApi.fallIncidentRecorded(residentId, TestUsers.nextId());
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());

        queue().andExpect(jsonPath(row()).isEmpty());
    }

    @Test
    void Q449_creatorDiscardsOwnDraftWithoutReason() throws Exception {
        long id = create();
        mvc.perform(get("/api/v1/care-plans/" + id))
                .andExpect(jsonPath("$.allowedCommands").value(org.hamcrest.Matchers.hasItem("discardDraft")))
                .andExpect(jsonPath("$.allowedCommands")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("cancel"))));

        mvc.perform(write(post("/api/v1/care-plans/" + id + "/cancel"), "{}")).andExpect(status().isOk());

        assertThat(column("status", id)).isEqualTo("CANCELLED");
    }

    @Test
    void Q449_cancellingPendingApprovalStillNeedsReason() throws Exception {
        long id = create();
        submit(id);

        mvc.perform(write(post("/api/v1/care-plans/" + id + "/cancel"), "{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/cancel"), "{\"reason\":\"Gửi nhầm\"}"))
                .andExpect(status().isOk());
        assertThat(column("status", id)).isEqualTo("CANCELLED");
    }

    @Test
    void Q449_approverCancellingSomeoneElsesDraftNeedsReason() throws Exception {
        long id = create();
        user.set(doctor.user());

        mvc.perform(write(post("/api/v1/care-plans/" + id + "/cancel"), "{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
    }
}
