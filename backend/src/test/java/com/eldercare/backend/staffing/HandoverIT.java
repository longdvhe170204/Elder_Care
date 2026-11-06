package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.care.api.dto.TaskHandoverView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.staffing.internal.handover.domain.*;
import com.eldercare.backend.staffing.internal.handover.service.*;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.care.*;
import com.eldercare.backend.support.fake.incident.FakeAlertHandover;
import com.eldercare.backend.support.fake.medication.FakeDoseHandover;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** T024/T025: lệnh thật và MySQL thật; nguồn chưa cài dùng đúng bản giả chung của chủ API. */
@Import({
    FakeM01Config.class,
    FakeLocations.Config.class,
    FakeM04Config.class,
    FakeAlertHandover.Config.class,
    FakeDoseHandover.Config.class,
    com.eldercare.backend.support.fake.roombedallocation.FakeHandoverLocations.Config.class,
    com.eldercare.backend.support.fake.health.FakeVitalSigns.Config.class,
    HandoverIT.Users.class
})
@AutoConfigureMockMvc
@org.springframework.test.context.event.RecordApplicationEvents
class HandoverIT extends MySqlIntegrationTestBase {
    @Autowired
    com.eldercare.backend.staffing.internal.handover.persistence.HandoverStore embeddedStore;

    private static final java.util.concurrent.atomic.AtomicInteger sequence =
            new java.util.concurrent.atomic.AtomicInteger();

    @Autowired
    MockMvc mvc;

    @Autowired
    HandoverService service;

    @Autowired
    JdbcTemplate jdbc;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    SwitchableUser current;

    @Autowired
    FakeCareRequirements tasks;

    @Autowired
    FakeAlertHandover alerts;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeHandoverLocations bedSources;

    @Autowired
    FakeDoseHandover doses;

    @Autowired
    com.eldercare.backend.support.fake.health.FakeVitalSigns vital;

    @Autowired
    com.eldercare.backend.support.fake.staffing.FakeHeldBelongings belongings;

    @Autowired
    com.eldercare.backend.support.fake.staffing.FakeTripDuties trips;

    @Autowired
    com.eldercare.backend.support.fake.resident.FakeResidents residents;

    @Autowired
    com.eldercare.backend.support.fake.resident.FakeAbsences absences;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeLocations locations;

    @Autowired
    com.eldercare.backend.staffing.scheduling.M09MinuteJob job;

    @Autowired
    org.springframework.test.context.event.ApplicationEvents events;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("careShiftLookup")
    com.eldercare.backend.care.api.spi.CareShiftPort care;

    @Autowired
    com.eldercare.backend.medication.api.spi.NursingShiftPort nursing;

    @Autowired
    com.eldercare.backend.incident.api.spi.AlertStaffPort alertStaff;

    @Autowired
    List<com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider> correctionProviders;

    @Autowired
    List<com.eldercare.backend.administration.retention.api.spi.RetentionProvider> retentionProviders;

    @Autowired
    com.eldercare.backend.administration.correction.api.CorrectionApi corrections;

    @Autowired
    com.eldercare.backend.administration.parameter.api.ParameterApi parameters;

    private CurrentUser giver, receiver;
    private long shift, next, floor, resident, task;
    private Instant starts, ends;

    @BeforeEach
    void setup() {
        starts = Instant.parse("2035-04-01T00:00:00Z").plus(Duration.ofDays(sequence.incrementAndGet()));
        ends = starts.plusSeconds(8 * 3600);
        floor = TestUsers.nextId();
        resident = TestUsers.nextId();
        task = TestUsers.nextId();
        residents.resident(
                resident,
                "[Họ tên NCT kiểm thử]",
                null,
                com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE);
        long room = TestUsers.nextId();
        locations
                .floor(floor, TestUsers.nextId())
                .room(room, floor)
                .stay(resident, room, starts.minusSeconds(3600), null);
        giver = person(Role.NURSING_STAFF);
        receiver = person(Role.NURSING_STAFF);
        long template = insert(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu bàn giao] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type,TRUE AS requires_handover) src");
        long schedule = insert(
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2035-04','FLOOR',?,'PUBLISHED')",
                floor);
        shift = shift(schedule, template, starts, ends, giver);
        next = shift(schedule, template, ends, ends.plusSeconds(8 * 3600), receiver);
        tasks.source(
                floor,
                shift,
                new TaskHandoverView(
                        task, resident, null, "Công việc kiểm thử", starts.plusSeconds(3600), "OVERDUE", "MANDATORY"));
        current.set(giver);
        clock.setTo(ends.minusSeconds(1800));
        alerts.failOnConfirm(false);
        tasks.onConfirm(() -> {});
        alerts.onConfirm(() -> {});
    }

    @AfterEach
    void reset() {
        clock.reset();
        alerts.failOnConfirm(false);
        tasks.onConfirm(() -> {});
        alerts.onConfirm(() -> {});
        tasks.beforeRead(() -> {});
        doses.beforeRead(() -> {});
        belongings.available(true);
        absences.available(true);
    }

    private long insert(String sql, Object... args) {
        return AssignmentFixtures.insert(jdbc, sql, args);
    }

    private CurrentUser person(Role role) {
        var p = new TestUsers(jdbc, time).staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên bàn giao]','Điều dưỡng',?)",
                p.staffId(),
                p.accountId());
        return p.user();
    }

    private long shift(long schedule, long template, Instant from, Instant to, CurrentUser lead) {
        long id = insert(
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status,lead_staff_id) VALUES (?,?,?,?,'FLOOR',?,TRUE,'IN_PROGRESS',?)",
                schedule,
                template,
                ShiftStore.sql(from),
                ShiftStore.sql(to),
                floor,
                lead.staffId().orElseThrow());
        jdbc.update(
                "INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)",
                id,
                lead.staffId().orElseThrow());
        jdbc.update(
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,command,by_system) SELECT 'M09.SHIFT',src.shift_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,src.basis,FALSE FROM (SELECT ? AS shift_id,? AS staff_id,? AS starts_at,'SCHEDULED' AS basis) src",
                id,
                lead.staffId().orElseThrow(),
                ShiftStore.sql(from));
        return id;
    }

    private long draft() {
        return service.createHandover(shift).id();
    }

    @Autowired
    org.springframework.context.ApplicationContext context;

    private com.eldercare.backend.care.api.spi.ShiftStaffPort careStaff() {
        return context.getBean("careStaffLookup", com.eldercare.backend.care.api.spi.ShiftStaffPort.class);
    }

    private void freezeAt(Instant at) {
        clock.setTo(at);
        org.mockito.Mockito.doReturn(at).when(time).now();
    }

    @Test
    void FR_029_handoverChecklistFollowsPreparationFeedbackResubmissionAndConfirmation() {
        var port = careStaff();
        long sender = giver.staffId().orElseThrow(),
                recipient = receiver.staffId().orElseThrow();
        freezeAt(ends.minusSeconds(1801));
        assertThat(port.handoverTasks(sender, shift).prepareAtEnd()).isFalse();
        freezeAt(ends.minusSeconds(1800));
        current.set(receiver);
        assertThat(port.handoverTasks(sender, shift).prepareAtEnd()).isTrue();
        assertThat(embeddedStore.forShift(shift)).isEmpty();
        current.set(giver);
        long id = draft();
        assessment(id);
        assertThat(port.handoverTasks(sender, shift).prepareAtEnd()).isTrue();
        assertThat(port.handoverTasks(recipient, next).receiveAtStart()).isFalse();
        service.complete(id);
        freezeAt(ends);
        assertThat(port.handoverTasks(sender, shift).prepareAtEnd()).isFalse();
        assertThat(port.handoverTasks(recipient, next).receiveAtStart()).isTrue();
        current.set(receiver);
        long comment = service.comment(id, "Bổ sung để tiếp nhận");
        assertThat(port.handoverTasks(recipient, next).receiveAtStart()).isFalse();
        assertThat(port.handoverTasks(sender, shift).prepareAtEnd()).isTrue();
        current.set(giver);
        service.resubmit(id, Map.of(comment, "Đã bổ sung"), List.of());
        assertThat(port.handoverTasks(sender, shift).prepareAtEnd()).isFalse();
        assertThat(port.handoverTasks(recipient, next).receiveAtStart()).isTrue();
        current.set(receiver);
        service.confirm(id);
        assertThat(port.handoverConfirmed(shift)).isTrue();
        assertThat(port.handoverTasks(recipient, next).receiveAtStart()).isFalse();
    }

    @Test
    void FR_029_handoverChecklistRequiresResponsibilityAndScheduledReceivingMembership() {
        long id = draft();
        assessment(id);
        service.complete(id);
        clock.setTo(ends);
        var member = person(Role.NURSING_STAFF);
        jdbc.update(
                "INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)",
                next,
                member.staffId().orElseThrow());
        assertThat(careStaff()
                        .handoverTasks(member.staffId().orElseThrow(), next)
                        .receiveAtStart())
                .isFalse();
        var manager = receivingFloorManager();
        assertThat(careStaff()
                        .handoverTasks(manager.staffId().orElseThrow(), next)
                        .receiveAtStart())
                .isFalse();
        jdbc.update(
                "INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)",
                next,
                manager.staffId().orElseThrow());
        assertThat(careStaff()
                        .handoverTasks(manager.staffId().orElseThrow(), next)
                        .receiveAtStart())
                .isTrue();
        jdbc.update(
                "UPDATE shift_staff SET absent_at=? WHERE shift_id=? AND staff_id=?",
                ShiftStore.sql(ends),
                next,
                manager.staffId().orElseThrow());
        assertThat(careStaff()
                        .handoverTasks(manager.staffId().orElseThrow(), next)
                        .receiveAtStart())
                .isFalse();
        current.set(receiver);
        service.comment(id, "Chờ bổ sung cuối ca");
        assertThat(careStaff()
                        .handoverTasks(giver.staffId().orElseThrow(), shift)
                        .prepareAtEnd())
                .isTrue();
        clock.setTo(ends.plusSeconds(3 * 3600));
        assertThat(careStaff()
                        .handoverTasks(giver.staffId().orElseThrow(), shift)
                        .prepareAtEnd())
                .isFalse();
    }

    @Test
    void FR_029_handoverChecklistDoesNotOfferDraftCancelledUnknownOrNonHandoverShifts() {
        var port = careStaff();
        long sender = giver.staffId().orElseThrow();
        for (String status : List.of("DRAFT")) {
            jdbc.update("UPDATE work_shift SET status=? WHERE id=?", status, shift);
            assertThat(port.handoverTasks(sender, shift))
                    .isEqualTo(new com.eldercare.backend.care.api.spi.ShiftStaffPort.HandoverTasksView(false, false));
        }
        jdbc.update("UPDATE work_shift SET status='IN_PROGRESS',requires_handover=FALSE WHERE id=?", shift);
        assertThat(port.handoverTasks(sender, shift).prepareAtEnd()).isFalse();
        jdbc.update("UPDATE work_shift SET status='CANCELLED' WHERE id=?", shift);
        assertThat(port.handoverTasks(sender, shift).prepareAtEnd()).isFalse();
        assertThat(port.handoverTasks(TestUsers.nextId(), next).receiveAtStart())
                .isFalse();
        assertThat(port.handoverTasks(sender, TestUsers.nextId()).prepareAtEnd())
                .isFalse();
    }

    private String etag(long id) {
        return "\"" + service.record(id).version() + "\"";
    }

    private HandoverItemInput item(String note) {
        return new HandoverItemInput(
                "TASK", "M04.TASK", Long.toString(task), resident, null, "Công việc kiểm thử", note, false);
    }

    private void assessment(long id) {
        service.assess(id, etag(id), "Nhận định chung", Map.of(resident, "Theo dõi trong ca"));
    }

    @Test
    void FR_039_createAtBoundaryHasNoAutomaticallyCreatedItemsAndIsUnique() {
        clock.setTo(ends.minusSeconds(1801));
        assertThatThrownBy(this::draft).isInstanceOf(BusinessException.class);
        clock.setTo(ends.minusSeconds(1800));
        long id = draft();
        assertThat(service.record(id).nextShiftId()).isEqualTo(next);
        assertThat(service.items(id)).isEmpty();
        assertThatThrownBy(this::draft).isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_039_sourceReadDoesNotCreateItemsAndUsesCurrentCriticalState() {
        long id = draft();
        assertThat(service.sources(id, false)).anySatisfy(s -> {
            assertThat(s.sourceId()).isEqualTo(Long.toString(task));
            assertThat(s.critical()).isTrue();
            assertThat(s.hasItem()).isFalse();
        });
        assertThat(service.items(id)).isEmpty();
        service.addHandoverItem(id, etag(id), item("Đã ghi chú"));
        assertThat(service.sources(id, false)).anySatisfy(s -> {
            if (Long.toString(task).equals(s.sourceId()))
                assertThat(s.hasItem()).isTrue();
        });
    }

    @Test
    void FR_040_staleVersionAndForeignChildCannotModifyDraft() {
        long id = draft();
        String old = etag(id);
        long child = service.addHandoverItem(id, old, item("Ghi chú"));
        assertThatThrownBy(() -> service.updateItem(id, child, old, item("Thay đổi")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.deleteItem(id, child + 999999, etag(id)))
                .isInstanceOf(NotFoundException.class);
        assertThat(service.items(id)).hasSize(1);
    }

    @Test
    void FR_041_missingAssessmentOrCriticalNoteBlocksThenFinalizationFreezesContent() {
        long id = draft();
        long child = service.addHandoverItem(id, etag(id), item(null));
        assertThatThrownBy(() -> service.complete(id)).isInstanceOf(BusinessException.class);
        assessment(id);
        assertThatThrownBy(() -> service.complete(id)).hasMessageContaining("ghi chú");
        service.updateItem(id, child, etag(id), item("Theo dõi tiếp"));
        Instant beforeComplete = time.now();
        service.complete(id);
        Instant afterComplete = time.now();
        assertThat(service.record(id).status()).isEqualTo("PREPARED");
        assertThat(service.record(id).finalizedAt()).isBetween(beforeComplete, afterComplete);
        assertThatThrownBy(() -> service.updateItem(id, child, etag(id), item("Sửa gốc")))
                .isInstanceOf(BusinessException.class);
        service.addHandoverItem(
                id,
                etag(id),
                new HandoverItemInput("FOLLOW_UP_ISSUE", null, null, resident, null, "Phát sinh", null, false));
        assertThat(service.items(id).getLast().section()).isEqualTo("AFTER_PREPARATION");
    }

    @Test
    void FR_044_confirmationCallbacksShareTransactionAndFailureRollsBackAllEffects() {
        long id = draft();
        assessment(id);
        service.complete(id);
        current.set(receiver);
        clock.setTo(ends);
        tasks.onConfirm(() -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .isTrue();
            jdbc.update("UPDATE shift_handover SET assessments=assessments WHERE id=-1");
            jdbc.update("UPDATE work_shift SET below_threshold=TRUE WHERE id=?", shift);
        });
        alerts.failOnConfirm(true);
        assertThatThrownBy(() -> service.confirm(id)).isInstanceOf(RuntimeException.class);
        assertThat(service.record(id).status()).isEqualTo("PREPARED");
        assertThat(jdbc.queryForObject("SELECT below_threshold FROM work_shift WHERE id=?", Boolean.class, shift))
                .isFalse();
        alerts.failOnConfirm(false);
        alerts.onConfirm(() -> assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isTrue());
        service.confirm(id);
        assertThat(service.record(id).status()).isEqualTo("CONFIRMED");
        assertThat(jdbc.queryForObject("SELECT below_threshold FROM work_shift WHERE id=?", Boolean.class, shift))
                .isTrue();
        assertThatThrownBy(() -> service.confirm(id)).isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_042_everyCommentNeedsReplyAndResubmitDoesNotRewriteFrozenItems() {
        long id = draft();
        assessment(id);
        service.complete(id);
        current.set(receiver);
        clock.setTo(ends);
        long comment = service.comment(id, "Bổ sung theo dõi");
        current.set(giver);
        assertThatThrownBy(() -> service.resubmit(id, Map.of(), List.of())).isInstanceOf(BusinessException.class);
        service.resubmit(id, Map.of(comment, "Đã kiểm tra và bổ sung"), List.of());
        assertThat(service.record(id).status()).isEqualTo("PREPARED");
    }

    @Test
    void FR_042_aNurseListedInShiftCanEditButOnlyLeadCanComplete() {
        long id = draft();
        var colleague = person(Role.NURSING_STAFF);
        jdbc.update(
                "INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)",
                shift,
                colleague.staffId().orElseThrow());
        current.set(colleague);
        service.addHandoverItem(id, etag(id), item("Ghi bởi điều dưỡng"));
        assertThatThrownBy(() -> service.complete(id)).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_047_caregiverOnlySeesAssignedResidentItemsAndAssessmentsPlusGeneral() {
        long id = draft(), other = TestUsers.nextId(), room = TestUsers.nextId();
        residents.resident(
                other,
                "[Họ tên NCT kiểm thử khác]",
                null,
                com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE);
        locations.room(room, floor).stay(other, room, starts.minusSeconds(3600), null);
        service.addHandoverItem(
                id,
                etag(id),
                new HandoverItemInput("FOLLOW_UP_ISSUE", null, null, resident, null, "Nội dung được xem", null, false));
        service.addHandoverItem(
                id,
                etag(id),
                new HandoverItemInput(
                        "FOLLOW_UP_ISSUE", null, null, other, null, "Nội dung ngoài phân công", null, false));
        service.assess(id, etag(id), "Nhận định chung", Map.of(resident, "Được xem", other, "Ngoài phân công"));
        var caregiver = person(Role.CAREGIVER);
        jdbc.update(
                "INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)",
                shift,
                caregiver.staffId().orElseThrow());
        jdbc.update(
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at,status) VALUES (?,?,'RESIDENT',?,'CAREGIVER','PRIMARY',?,?,'ACTIVE')",
                shift,
                caregiver.staffId().orElseThrow(),
                resident,
                ShiftStore.sql(starts),
                ShiftStore.sql(ends));
        current.set(caregiver);
        assertThat(service.items(id)).extracting(HandoverItem::residentId).containsExactly(resident);
        assertThat(service.assessments(id)).hasSize(1);
        assertThat(service.general(id)).isEqualTo("Nhận định chung");
        assertThat(service.allowedCommands(id)).isEmpty();
    }

    @Test
    void FR_040_resolvedOrdinaryItemDoesNotRequireOptionalNote() {
        long id = draft();
        long child = service.addHandoverItem(id, etag(id), item(null));
        tasks.source(
                floor,
                shift,
                new TaskHandoverView(
                        task, resident, null, "Công việc đã hoàn thành", starts, "COMPLETED", "MANDATORY"));
        service.updateItem(
                id,
                child,
                etag(id),
                new HandoverItemInput(
                        "TASK", "M04.TASK", Long.toString(task), resident, null, "Công việc kiểm thử", null, true));
        assertThat(service.items(id).getFirst().section()).isEqualTo("RESOLVED_DURING_SHIFT");
    }

    @Test
    void FR_040_cannotAttachKnownSourceFromAnotherFloorThroughCurrentStatusLookup() {
        long id = draft(), foreignResident = TestUsers.nextId(), foreignTask = TestUsers.nextId();
        tasks.source(
                TestUsers.nextId(),
                TestUsers.nextId(),
                new TaskHandoverView(foreignTask, foreignResident, null, "Ngoài tầng", starts, "OVERDUE", "MANDATORY"));
        assertThatThrownBy(() -> service.addHandoverItem(
                        id,
                        etag(id),
                        new HandoverItemInput(
                                "TASK",
                                "M04.TASK",
                                Long.toString(foreignTask),
                                foreignResident,
                                null,
                                "Ngoài tầng",
                                null,
                                false)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_042_simultaneousConfirmAndCommentOnlyOneTransitionWins() throws Exception {
        long id = draft();
        assessment(id);
        service.complete(id);
        current.set(receiver);
        clock.setTo(ends);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var confirm = executor.submit(() -> {
                start.await();
                try {
                    service.confirm(id);
                    return true;
                } catch (BusinessException e) {
                    return false;
                }
            });
            var comment = executor.submit(() -> {
                start.await();
                try {
                    service.comment(id, "Ý kiến đồng thời");
                    return true;
                } catch (BusinessException e) {
                    return false;
                }
            });
            start.countDown();
            assertThat(List.of(confirm.get(), comment.get())).containsExactlyInAnyOrder(true, false);
        }
        assertThat(service.record(id).status()).isIn("CONFIRMED", "HAS_COMMENTS");
    }

    @Test
    void FR_039_httpCreateLookupEtagAndNoReadAudit() throws Exception {
        String bearer = TestUsers.bearer(new TestUsers(jdbc, time).session(giver.accountId()));
        mvc.perform(get("/api/v1/shifts/{id}", shift).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedCommands").value(org.hamcrest.Matchers.hasItem("createHandover")));
        mvc.perform(get("/api/v1/shifts/{id}/handovers", shift).header("Authorization", bearer))
                .andExpect(status().isNotFound());
        var result = mvc.perform(post("/api/v1/shifts/{id}/handovers", shift)
                        .header("Authorization", bearer)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items").isEmpty())
                .andReturn();
        long id = service.forShift(shift).id();
        int audits = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.HANDOVER' AND entity_id=?",
                Integer.class,
                Long.toString(id));
        mvc.perform(get("/api/v1/shifts/{id}/handovers", shift).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""));
        mvc.perform(get("/api/v1/handovers/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedCommands").isArray());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.HANDOVER' AND entity_id=?",
                        Integer.class,
                        Long.toString(id)))
                .isEqualTo(audits);
    }

    @Test
    void FR_022_lateCompletionBeforeMinuteJobClosesLeadHistoryAndEmitsShiftEndedOnce() {
        long id = draft();
        assessment(id);
        clock.setTo(ends);
        service.complete(id);
        assertThat(jdbc.queryForObject(
                                "SELECT ends_at FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(h.details->'$.legacy.id'),'null') AS UNSIGNED),h.id) AS id,h.entity_id AS shift_id,h.value_id AS staff_id,h.starts_at AS starts_at,h.ends_at AS ends_at,h.actor_id AS actor_id,h.reason AS reason,h.command AS basis FROM status_history h WHERE h.entity_type='M09.SHIFT' AND h.attribute='LEAD_STAFF' AND h.kind='PERIOD') shift_lead_history WHERE shift_id=?",
                                java.sql.Timestamp.class,
                                shift)
                        .toInstant())
                .isEqualTo(ends);
        assertThat(events.stream(com.eldercare.backend.staffing.api.events.ShiftEnded.class)
                        .filter(e -> e.shiftId() == shift)
                        .count())
                .isOne();
        job.runNow();
        assertThat(events.stream(com.eldercare.backend.staffing.api.events.ShiftEnded.class)
                        .filter(e -> e.shiftId() == shift)
                        .count())
                .isOne();
    }

    @Test
    void FR_043_remindersAreOncePerMilestoneAndNeverCreateHandoverOrItems() {
        job.runNow();
        job.runNow();
        assertThat(events.stream(com.eldercare.backend.staffing.api.events.HandoverNotPrepared.class)
                        .filter(e -> e.shiftId() == shift)
                        .count())
                .isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shift_handover WHERE shift_id=?", Integer.class, shift))
                .isZero();
        clock.setTo(ends.plusSeconds(60));
        job.runNow();
        assertThat(jdbc.queryForObject("SELECT status FROM work_shift WHERE id=?", String.class, shift))
                .isEqualTo("AWAITING_HANDOVER");
        assertThat(jdbc.queryForObject(
                        "SELECT shift_end_reminded_at IS NOT NULL FROM work_shift WHERE id=?", Boolean.class, shift))
                .isTrue();
        clock.setTo(ends.plusSeconds(31 * 60));
        job.runNow();
        job.runNow();
        assertThat(jdbc.queryForObject(
                        "SELECT unconfirmed_handover_reminded_at IS NOT NULL FROM work_shift WHERE id=?",
                        Boolean.class,
                        shift))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shift_handover WHERE shift_id=?", Integer.class, shift))
                .isZero();
    }

    @Test
    void BR_M09_07_lateCompletionClosesAwaitingShiftAndFeedbackNeverReopensClosedShift() {
        long id = draft();
        assessment(id);
        clock.setTo(ends.plusSeconds(60));
        job.runNow();
        assertThat(jdbc.queryForObject("SELECT status FROM work_shift WHERE id=?", String.class, shift))
                .isEqualTo("AWAITING_HANDOVER");
        service.complete(id);
        assertThat(jdbc.queryForObject("SELECT status FROM work_shift WHERE id=?", String.class, shift))
                .isEqualTo("CLOSED");
        current.set(receiver);
        service.comment(id, "Bổ sung sau khi đóng ca");
        clock.setTo(ends.plusSeconds(120));
        job.runNow();
        assertThat(service.record(id).status()).isEqualTo("HAS_COMMENTS");
        assertThat(jdbc.queryForObject("SELECT status FROM work_shift WHERE id=?", String.class, shift))
                .isEqualTo("CLOSED");
    }

    @Test
    void FR_038_floorManagerCannotConfirmWhenThereIsNoNextShift() {
        jdbc.update("UPDATE work_shift SET status='CANCELLED' WHERE id=?", next);
        var manager = person(Role.FLOOR_MANAGER);
        current.set(manager);
        clock.setTo(ends.plusSeconds(3 * 3600));
        jdbc.update(
                "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,assigned_by,reason,status) SELECT 'FLOOR_MANAGER',src.staff_id,src.floor_area_id,src.starts_at,1,'[Khởi tạo fixture]','ACTIVE' FROM (SELECT ? AS staff_id,? AS floor_area_id,? AS starts_at) src",
                manager.staffId().orElseThrow(),
                floor,
                ShiftStore.sql(starts.minusSeconds(3600)));
        long id = draft();
        assessment(id);
        service.complete(id);
        assertThat(service.allowedCommands(id)).doesNotContain("confirm", "comment");
        assertThatThrownBy(() -> service.confirm(id)).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> service.comment(id, "Ý kiến ngoài lịch ca"))
                .isInstanceOf(PermissionDeniedException.class);
        assertThat(service.record(id).nextShiftId()).isNull();
        assertThat(service.record(id).status()).isEqualTo("PREPARED");
    }

    private CurrentUser receivingFloorManager() {
        var manager = person(Role.FLOOR_MANAGER);
        jdbc.update(
                "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,assigned_by,reason,status) VALUES ('FLOOR_MANAGER',?,?,?,1,'[Khởi tạo fixture]','ACTIVE')",
                manager.staffId().orElseThrow(),
                floor,
                ShiftStore.sql(starts.minusSeconds(3600)));
        return manager;
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"NOT_LISTED", "ABSENT", "WITHDRAWN", "LEAVE"})
    void FR_038_floorManagerMustBePresentInTheScheduledNextShift(String condition) {
        long id = draft();
        assessment(id);
        service.complete(id);
        var manager = receivingFloorManager();
        long staffId = manager.staffId().orElseThrow();
        if (!"NOT_LISTED".equals(condition)) {
            jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", next, staffId);
            switch (condition) {
                case "ABSENT" ->
                    jdbc.update(
                            "UPDATE shift_staff SET absent_at=? WHERE shift_id=? AND staff_id=?",
                            ShiftStore.sql(ends),
                            next,
                            staffId);
                case "WITHDRAWN" ->
                    jdbc.update(
                            "UPDATE shift_staff SET withdrawn_on_resignation_at=? WHERE shift_id=? AND staff_id=?",
                            ShiftStore.sql(ends),
                            next,
                            staffId);
                case "LEAVE" ->
                    jdbc.update(
                            "UPDATE shift_staff SET approved_leave_request_id=? WHERE shift_id=? AND staff_id=?",
                            TestUsers.nextId(),
                            next,
                            staffId);
                default -> throw new IllegalArgumentException(condition);
            }
        }
        current.set(manager);
        clock.setTo(ends);
        assertThat(service.allowedCommands(id)).doesNotContain("confirm", "comment");
        assertThatThrownBy(() -> service.confirm(id)).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> service.comment(id, "Ý kiến khi không đủ điều kiện ca sau"))
                .isInstanceOf(PermissionDeniedException.class);
        assertThat(service.record(id).status()).isEqualTo("PREPARED");
    }

    @Test
    void FR_038_floorManagerListedInTheScheduledNextShiftCanConfirm() {
        long id = draft();
        assessment(id);
        service.complete(id);
        var manager = receivingFloorManager();
        jdbc.update(
                "INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)",
                next,
                manager.staffId().orElseThrow());
        current.set(manager);
        clock.setTo(ends);
        assertThat(service.allowedCommands(id)).contains("confirm", "comment");
        service.confirm(id);
        assertThat(service.record(id).status()).isEqualTo("CONFIRMED");
    }

    @Test
    void FR_038_nextShiftMembershipAloneDoesNotGrantConfirmation() {
        long id = draft();
        assessment(id);
        service.complete(id);
        var member = person(Role.NURSING_STAFF);
        jdbc.update(
                "INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)",
                next,
                member.staffId().orElseThrow());
        current.set(member);
        clock.setTo(ends);
        assertThatThrownBy(() -> service.allowedCommands(id)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.confirm(id)).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_045_sourceAppearingAfterPreparationIsNewEvenIfItsPlannedTimeIsEarlier() {
        long id = draft();
        assessment(id);
        service.complete(id);
        long lateEntry = TestUsers.nextId();
        tasks.source(
                floor,
                shift,
                new TaskHandoverView(
                        lateEntry, resident, null, "Công việc nhập sau lúc chốt", starts, "OVERDUE", "MANDATORY"));
        assertThat(service.sources(id, true))
                .anySatisfy(s -> assertThat(s.sourceId()).isEqualTo(Long.toString(lateEntry)));
        assertThat(service.items(id)).isEmpty();
    }

    @Test
    void FR_044a_shiftPortsExposeOnlyUnconfirmedStartedNextShiftsAndActualConfirmation() {
        long id = draft();
        assessment(id);
        service.complete(id);
        current.set(receiver);
        clock.setTo(ends);
        assertThat(care.handoverConfirmed(shift)).isFalse();
        assertThat(nursing.handoverConfirmed(shift)).isFalse();
        assertThat(alertStaff.startedWithoutConfirmedHandover(ends.minusSeconds(1), ends.plusSeconds(1)))
                .anySatisfy(r -> {
                    assertThat(r.previousShiftId()).isEqualTo(shift);
                    assertThat(r.nextShiftId()).isEqualTo(next);
                });
        service.confirm(id);
        assertThat(care.handoverConfirmed(shift)).isTrue();
        assertThat(nursing.handoverConfirmed(shift)).isTrue();
        assertThat(alertStaff.startedWithoutConfirmedHandover(ends.minusSeconds(1), ends.plusSeconds(1)))
                .noneMatch(r -> r.previousShiftId() == shift);
    }

    @Test
    void FR_057_correctionNotifiesConfirmerWithoutTheLocalAccountLink() {
        long id = draft();
        assessment(id);
        service.complete(id);
        current.set(receiver);
        clock.setTo(ends);
        service.confirm(id);
        jdbc.update(
                "UPDATE staff_profile SET account_id=NULL WHERE id=?",
                receiver.staffId().orElseThrow());
        current.set(giver);

        corrections.create(
                "M09.HANDOVER",
                id,
                com.eldercare.backend.administration.correction.api.dto.CorrectionType.AMEND_CONTENT,
                Map.of("generalAssessment", "Nhận định đã kiểm lại"),
                new com.eldercare.backend.common.Reason("Đính chính sau xác nhận"));

        var event = events.stream(com.eldercare.backend.staffing.api.events.HandoverCorrected.class)
                .filter(e -> e.handoverId() == id)
                .findFirst()
                .orElseThrow();
        assertThat(event.notificationRequests().getFirst().recipientGroups())
                .contains(new com.eldercare.backend.common.notification.RecipientGroup.Staff(
                        receiver.staffId().orElseThrow(), "HANDOVER_CORRECTED"));
    }

    @Test
    void FR_057_correctionTargetsCurrentShiftLeadAfterTheConfirmingShiftEnds() {
        long id = draft();
        assessment(id);
        service.complete(id);
        current.set(receiver);
        clock.setTo(ends);
        service.confirm(id);
        var manager = person(Role.FLOOR_MANAGER);
        var lead = person(Role.NURSING_STAFF);
        jdbc.update(
                "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,assigned_by,status) VALUES ('FLOOR_MANAGER',?,?,?,1,'ACTIVE')",
                manager.staffId().orElseThrow(),
                floor,
                ShiftStore.sql(starts));
        long schedule = jdbc.queryForObject("SELECT schedule_id FROM work_shift WHERE id=?", Long.class, next);
        long template = jdbc.queryForObject("SELECT template_id FROM work_shift WHERE id=?", Long.class, next);
        shift(schedule, template, ends.plusSeconds(28800), ends.plusSeconds(57600), lead);
        clock.setTo(ends.plusSeconds(28860));
        current.set(manager);

        corrections.create(
                "M09.HANDOVER",
                id,
                com.eldercare.backend.administration.correction.api.dto.CorrectionType.AMEND_CONTENT,
                Map.of("generalAssessment", "Nhận định bổ sung sau ca xác nhận"),
                new com.eldercare.backend.common.Reason("Kiểm lại nội dung bàn giao"));

        var event = events.stream(com.eldercare.backend.staffing.api.events.HandoverCorrected.class)
                .filter(e -> e.handoverId() == id)
                .findFirst()
                .orElseThrow();
        assertThat(event.notificationRequests().getFirst().recipientGroups())
                .containsExactly(
                        new com.eldercare.backend.common.notification.RecipientGroup.Staff(
                                giver.staffId().orElseThrow(), "HANDOVER_CORRECTED"),
                        new com.eldercare.backend.common.notification.RecipientGroup.Staff(
                                receiver.staffId().orElseThrow(), "HANDOVER_CORRECTED"),
                        new com.eldercare.backend.common.notification.RecipientGroup.ShiftLead(
                                floor, new com.eldercare.backend.common.notification.Substitution.Default()));
    }

    @Test
    void FR_046_correctionKeepsOriginalAndNeverTransfersTasksAgain() {
        long id = draft();
        assessment(id);
        service.complete(id);
        current.set(receiver);
        clock.setTo(ends);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        tasks.onConfirm(calls::incrementAndGet);
        service.confirm(id);
        current.set(giver);
        var provider = correctionProviders.stream()
                .filter(p -> p.recordType().equals("M09.HANDOVER"))
                .findFirst()
                .orElseThrow();
        assertThat(provider.isConfirmed(id)).isTrue();
        assertThat(provider.originalRecorder(id)).isEqualTo(giver.accountId());
        corrections.create(
                "M09.HANDOVER",
                id,
                com.eldercare.backend.administration.correction.api.dto.CorrectionType.AMEND_CONTENT,
                Map.of("generalAssessment", "Nhận định đã đính chính"),
                new com.eldercare.backend.common.Reason("Sửa nhận định sau kiểm tra"));
        assertThat(service.general(id)).isEqualTo("Nhận định đã đính chính");
        assertThat(embeddedStore.general(id)).isEqualTo("Nhận định chung");
        assertThat(calls.get()).isOne();
        assertThat(service.record(id).status()).isEqualTo("CONFIRMED");
    }

    @Test
    void FR_039_sourcesAreReadInParallelAndBelongingStateIsReadEvenAfterRelease() {
        long id = draft(), belonging = TestUsers.nextId();
        belongings.hold(new com.eldercare.backend.staffing.api.spi.HeldBelongingView(
                belonging, giver.staffId().orElseThrow(), resident, null));
        doses.source(
                floor,
                shift,
                List.of(new com.eldercare.backend.medication.api.dto.DoseHandoverView(
                        "dose:1", resident, null, "Liều bỏ lỡ", starts, "MISSED")));
        var both = new java.util.concurrent.CountDownLatch(2);
        Runnable barrier = () -> {
            both.countDown();
            try {
                assertThat(both.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        };
        tasks.beforeRead(barrier);
        doses.beforeRead(barrier);
        var rows = service.sources(id, false);
        assertThat(rows).extracting(HandoverSource::sourceType).contains("M04.TASK", "M07.DOSE", "M10.BELONGING");
        assertThat(service.items(id)).isEmpty();
        tasks.beforeRead(() -> {});
        doses.beforeRead(() -> {});
        long child = service.addHandoverItem(
                id,
                etag(id),
                new HandoverItemInput(
                        "STORED_BELONGING",
                        "M10.BELONGING",
                        Long.toString(belonging),
                        resident,
                        null,
                        "Đồ đang giữ",
                        null,
                        false));
        belongings.release(belonging);
        service.updateItem(
                id,
                child,
                etag(id),
                new HandoverItemInput(
                        "STORED_BELONGING",
                        "M10.BELONGING",
                        Long.toString(belonging),
                        resident,
                        null,
                        "Đồ đã trả",
                        null,
                        true));
        assertThat(service.items(id).getFirst().resolved()).isTrue();
        belongings.available(false);
        assertThat(service.sources(id, false)).anyMatch(HandoverSource::dataUnavailable);
    }

    @Test
    void FR_039_residentStatusReadSurvivesUnavailableAbsenceProvider() {
        absences.available(false);
        jdbc.update(
                "INSERT INTO resident(id,profile_code,full_name,date_of_birth,gender,status,status_since,created_at,created_by) VALUES (?,?,'[Họ tên NCT nguồn]','1940-01-01','FEMALE','IN_RESIDENCE',?,?,?)",
                resident,
                "H" + resident,
                ShiftStore.sql(starts),
                ShiftStore.sql(starts),
                giver.accountId());
        // Lịch sử trạng thái của 001 ở status_history từ 001 T084 (data-model 001 mục 0.1)
        com.eldercare.backend.support.ResidentHistoryRows.insertStatus(
                jdbc,
                null,
                resident,
                "TEMPORARILY_ABSENT",
                "IN_RESIDENCE",
                "RECORD_RETURN",
                ShiftStore.sql(starts.plusSeconds(1)),
                giver.accountId(),
                false,
                "Ghi nhận đã trở về");
        long id = draft();
        assertThat(service.sources(id, false))
                .anyMatch(s -> "M01.STATUS".equals(s.sourceType())
                        && !s.dataUnavailable()
                        && "IN_RESIDENCE".equals(s.currentStatus()));
        assertThat(service.sources(id, false))
                .anyMatch(s -> "M02.ABSENCE".equals(s.sourceType()) && s.dataUnavailable());
    }

    @Test
    void FR_052_correctionChainCannotExposeHandoverToOfficeRole() {
        long id = draft();
        assessment(id);
        service.complete(id);
        current.set(receiver);
        clock.setTo(ends);
        service.confirm(id);
        current.set(person(Role.OFFICE_STAFF));
        assertThatThrownBy(() -> corrections.chain("M09.HANDOVER", id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void FR_039_bedWaitingFromPreviousShiftKeepsItsOriginalStart() {
        long room = TestUsers.nextId(), bed = TestUsers.nextId();
        locations.room(room, floor);
        Instant awaitingSince = starts.minusSeconds(3600);
        bedSources.beds(
                floor,
                List.of(new com.eldercare.backend.roombedallocation.api.dto.AwaitingReadyBedView(
                        bed, room, "Giường chờ từ ca trước", awaitingSince)));

        assertThat(service.sources(draft(), false))
                .filteredOn(source -> "M03.BED".equals(source.sourceType())
                        && Long.toString(bed).equals(source.sourceId()))
                .singleElement()
                .satisfies(source -> {
                    assertThat(source.dataUnavailable()).isFalse();
                    assertThat(source.at()).isEqualTo(awaitingSince);
                });
    }

    @Test
    void FR_040_bedDisappearingFromOverdueListIsNotAutomaticallyResolved() {
        long room = TestUsers.nextId(), bed = TestUsers.nextId();
        locations.room(room, floor);
        bedSources.beds(
                floor,
                List.of(new com.eldercare.backend.roombedallocation.api.dto.AwaitingReadyBedView(
                        bed, room, "Giường chờ sẵn sàng", starts)));
        long id = draft();
        long child = service.addHandoverItem(
                id,
                etag(id),
                new HandoverItemInput(
                        "BED_AWAITING_READINESS",
                        "M03.BED",
                        Long.toString(bed),
                        null,
                        room,
                        "Giường chờ sẵn sàng",
                        null,
                        false));
        bedSources.beds(floor, List.of());
        var resolved = new HandoverItemInput(
                "BED_AWAITING_READINESS", "M03.BED", Long.toString(bed), null, room, "Giường chờ sẵn sàng", null, true);
        assertThatThrownBy(() -> service.updateItem(id, child, etag(id), resolved))
                .isInstanceOf(BusinessException.class);
        // Đổi ngưỡng CFG làm mất dòng quá hạn không chứng minh giường đã rời trạng thái chờ.
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(bedSources, "state", bed, "AWAITING_READINESS");
        assertThatThrownBy(() -> service.updateItem(id, child, etag(id), resolved))
                .isInstanceOf(BusinessException.class);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(bedSources, "state", bed, "AVAILABLE");
        service.updateItem(id, child, etag(id), resolved);
        assertThat(service.items(id).getFirst().resolved()).isTrue();
    }

    @Test
    void FR_047_retentionWaitsForEveryResidentAndKeepsReferencedShiftAndStaff() {
        long id = draft(), other = TestUsers.nextId(), room = TestUsers.nextId();
        residents.resident(
                other,
                "[Họ tên NCT lưu giữ]",
                null,
                com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE);
        locations.room(room, floor).stay(other, room, starts.minusSeconds(1), null);
        service.addHandoverItem(id, etag(id), item("Ghi chú"));
        service.addHandoverItem(
                id,
                etag(id),
                new HandoverItemInput("FOLLOW_UP_ISSUE", null, null, other, null, "Theo dõi", null, false));
        assessment(id);
        service.complete(id);
        current.set(receiver);
        clock.setTo(ends);
        service.confirm(id);
        var provider = retentionProviders.stream()
                .filter(p -> p.dataType().equals("M09.HANDOVER"))
                .findFirst()
                .orElseThrow();
        var period = parameters
                .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M15_03)
                .asPeriod();
        residents.changeStatus(resident, com.eldercare.backend.resident.api.dto.ResidentStatus.RESIDENCY_ENDED, ends);
        clock.setTo(ends.atZone(BusinessTime.ZONE).plus(period).plusSeconds(1).toInstant());
        Instant cutoff = time.now().atZone(BusinessTime.ZONE).minus(period).toInstant();
        assertThat(provider.expiredCandidates(cutoff, 100).toList()).noneMatch(c -> c.id() == id);
        residents.changeStatus(other, com.eldercare.backend.resident.api.dto.ResidentStatus.RESIDENCY_ENDED, ends);
        assertThat(provider.expiredCandidates(cutoff, 100).toList()).anyMatch(c -> c.id() == id);
        for (String type : List.of("M09.SHIFT", "M09.STAFF")) {
            var referenced = retentionProviders.stream()
                    .filter(p -> p.dataType().equals(type))
                    .findFirst()
                    .orElseThrow();
            assertThat(referenced.referencesTo(
                            type.equals("M09.SHIFT") ? shift : giver.staffId().orElseThrow()))
                    .anyMatch(ref -> ref.dataType().equals("M09.HANDOVER") && ref.id() == id);
        }
    }

    @Test
    void FR_040_closedDoseStatusAloneCannotProveResidentIdentityForNewItem() {
        doses.source(
                floor + 1,
                next,
                List.of(new com.eldercare.backend.medication.api.dto.DoseHandoverView(
                        "closed-other-resident", resident + 1, null, "Liều của người khác", starts, "TAKEN")));
        long id = draft();
        assertThatThrownBy(() -> service.addHandoverItem(
                        id,
                        etag(id),
                        new HandoverItemInput(
                                "DOSE",
                                "M07.DOSE",
                                "closed-other-resident",
                                resident,
                                null,
                                "Liều nhập sai người",
                                null,
                                false)))
                .isInstanceOf(BusinessException.class);
        assertThat(service.items(id)).isEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser current() {
            return new SwitchableUser();
        }

        @Bean
        com.eldercare.backend.support.fake.staffing.FakeHeldBelongings handoverBelongings() {
            return new com.eldercare.backend.support.fake.staffing.FakeHeldBelongings();
        }

        @Bean
        com.eldercare.backend.support.fake.staffing.FakeTripDuties handoverTrips() {
            return new com.eldercare.backend.support.fake.staffing.FakeTripDuties();
        }
    }
}
