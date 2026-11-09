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

    private CurrentUser receivingFloorManager() {
        var manager = person(Role.FLOOR_MANAGER);
        jdbc.update(
                "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,assigned_by,reason,status) VALUES ('FLOOR_MANAGER',?,?,?,1,'[Khởi tạo fixture]','ACTIVE')",
                manager.staffId().orElseThrow(),
                floor,
                ShiftStore.sql(starts.minusSeconds(3600)));
        return manager;
    }

    @Test
    void FR_038_floorManagerMustBePresentInTheScheduledNextShift() {
        com.eldercare.backend.support.TableRows.each(
                java.util.stream.Stream.of("NOT_LISTED", "ABSENT", "WITHDRAWN", "LEAVE"), this::nextShiftRequired);
    }

    /** Mỗi dòng dựng ca, người cao tuổi riêng như một ca của test tham số trước đây. */
    private void nextShiftRequired(String condition) {
        setup();
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
