package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.staffing.internal.shift.domain.ShiftInput;
import com.eldercare.backend.staffing.internal.shift.domain.TemplateInput;
import com.eldercare.backend.staffing.internal.shift.service.ShiftScheduleService;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** T011/T013: lệnh thật, quyền theo ma trận, dữ liệu ca thật; chỉ nguồn nhiệm vụ của Phase 4 dùng bản giả 002. */
@Import({FakeM15Config.class, SwitchableUserConfig.class})
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
        return service.createTemplate(new TemplateInput(
                        "[Mẫu ca kiểm thử " + TestUsers.nextId() + "]", from, to, scope, handover, true))
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
                        "SELECT COUNT(*) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(e.content->'$.legacy.id'),'null') AS UNSIGNED),e.id) AS id,e.entity_id AS schedule_id,e.occurred_at AS published_at,e.content->'$.content' AS content FROM record_event e WHERE e.kind='M09.SHIFT_PUBLICATION') shift_publication WHERE schedule_id=?",
                        Integer.class,
                        schedule))
                .isOne();
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.schedule.status')) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(e.content->'$.legacy.id'),'null') AS UNSIGNED),e.id) AS id,e.entity_id AS schedule_id,e.occurred_at AS published_at,e.content->'$.content' AS content FROM record_event e WHERE e.kind='M09.SHIFT_PUBLICATION') shift_publication WHERE schedule_id=?",
                        String.class,
                        schedule))
                .isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.shifts[0].shift.status')) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(e.content->'$.legacy.id'),'null') AS UNSIGNED),e.id) AS id,e.entity_id AS schedule_id,e.occurred_at AS published_at,e.content->'$.content' AS content FROM record_event e WHERE e.kind='M09.SHIFT_PUBLICATION') shift_publication WHERE schedule_id=?",
                        String.class,
                        schedule))
                .isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.SCHEDULE' AND entity_id=?",
                        Integer.class,
                        Long.toString(schedule)))
                .isOne();
    }
}
