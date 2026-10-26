package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m09.api.events.SchedulePublished;
import com.cambae.backend.m09.shift.internal.domain.ShiftInput;
import com.cambae.backend.m09.shift.internal.domain.TemplateInput;
import com.cambae.backend.m09.shift.internal.service.ShiftScheduleService;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** T011/T013: lệnh thật, quyền theo ma trận, dữ liệu ca thật; chỉ nguồn nhiệm vụ của Phase 4 dùng bản giả 002. */
@Import({FakeM15Config.class, ShiftScheduleIT.Users.class})
@RecordApplicationEvents
class ShiftScheduleIT extends MySqlIntegrationTestBase {
    private static final AtomicInteger MONTHS = new AtomicInteger();

    @Autowired
    ShiftScheduleService service;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    SwitchableUser current;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    ApplicationEvents events;

    private TestUsers users;
    private YearMonth month;

    @BeforeEach
    void setup() {
        clock.setTo(Instant.parse("2026-10-24T00:00:00Z"));
        users = new TestUsers(jdbc, time);
        current.set(users.staff(Role.NURSING_HOME_MANAGER).user());
        month = YearMonth.of(2028, 1).plusMonths(MONTHS.incrementAndGet());
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private long staff(Role role) {
        var user = users.staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?, '[Nhân viên lịch ca]', 'Chức danh mô tả', ?)",
                user.staffId(),
                user.accountId());
        return user.staffId();
    }

    private long template(String scope, boolean handover, LocalTime from, LocalTime to) {
        return service.createTemplate(new TemplateInput("[Mẫu ca kiểm thử]", from, to, scope, handover, true))
                .id();
    }

    private long schedule(String scope, Long id) {
        return service.createSchedule(month, scope, id, new Target("M09.SCHEDULE", null, null, id))
                .id();
    }

    private ShiftInput input(long template, long staff, Long lead, int from, int to, boolean confirm, Reason reason) {
        Instant start = time.startOfDay(month.atDay(1)).plus(Duration.ofHours(from));
        Instant end = time.startOfDay(month.atDay(1)).plus(Duration.ofHours(to));
        return new ShiftInput(
                template,
                month.atDay(1),
                start,
                end,
                List.of(new ShiftInput.Member(staff, false)),
                lead,
                confirm,
                reason);
    }

    @Test
    void BR_M09_12_equalTemplateTimesCreate24HourShiftAndTemplateEditDoesNotMoveIt() {
        long template = template("FACILITY", false, LocalTime.of(7, 0), LocalTime.of(7, 0));
        long schedule = schedule("FACILITY", null);
        long worker = staff(Role.CAREGIVER);
        var fullDay = new ShiftInput(
                template, month.atDay(1), null, null, List.of(new ShiftInput.Member(worker, false)), null, false, null);
        assertThatThrownBy(() -> service.addShift(schedule, "\"0\"", fullDay, Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo("WARNING_CONFIRMATION_REQUIRED");
        long id = service.addShift(
                        schedule,
                        "\"0\"",
                        new ShiftInput(
                                template,
                                month.atDay(1),
                                null,
                                null,
                                List.of(new ShiftInput.Member(worker, false)),
                                null,
                                true,
                                new Reason("Đã xem cảnh báo ca 24 giờ")),
                        Target.UNSPECIFIED)
                .shiftId();
        var before = service.detail(id);
        assertThat(Duration.between(before.startsAt(), before.endsAt())).isEqualTo(Duration.ofHours(24));
        service.updateTemplate(
                template,
                "\"0\"",
                new TemplateInput("[Mẫu đã sửa]", LocalTime.of(8, 0), LocalTime.of(16, 0), "FACILITY", false, true));
        assertThat(service.detail(id).startsAt()).isEqualTo(before.startsAt());
        assertThat(service.detail(id).endsAt()).isEqualTo(before.endsAt());
    }

    @Test
    void FR_013_overlapAcrossScopesIsBlockedButAdjacentShiftIsAllowed() {
        long worker = staff(Role.NURSING_STAFF);
        long facility = schedule("FACILITY", null);
        long floor = schedule("FLOOR", TestUsers.nextId());
        long a = template("FACILITY", false, LocalTime.of(7, 0), LocalTime.of(15, 0));
        long b = template("FLOOR", true, LocalTime.of(7, 0), LocalTime.of(15, 0));
        service.addShift(facility, "\"0\"", input(a, worker, null, 0, 8, false, null), Target.UNSPECIFIED);
        assertThatThrownBy(() -> service.addShift(
                        floor, "\"0\"", input(b, worker, worker, 7, 15, false, null), Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo("SHIFT_OVERLAP");
        service.addShift(floor, "\"0\"", input(b, worker, worker, 8, 16, false, null), Target.UNSPECIFIED);
    }

    @Test
    void FR_014_exceededContinuousHoursRequireConfirmationAndReason() {
        long worker = staff(Role.CAREGIVER);
        long template = template("FACILITY", false, LocalTime.of(7, 0), LocalTime.of(15, 0));
        long schedule = schedule("FACILITY", null);
        service.addShift(schedule, "\"0\"", input(template, worker, null, 0, 8, false, null), Target.UNSPECIFIED);
        assertThatThrownBy(() -> service.addShift(
                        schedule, "\"1\"", input(template, worker, null, 8, 17, false, null), Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo("WARNING_CONFIRMATION_REQUIRED");
        assertThatThrownBy(() -> service.addShift(
                        schedule, "\"1\"", input(template, worker, null, 8, 17, true, null), Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class);
        service.addShift(
                schedule,
                "\"1\"",
                input(template, worker, null, 8, 17, true, new Reason("Đã xem cảnh báo")),
                Target.UNSPECIFIED);
    }

    @Test
    void FR_002_015_titleCannotSubstituteForLinkedAccountRole() {
        long worker = staff(Role.CAREGIVER);
        jdbc.update("UPDATE staff_profile SET position='NURSING_STAFF' WHERE id=?", worker);
        long template = template("FLOOR", true, LocalTime.of(7, 0), LocalTime.of(15, 0));
        long schedule = schedule("FLOOR", TestUsers.nextId());
        assertThatThrownBy(() -> service.addShift(
                        schedule, "\"0\"", input(template, worker, worker, 0, 8, false, null), Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_016_previewListsBothMissingLeadAndMissingMonthCoverageAndPublishRollsBack() {
        long worker = staff(Role.CAREGIVER);
        long template = template("FLOOR", true, LocalTime.of(7, 0), LocalTime.of(15, 0));
        long schedule = schedule("FLOOR", TestUsers.nextId());
        service.addShift(schedule, "\"0\"", input(template, worker, null, 0, 8, false, null), Target.UNSPECIFIED);
        assertThat(service.preview(schedule).unmetConditions())
                .extracting("code")
                .contains("SHIFT_LEAD_REQUIRED", "FLOOR_MANAGER_REQUIRED");
        assertThatThrownBy(() -> service.publish(schedule, List.of(), false, new Reason("Công bố"), Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM shift_schedule WHERE id=?", String.class, schedule))
                .isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM shift_publication WHERE schedule_id=?", Integer.class, schedule))
                .isZero();
    }

    @Test
    void FR_016_fullMonthCoverageIncludesChangesOfFloorManagerButRejectsAnyGap() {
        long floor = TestUsers.nextId();
        long manager1 = staff(Role.FLOOR_MANAGER);
        long manager2 = staff(Role.FLOOR_MANAGER);
        long schedule = schedule("FLOOR", floor);
        Instant start = time.startOfDay(month.atDay(1));
        Instant middle = time.startOfDay(month.atDay(15));
        Instant end = time.startOfDay(month.plusMonths(1).atDay(1));
        assignments.assign(
                manager1, new AssignmentView(AssignmentKind.FLOOR_MANAGER, floor, null, null, null, start, middle));
        assignments.assign(
                manager2,
                new AssignmentView(AssignmentKind.FLOOR_MANAGER, floor, null, null, null, middle.plusSeconds(1), end));
        assertThat(service.preview(schedule).unmetConditions())
                .extracting("code")
                .contains("FLOOR_MANAGER_REQUIRED");
        assignments
                .unassign(manager2)
                .assign(
                        manager2,
                        new AssignmentView(AssignmentKind.FLOOR_MANAGER, floor, null, null, null, middle, end));
        assertThat(service.preview(schedule).unmetConditions())
                .extracting("code")
                .doesNotContain("FLOOR_MANAGER_REQUIRED");
    }

    @Test
    void FR_055_publicationStoresSnapshotAndAuditButDraftEditsHaveNoPerEditAudit() {
        long worker = staff(Role.CAREGIVER);
        long template = template("FACILITY", false, LocalTime.of(7, 0), LocalTime.of(15, 0));
        long schedule = schedule("FACILITY", null);
        service.addShift(schedule, "\"0\"", input(template, worker, null, 0, 8, false, null), Target.UNSPECIFIED);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.SCHEDULE' AND entity_id=?",
                        Integer.class,
                        Long.toString(schedule)))
                .isZero();
        assertThatThrownBy(() -> service.publish(schedule, List.of(), false, null, Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class);
        service.publish(schedule, List.of(), false, new Reason("Đã rà soát lịch"), Target.UNSPECIFIED);
        assertThat(jdbc.queryForObject("SELECT self_approved FROM shift_schedule WHERE id=?", Boolean.class, schedule))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM shift_publication WHERE schedule_id=?", Integer.class, schedule))
                .isOne();
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.schedule.status')) FROM shift_publication WHERE schedule_id=?",
                        String.class,
                        schedule))
                .isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.shifts[0].shift.status')) FROM shift_publication WHERE schedule_id=?",
                        String.class,
                        schedule))
                .isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.SCHEDULE' AND entity_id=?",
                        Integer.class,
                        Long.toString(schedule)))
                .isOne();
    }

    @Test
    void FR_017_staleVersionAndEditsAfterPublicationAreRejected() {
        long worker = staff(Role.CAREGIVER);
        long template = template("FACILITY", false, LocalTime.of(7, 0), LocalTime.of(15, 0));
        long schedule = schedule("FACILITY", null);
        var input = input(template, worker, null, 0, 8, false, null);
        long id = service.addShift(schedule, "\"0\"", input, Target.UNSPECIFIED).shiftId();
        assertThatThrownBy(() -> service.addShift(schedule, "\"0\"", input, Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo("VERSION_MISMATCH");
        service.publish(schedule, List.of(), false, new Reason("Công bố"), Target.UNSPECIFIED);
        assertThatThrownBy(() -> service.editShift(id, "\"0\"", input, Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_011_shiftMustStartInScheduleMonthAndInactiveTemplateIsNotReusable() {
        long worker = staff(Role.CAREGIVER);
        long template = template("FACILITY", false, LocalTime.of(7, 0), LocalTime.of(15, 0));
        long schedule = schedule("FACILITY", null);
        assertThatThrownBy(() -> service.addShift(
                        schedule,
                        "\"0\"",
                        new ShiftInput(
                                template,
                                month.plusMonths(1).atDay(1),
                                null,
                                null,
                                List.of(new ShiftInput.Member(worker, false)),
                                null,
                                false,
                                null),
                        Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class);
        service.updateTemplate(
                template,
                "\"0\"",
                new TemplateInput("[Ngừng dùng]", LocalTime.of(7, 0), LocalTime.of(15, 0), "FACILITY", false, false));
        assertThatThrownBy(() -> service.addShift(
                        schedule, "\"0\"", input(template, worker, null, 0, 8, false, null), Target.UNSPECIFIED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_013_concurrentAssignmentsInDifferentScopesCannotBothWin() throws Exception {
        long worker = staff(Role.NURSING_STAFF);
        long a = schedule("FLOOR", TestUsers.nextId()), b = schedule("FLOOR", TestUsers.nextId());
        long template = template("FLOOR", true, LocalTime.of(7, 0), LocalTime.of(15, 0));
        var input = input(template, worker, worker, 0, 8, false, null);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var results = List.of(a, b).stream()
                    .map(id -> pool.submit(() -> {
                        ready.countDown();
                        start.await();
                        try {
                            service.addShift(id, "\"0\"", input, Target.UNSPECIFIED);
                            return "CREATED";
                        } catch (BusinessException e) {
                            return e.errorCode();
                        }
                    }))
                    .toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(
                            results.get(0).get(30, TimeUnit.SECONDS),
                            results.get(1).get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", "SHIFT_OVERLAP");
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void FR_054_publicationNotificationContainsOnlyEachRecipientsOwnShifts() {
        long first = staff(Role.CAREGIVER), second = staff(Role.CAREGIVER);
        long schedule = schedule("FACILITY", null);
        long template = template("FACILITY", false, LocalTime.of(7, 0), LocalTime.of(15, 0));
        long a = service.addShift(
                        schedule, "\"0\"", input(template, first, null, 0, 8, false, null), Target.UNSPECIFIED)
                .shiftId();
        long b = service.addShift(
                        schedule, "\"1\"", input(template, second, null, 0, 8, false, null), Target.UNSPECIFIED)
                .shiftId();
        service.publish(schedule, List.of(), false, new Reason("Công bố lịch riêng"), Target.UNSPECIFIED);
        var event = events.stream(SchedulePublished.class)
                .filter(e -> e.scheduleId() == schedule)
                .findFirst()
                .orElseThrow();
        assertThat(event.notificationRequests()).hasSize(2);
        for (var request : event.notificationRequests()) {
            long recipient = ((RecipientGroup.Staff) request.recipientGroups().getFirst()).staffId();
            assertThat(request.displayParams().get("shiftIds")).isEqualTo(List.of(recipient == first ? a : b));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser shiftUser() {
            return new SwitchableUser();
        }
    }
}
