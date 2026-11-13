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

    private void submit(long id) throws Exception {
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", "\"0\""), items()))
                .andExpect(status().isOk());
        mvc.perform(write(
                        post("/api/v1/care-plans/" + id + "/submit"),
                        "{\"effectiveFrom\":\"" + time.today().plusDays(1) + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void FR_003_createPreventsSecondOpenRevisionAndEmptySubmission() throws Exception {
        long id = create();
        mvc.perform(write(post("/api/v1/residents/" + residentId + "/care-plans"), "{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("OPEN_REVISION_EXISTS"));
        mvc.perform(write(
                        post("/api/v1/care-plans/" + id + "/submit"),
                        "{\"effectiveFrom\":\"" + time.today().plusDays(1) + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void FR_003_submitApproveAndWithdrawKeepItemsLockedAndAuditAtomic() throws Exception {
        long id = create();
        submit(id);
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", "\"2\""), items()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REVISION_LOCKED"));
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/approve"), "{}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/care-plans/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_EFFECT"))
                .andExpect(jsonPath("$.selfApproved").value(false));
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/withdraw-approval"), "{\"reason\":\"Cần xem lại\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM care_plan WHERE id=?", String.class, id))
                .isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT approved_by FROM care_plan WHERE id=?", Long.class, id))
                .isEqualTo(doctor.accountId());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M04.CARE_PLAN' AND entity_id=?",
                        Integer.class,
                        Long.toString(id)))
                .isEqualTo(5);
    }

    /**
     * DBR-11, BR-M04-19: phiên bản đang hiệu lực không sửa; phiên bản mới đã duyệt hiệu lực thì bản cũ hết hạn ngày trước
     * đó, mỗi ngày tối đa một phiên bản hiệu lực. BR-M04-03: báo Người phụ trách ca và Trưởng tầng rằng kế hoạch đã đổi.
     */
    @Test
    void BR_M04_19_DBR_11_newApprovedRevisionReplacesLockedEffectiveOneAndNotifiesShift() throws Exception {
        var first = time.today().plusDays(200 + TestUsers.nextId() % 300);
        var activation = context.getBean(com.eldercare.backend.care.internal.careplan.service.CarePlanActivation.class);
        long v1 = approved(first);
        activation.run(first, time.now());
        user.set(nurse.user());
        var version = jdbc.queryForObject("SELECT version FROM care_plan WHERE id=?", Long.class, v1);
        mvc.perform(write(
                        put("/api/v1/care-plans/" + v1 + "/items").header("If-Match", "\"" + version + "\""), items()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REVISION_LOCKED"));
        long v2 = approved(first.plusDays(3));
        activation.run(first.plusDays(3), time.now());

        assertThat(jdbc.queryForList(
                        "SELECT status FROM care_plan WHERE resident_id=? ORDER BY id", String.class, residentId))
                .containsExactly("EXPIRED", "EFFECTIVE");
        assertThat(jdbc.queryForObject("SELECT effective_to FROM care_plan WHERE id=?", java.sql.Date.class, v1)
                        .toLocalDate())
                .isEqualTo(first.plusDays(2));
        var notice = recorded.stream(com.eldercare.backend.care.api.events.CarePlanChanged.class)
                .filter(e -> e.newVersionId() == v2)
                .findFirst()
                .orElseThrow();
        assertThat(notice.previousVersionId()).contains(v1);
        assertThat(notice.context().recipients())
                .extracting(r -> r.getClass().getSimpleName())
                .containsExactlyInAnyOrder("ShiftLead", "FloorManager");
    }

    private long approved(java.time.LocalDate effectiveFrom) throws Exception {
        long id = create();
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", "\"0\""), items()))
                .andExpect(status().isOk());
        mvc.perform(write(
                        post("/api/v1/care-plans/" + id + "/submit"),
                        "{\"effectiveFrom\":\"" + effectiveFrom + "\",\"changeReason\":\"Điều chỉnh kế hoạch\"}"))
                .andExpect(status().isOk());
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/approve"), "{}")).andExpect(status().isOk());
        return id;
    }

    private String agreement() {
        return "[{\"choice\":\"Lựa chọn kiểm thử\",\"discussedRisks\":\"Đã trao đổi rủi ro\",\"residentParticipates\":true,\"participants\":[{\"type\":\"STAFF\",\"id\":"
                + nurse.staffId() + "}],\"assessingDoctorId\":" + doctor.staffId()
                + ",\"selfDeterminationAssessment\":{\"canSelfDetermine\":true,\"supportMethod\":\"Giải thích bằng lời dễ hiểu\",\"reviewDate\":\""
                + time.today().plusDays(30)
                + "\"},\"decisionMaker\":{\"type\":\"RESIDENT\"},\"opinions\":[{\"source\":{\"type\":\"RESIDENT\"},\"content\":\"Ý kiến kiểm thử\"}],\"reviewDate\":\""
                + time.today().plusDays(30) + "\"}]";
    }

    @Test
    void FR_004_nullArrayMembersAreRejectedWithoutPartialWrites() {
        com.eldercare.backend.support.TableRows.each(
                java.util.stream.Stream.of(
                        "nullItem", "nullRisk", "nullTime", "nullWeekday", "nullParticipant", "nullOpinion"),
                this::nullMemberRejected);
    }

    /** Mỗi dòng dựng người cao tuổi riêng như một ca của test tham số trước đây. */
    private void nullMemberRejected(String sample) throws Exception {
        setup();
        long id = create();
        boolean risk =
                java.util.Set.of("nullRisk", "nullParticipant", "nullOpinion").contains(sample);
        String body = switch (sample) {
            case "nullItem", "nullRisk" -> "[null]";
            case "nullTime" -> items().replace("\"billable\":false", "\"billable\":false,\"times\":[null]");
            case "nullWeekday" -> items().replace("\"billable\":false", "\"billable\":false,\"weekdays\":[null]");
            case "nullParticipant" ->
                agreement()
                        .replace(
                                "\"participants\":[{\"type\":\"STAFF\",\"id\":" + nurse.staffId() + "}]",
                                "\"participants\":[null]");
            default ->
                agreement()
                        .replace(
                                "\"opinions\":[{\"source\":{\"type\":\"RESIDENT\"},\"content\":\"Ý kiến kiểm thử\"}]",
                                "\"opinions\":[null]");
        };
        mvc.perform(write(
                        put("/api/v1/care-plans/" + id + (risk ? "/risk-agreements" : "/items"))
                                .header("If-Match", "\"0\""),
                        body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_plan_item WHERE care_plan_id=?", Integer.class, id))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM care_plan WHERE id=?", Integer.class, id))
                .isZero();
    }
}
