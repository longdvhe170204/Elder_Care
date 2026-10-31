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
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class, FakeM01Config.class})
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
    void FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage() {
        var metadata = new com.eldercare.backend.care.api.dto.CareEventContext(
                "M04.CARE_PLAN",
                7,
                "RETURN:2",
                time.now(),
                java.util.Optional.empty(),
                java.util.List.of(new com.eldercare.backend.common.notification.RecipientGroup.Staff(4, "CREATOR")));
        var event = new com.eldercare.backend.care.api.events.CarePlanReturned(
                7, residentId, metadata, new com.eldercare.backend.common.Reason("\"".repeat(2000)).value());
        String publicationId = java.util.UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO EVENT_PUBLICATION (ID,LISTENER_ID,EVENT_TYPE,SERIALIZED_EVENT,PUBLICATION_DATE,COMPLETION_DATE,STATUS)"
                        + " VALUES (?,?,?,?,?,?,'COMPLETED')",
                publicationId,
                "FR_003",
                event.getClass().getName(),
                json.writeValueAsString(event),
                java.sql.Timestamp.from(time.now()),
                java.sql.Timestamp.from(time.now()));
        var replay = json.readValue(
                jdbc.queryForObject(
                        "SELECT SERIALIZED_EVENT FROM EVENT_PUBLICATION WHERE ID=?", String.class, publicationId),
                com.eldercare.backend.care.api.events.CarePlanReturned.class);
        assertThat(replay.reason()).isEqualTo(event.reason());
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

    @Test
    void FR_004_staleItemVersionDoesNotOverwriteAndReturnAllowsEditing() throws Exception {
        long id = create();
        submit(id);
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/return"), "{\"reason\":\"Bổ sung nội dung\"}"))
                .andExpect(status().isOk());
        var returned = recorded.stream(com.eldercare.backend.care.api.events.CarePlanReturned.class)
                .filter(e -> e.planId() == id)
                .findFirst()
                .orElseThrow();
        var notice = returned.notificationRequests().getFirst();
        assertThat(notice.content())
                .contains(new com.eldercare.backend.common.notification.ContentPart(
                        com.eldercare.backend.common.notification.InformationType.HEALTH, "Bổ sung nội dung"));
        assertThat(notice.title().orElse("")).doesNotContain("Bổ sung nội dung");
        assertThat(notice.shortContent().orElse("")).doesNotContain("Bổ sung nội dung");
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.CarePlanReturned.class)
                        .filter(e -> e.planId() == id)
                        .findFirst()
                        .orElseThrow()
                        .context()
                        .recipients())
                .contains(new com.eldercare.backend.common.notification.RecipientGroup.Staff(
                        nurse.staffId(), "CARE_PLAN_CREATOR"));
        user.set(nurse.user());
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", "\"0\""), items()))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", "\"3\""), items()))
                .andExpect(status().isOk());
    }

    @Test
    void FR_007_officeCannotViewAndUnassignedNurseCannotCreate() throws Exception {
        long id = create();
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        mvc.perform(get("/api/v1/care-plans/" + id)).andExpect(status().isForbidden());
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());
        mvc.perform(write(post("/api/v1/residents/" + residentId + "/care-plans"), "{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void FR_039_onlyOneItemInARevisionMaySpecifyTheDailyWaterTarget() throws Exception {
        long id = create();
        jdbc.update(
                "UPDATE catalog_item SET attributes=JSON_SET(attributes,'$.group','DRINKING_ASSISTANCE') WHERE id=?",
                taskTypeId);
        String water = items().substring(1, items().length() - 1)
                .replace("\"billable\":false", "\"billable\":false,\"waterTargetMl\":1500");
        mvc.perform(write(
                        put("/api/v1/care-plans/" + id + "/items").header("If-Match", "\"0\""),
                        "[" + water + "," + water + "]"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_plan_item WHERE care_plan_id=?", Integer.class, id))
                .isZero();
    }

    @Test
    void FR_005_selfApprovalRequiresReasonAndMarksTheRevision() throws Exception {
        var combined = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF, Role.DOCTOR);
        assignments.assign(
                combined.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        residentId,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
        user.set(combined.user());
        long id = create();
        submit(id);
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/approve"), "{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/approve"), "{\"reason\":\"Đã tự kiểm tra đầy đủ\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/care-plans/" + id))
                .andExpect(jsonPath("$.selfApproved").value(true));
    }

    @Test
    void FR_004_approvalRechecksProposedDateWithoutChangingIt() throws Exception {
        long id = create();
        submit(id);
        jdbc.update("UPDATE care_plan SET effective_from=? WHERE id=?", time.today(), id);
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/approve"), "{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_EFFECTIVE_DATE"));
        assertThat(jdbc.queryForObject("SELECT status FROM care_plan WHERE id=?", String.class, id))
                .isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void FR_006_activationExpiresPreviousRevisionInOneTransactionAndKeepsHistoryReadable() throws Exception {
        long first = create();
        submit(first);
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + first + "/approve"), "{}"))
                .andExpect(status().isOk());
        var step = dailyWork
                .orderedStream()
                .filter(work -> work.name().equals("CARE_PLAN_EFFECTIVE"))
                .findFirst();
        assertThat(step).as("T015 đăng ký bước hiệu lực trên khung job hiện có").isPresent();
        var day = time.today().plusDays(1);
        assertThat(step.get().run(day, time.startOfDay(day))).isEqualTo(1);
        var notice = recorded.stream(com.eldercare.backend.care.api.events.CarePlanChanged.class)
                .filter(e -> e.newVersionId() == first)
                .findFirst()
                .orElseThrow();
        assertThat(notice.context().recipients())
                .contains(
                        new com.eldercare.backend.common.notification.RecipientGroup.ShiftLead(
                                floorAreaId, new com.eldercare.backend.common.notification.Substitution.Default()),
                        new com.eldercare.backend.common.notification.RecipientGroup.FloorManager(
                                floorAreaId, new com.eldercare.backend.common.notification.Substitution.Default()));
        user.set(nurse.user());
        var created = mvc.perform(write(post("/api/v1/residents/" + residentId + "/care-plans"), "{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn();
        long second = json.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();
        mvc.perform(write(
                        post("/api/v1/care-plans/" + second + "/submit"),
                        "{\"effectiveFrom\":\"" + day.plusDays(1) + "\",\"changeReason\":\"Điều chỉnh chăm sóc\"}"))
                .andExpect(status().isOk());
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + second + "/approve"), "{}"))
                .andExpect(status().isOk());
        assertThat(step.get().run(day.plusDays(1), time.startOfDay(day.plusDays(1))))
                .isEqualTo(1);
        var api = context.getBean(com.eldercare.backend.care.internal.careplan.service.CarePlanQueries.class);
        assertThat(api.effectiveOn(residentId, day).orElseThrow().id()).isEqualTo(first);
        assertThat(api.effectiveOn(residentId, day.plusDays(1)).orElseThrow().id())
                .isEqualTo(second);
        assertThat(jdbc.queryForObject(
                        "SELECT effective_to FROM care_plan WHERE id=?", java.time.LocalDate.class, first))
                .isEqualTo(day);
        assertThat(step.get().run(day.plusDays(1), time.startOfDay(day.plusDays(1))))
                .isZero();
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
    void FR_002a_riskAgreementUsesSharedChildrenAndCannotReplaceTaskItems() throws Exception {
        long id = create();
        mvc.perform(write(
                        put("/api/v1/care-plans/" + id + "/risk-agreements").header("If-Match", "\"0\""), agreement()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/care-plans/" + id))
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(
                        jsonPath("$.riskAgreements[0].opinions[0].source.type").value("RESIDENT"))
                .andExpect(jsonPath("$.riskAgreements[0].selfDeterminationAssessment.at")
                        .isNotEmpty());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event e JOIN care_plan_item i ON i.id=e.entity_id WHERE e.entity_type='M04.RISK_AGREEMENT' AND i.care_plan_id=?",
                        Integer.class,
                        id))
                .isEqualTo(2);
        mvc.perform(write(
                        post("/api/v1/care-plans/" + id + "/submit"),
                        "{\"effectiveFrom\":\"" + time.today().plusDays(1) + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void FR_002a_invalidDoctorCapacityAndOpinionSourceAreRejected() throws Exception {
        long id = create();
        for (String invalid : java.util.List.of(
                agreement()
                        .replace(
                                "\"assessingDoctorId\":" + doctor.staffId(),
                                "\"assessingDoctorId\":" + nurse.staffId()),
                agreement().replace("\"canSelfDetermine\":true", "\"canSelfDetermine\":false"),
                agreement().replace("\"supportMethod\":\"Giải thích bằng lời dễ hiểu\"", "\"supportMethod\":\"\""),
                agreement()
                        .replace(
                                "\"source\":{\"type\":\"RESIDENT\"}",
                                "\"source\":{\"type\":\"FAMILY_MEMBER\",\"familyMemberId\":" + TestUsers.nextId()
                                        + "}"))) {
            mvc.perform(write(
                            put("/api/v1/care-plans/" + id + "/risk-agreements").header("If-Match", "\"0\""), invalid))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Test
    void FR_008_compareCanReadDraftsAndPreservesNullableWindows() throws Exception {
        long first = create();
        mvc.perform(write(put("/api/v1/care-plans/" + first + "/items").header("If-Match", "\"0\""), items()))
                .andExpect(status().isOk());
        mvc.perform(write(post("/api/v1/care-plans/" + first + "/cancel"), "{\"reason\":\"Lập lại\"}"))
                .andExpect(status().isOk());
        long second = create();
        mvc.perform(get("/api/v1/care-plans/compare")
                        .param("from", Long.toString(first))
                        .param("to", Long.toString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.removed.length()").value(1))
                .andExpect(jsonPath("$.added.length()").value(0));
    }

    @Test
    void FR_005_pendingApprovalRemindersUseConfiguredDurationsAndDoNotRepeat() throws Exception {
        long id = create();
        submit(id);
        var work = dailyWork
                .orderedStream()
                .filter(w -> w.name().equals("CARE_PLAN_APPROVAL_REMINDER"))
                .findFirst();
        assertThat(work).isPresent();
        var remind = parameters
                .read(com.eldercare.backend.administration.parameter.api.ParameterCode.CFG_M15_05)
                .asDuration();
        var escalate = parameters
                .read(com.eldercare.backend.administration.parameter.api.ParameterCode.CFG_M15_06)
                .asDuration();
        jdbc.update(
                "UPDATE care_plan SET submitted_at=? WHERE id=?",
                java.time.LocalDateTime.ofInstant(time.now().minus(remind).minusSeconds(1), BusinessTime.ZONE),
                id);
        work.get().run(time.today(), time.now());
        work.get().run(time.today(), time.now());
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.CarePlanApprovalOverdue.class)
                        .filter(e -> e.planId() == id)
                        .count())
                .isGreaterThanOrEqualTo(1);
        long count = recorded.stream(com.eldercare.backend.care.api.events.CarePlanApprovalOverdue.class)
                .filter(e -> e.planId() == id)
                .count();
        work.get().run(time.today(), time.now());
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.CarePlanApprovalOverdue.class)
                        .filter(e -> e.planId() == id)
                        .count())
                .isEqualTo(count);
        jdbc.update(
                "UPDATE care_plan SET submitted_at=? WHERE id=?",
                java.time.LocalDateTime.ofInstant(time.now().minus(escalate).minusSeconds(1), BusinessTime.ZONE),
                id);
        work.get().run(time.today(), time.now());
        work.get().run(time.today(), time.now());
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.CarePlanManagerEscalation.class)
                        .filter(e -> e.planId() == id)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void FR_002a_copyLocksSourceAndReviewReminderOnlyPublishesOnceForEachReviewDate() throws Exception {
        long id = create();
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", "\"0\""), items()))
                .andExpect(status().isOk());
        mvc.perform(write(
                        put("/api/v1/care-plans/" + id + "/risk-agreements").header("If-Match", "\"1\""), agreement()))
                .andExpect(status().isOk());
        mvc.perform(write(
                        post("/api/v1/care-plans/" + id + "/submit"),
                        "{\"effectiveFrom\":\"" + time.today().plusDays(1) + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(write(
                        put("/api/v1/care-plans/" + id + "/risk-agreements").header("If-Match", "\"3\""), agreement()))
                .andExpect(status().isConflict());
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/approve"), "{}")).andExpect(status().isOk());
        var activation = dailyWork
                .orderedStream()
                .filter(w -> w.name().equals("CARE_PLAN_EFFECTIVE"))
                .findFirst()
                .orElseThrow();
        activation.run(time.today().plusDays(1), time.startOfDay(time.today().plusDays(1)));
        var review = dailyWork
                .orderedStream()
                .filter(w -> w.name().equals("CARE_RISK_REVIEW"))
                .findFirst();
        assertThat(review).isPresent();
        var date = time.today().plusDays(30);
        review.get().run(date, time.startOfDay(date));
        review.get().run(date, time.startOfDay(date));
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.RiskAgreementReviewReminder.class)
                        .count())
                .isEqualTo(2);
        user.set(nurse.user());
        mvc.perform(write(post("/api/v1/residents/" + residentId + "/care-plans"), "{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskAgreements.length()").value(1))
                .andExpect(jsonPath("$.riskAgreements[0].sourceAgreementId").isNotEmpty())
                .andExpect(jsonPath("$.riskAgreements[0].opinions[0].content").value("Ý kiến kiểm thử"));
    }

    @Test
    void FR_003_itemEditsKeepBeforeAndAfterContentInTheAuditHistory() throws Exception {
        long id = create();
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", "\"0\""), items()))
                .andExpect(status().isOk());
        long itemId = jdbc.queryForObject(
                "SELECT id FROM care_plan_item WHERE care_plan_id=? AND kind='TASK' AND active", Long.class, id);
        String updated = items().replace("{\"taskTypeId\":", "{\"id\":" + itemId + ",\"taskTypeId\":")
                .replace("Nội dung kiểm thử", "Nội dung đã sửa");
        mvc.perform(write(put("/api/v1/care-plans/" + id + "/items").header("If-Match", "\"1\""), updated))
                .andExpect(status().isOk());
        var history = jdbc.queryForMap(
                "SELECT data_before,data_after FROM audit_log WHERE entity_type='M04.CARE_PLAN' AND entity_id=? AND action='CARE_PLAN_ITEMS_REPLACED' ORDER BY id DESC LIMIT 1",
                Long.toString(id));
        assertThat(history.get("data_before").toString()).contains("Nội dung kiểm thử");
        assertThat(history.get("data_after").toString()).contains("Nội dung đã sửa");
    }

    @Test
    void FR_003_dailyActivationCannotActivateAPlanForAResidentInFinalStatus() throws Exception {
        long id = create();
        submit(id);
        user.set(doctor.user());
        mvc.perform(write(post("/api/v1/care-plans/" + id + "/approve"), "{}")).andExpect(status().isOk());
        residents.changeStatus(residentId, ResidentStatus.RESIDENCY_ENDED, time.now());
        var step = dailyWork
                .orderedStream()
                .filter(w -> w.name().equals("CARE_PLAN_EFFECTIVE"))
                .findFirst()
                .orElseThrow();
        assertThat(step.run(
                        time.today().plusDays(1), time.startOfDay(time.today().plusDays(1))))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM care_plan WHERE id=?", String.class, id))
                .isEqualTo("PENDING_EFFECT");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {"nullItem", "nullRisk", "nullTime", "nullWeekday", "nullParticipant", "nullOpinion"})
    void FR_004_nullArrayMembersAreRejectedWithoutPartialWrites(String sample) throws Exception {
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

    @Test
    void R_14_carePlanCreationDocumentsTheApproved201Response() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var responses = json.readTree(result.getResponse().getContentAsString())
                .path("paths")
                .path("/api/v1/residents/{residentId}/care-plans")
                .path("post")
                .path("responses");
        assertThat(responses.has("201"))
                .as("POST tạo kế hoạch trả201 như hợp đồng đã duyệt")
                .isTrue();
        assertThat(responses.path("201").path("headers").has("ETag")).isTrue();
        assertThat(json.readTree(result.getResponse().getContentAsString())
                        .path("paths")
                        .path("/api/v1/care-plans/{id}")
                        .path("get")
                        .path("responses")
                        .path("200")
                        .path("headers")
                        .has("ETag"))
                .isTrue();
    }
}
