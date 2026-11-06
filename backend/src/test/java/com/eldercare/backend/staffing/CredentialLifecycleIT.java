package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.care.api.dto.TaskRequirements;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.staffing.api.events.*;
import com.eldercare.backend.staffing.internal.credential.domain.*;
import com.eldercare.backend.staffing.internal.credential.persistence.CredentialStore;
import com.eldercare.backend.staffing.internal.credential.service.CredentialService;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.care.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.*;
import org.springframework.test.util.ReflectionTestUtils;

/** T032: hiệu lực theo ngày Việt Nam, cảnh báo một lần, callback và thu hồi nguyên tử. */
@Import({
    FakeM04Config.class,
    com.eldercare.backend.support.fake.roombedallocation.FakeLocations.Config.class,
    CredentialLifecycleIT.Users.class
})
@RecordApplicationEvents
class CredentialLifecycleIT extends MySqlIntegrationTestBase {
    private static final java.util.concurrent.atomic.AtomicInteger sequence =
            new java.util.concurrent.atomic.AtomicInteger();

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    SwitchableUser current;

    @Autowired
    CredentialService service;

    @Autowired
    CredentialStore store;

    @Autowired
    FakeCareRequirements tasks;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeLocations locations;

    @Autowired
    ParameterApi parameters;

    @Autowired
    ApplicationEvents events;

    @Autowired
    com.eldercare.backend.staffing.scheduling.M09DailyJob daily;

    @Autowired
    com.eldercare.backend.staffing.scheduling.M09MinuteJob minute;

    private long person, type, task;
    private Instant now;
    private final List<Long> fixtureShifts = new ArrayList<>(),
            fixtureSchedules = new ArrayList<>(),
            fixtureTemplates = new ArrayList<>();

    @BeforeEach
    void setup() {
        now = Instant.parse("2034-07-01T18:00:00Z").plus(Duration.ofDays(sequence.incrementAndGet()));
        clock.setTo(now);
        current.set(new TestUsers(jdbc, new BusinessTime(Clock.systemUTC()))
                .staff(Role.NURSING_HOME_MANAGER)
                .user());
        var user = new TestUsers(jdbc, new BusinessTime(Clock.systemUTC())).staff(Role.NURSING_STAFF);
        person = user.staffId();
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên chứng chỉ]','Điều dưỡng',?)",
                person,
                user.accountId());
        type = service.addType("[Đào tạo " + person + "]", "TRAINING", true, Set.of("NURSING_STAFF"), false, true)
                .id();
        task = TestUsers.nextId();
        tasks.require(
                task,
                new TaskRequirements(
                        Role.NURSING_STAFF,
                        Set.of(store.type(type).orElseThrow().code())));
        tasks.failOnEnd(false);
    }

    @AfterEach
    void reset() throws Exception {
        clock.reset();
        tasks.failOnEnd(false);
        // Chỉ dọn chứng chỉ/danh mục/ca của test này qua tài khoản lưu giữ, giữ nguyên danh mục khởi tạo.
        try (var c = retentionConnection()) {
            c.setAutoCommit(false);
            var ids = jdbc.queryForList(
                    "SELECT id FROM credential WHERE staff_id=? ORDER BY id DESC", Long.class, person);
            for (long id : ids) remove(c, "credential", id);
            remove(c, "credential_type", type);
            try (var p = c.prepareStatement("DELETE FROM staff_assignment WHERE staff_id=?")) {
                p.setLong(1, person);
                p.executeUpdate();
            }
            for (long shift : fixtureShifts) {
                for (String statement : List.of(
                        "DELETE FROM status_history WHERE entity_type='M09.SHIFT' AND attribute='LEAD_STAFF' AND entity_id=?",
                        "DELETE FROM shift_staff WHERE shift_id=?")) {
                    try (var prepared = c.prepareStatement(statement)) {
                        prepared.setLong(1, shift);
                        prepared.executeUpdate();
                    }
                }
                remove(c, "work_shift", shift);
            }
            for (long id : fixtureSchedules) remove(c, "shift_schedule", id);
            for (long id : fixtureTemplates) remove(c, "shift_template", id);
            c.commit();
        }
    }

    private void remove(java.sql.Connection c, String table, long id) throws Exception {
        String statement = switch (table) {
            case "credential_type" -> "DELETE FROM catalog_item WHERE catalog_type='M09.CREDENTIAL_TYPE' AND id=?";
            case "shift_template" -> "DELETE FROM catalog_item WHERE catalog_type='M09.SHIFT_TEMPLATE' AND id=?";
            default -> "DELETE FROM " + table + " WHERE id=?";
        };
        try (var p = c.prepareStatement(statement)) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    private CredentialInput input(String number, LocalDate expiry) {
        return new CredentialInput(
                type, number, "[Đơn vị cấp]", Set.of(), time.today().minusDays(10), expiry, null);
    }

    private long add(LocalDate expiry) {
        return service.add(person, input("T-1", expiry)).id();
    }

    private void revoke(long id, LocalDate day) {
        ReflectionTestUtils.invokeMethod(service, "revoke", id, day, new Reason("Thu hồi theo quyết định"));
    }

    private long assignment(Instant from, Instant to, String state) {
        long floor = TestUsers.nextId(), room = TestUsers.nextId();
        locations
                .floor(floor, TestUsers.nextId())
                .room(room, floor)
                .stay(TestUsers.nextId(), room, from.minusSeconds(60), null);
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu ca chứng chỉ] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'23:00' AS start_time,'07:00' AS end_time,'FLOOR' AS scope_type,FALSE AS requires_handover) src");
        fixtureTemplates.add(template);
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (?,'FLOOR',?,'PUBLISHED')",
                YearMonth.from(from.atZone(BusinessTime.ZONE)).toString(),
                floor);
        fixtureSchedules.add(schedule);
        long shift = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES (?,?,?,?,'FLOOR',?,FALSE,'IN_PROGRESS')",
                schedule,
                template,
                ShiftStore.sql(from),
                ShiftStore.sql(to),
                floor);
        fixtureShifts.add(shift);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, person);
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,task_type_id,role,assignment_kind,starts_at,ends_at,status) VALUES (?,?,'FLOOR',?,?,'NURSING_STAFF','PRIMARY',?,?,?)",
                shift,
                person,
                floor,
                task,
                ShiftStore.sql(from),
                ShiftStore.sql(to),
                state);
    }

    @Test
    void BR_M09_05_requiredTrainingWarnsOnceAtConfiguredHorizonAndRenewalClosesWarning() {
        var expiry = time.today().plus(parameters.read(ParameterCode.CFG_M09_03).asPeriod());
        long id = add(expiry);
        daily.runNow();
        daily.runNow();
        assertThat(events.stream(CredentialExpiring.class)
                        .filter(e -> e.credentialId() == id)
                        .count())
                .isOne();
        Credential renewed = ReflectionTestUtils.invokeMethod(service, "renew", id, input("T-2", expiry.plusMonths(1)));
        assertThat(renewed.replacesId()).isEqualTo(id);
        assertThat(store.find(id).orElseThrow().replaced()).isTrue();
        assertThat(store.find(id).orElseThrow().credentialNumber()).isEqualTo("T-1");
        assertThat(events.stream(CredentialExpiryWarningClosed.class)
                        .filter(e -> e.credentialId() == id)
                        .count())
                .isOne();
    }

    @Test
    void FR_008_professionalLicenseAndNonRequiredTrainingDoNotRaiseM09TrainingWarning() {
        long license = jdbc.queryForObject(
                "SELECT id FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,COALESCE(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacyCode'),'null'),c.code) AS code,NULLIF(JSON_UNQUOTE(c.attributes->'$.credentialGroup'),'null') AS credential_group,(c.attributes->'$.hasExpiry'=CAST('true' AS JSON)) AS has_expiry,c.attributes->'$.requiredForRoles' AS required_for_roles,(c.attributes->'$.hasPracticeScopes'=CAST('true' AS JSON)) AS has_practice_scopes FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE') credential_type WHERE code='NURSING_LICENSE'",
                Long.class);
        long id = service.add(
                        person,
                        new CredentialInput(
                                license,
                                "L-1",
                                null,
                                Set.of(),
                                time.today().minusDays(1),
                                time.today().plusDays(1),
                                null))
                .id();
        daily.runNow();
        assertThat(events.stream(CredentialExpiring.class)
                        .filter(e -> e.credentialId() == id)
                        .count())
                .isZero();
    }

    @Test
    void FR_009_revocationEndsOnlyRequiredAssignmentsAndKeepsShiftMembership() {
        long id = add(time.today().plusDays(10));
        long assigned = assignment(now.minusSeconds(7200), now.plusSeconds(21600), "ACTIVE");
        revoke(id, time.today());
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assigned))
                .isEqualTo("ENDED");
        assertThat(jdbc.queryForObject(
                                "SELECT actual_end_at FROM staff_assignment WHERE id=?",
                                java.sql.Timestamp.class,
                                assigned)
                        .toInstant())
                .isEqualTo(time.startOfDay(time.today()));
        assertThat(tasks.endedAssignments()).contains(assigned);
        var notice = events.stream(AssignmentsEndedByCredential.class)
                .filter(e -> e.staffId() == person)
                .findFirst()
                .orElseThrow();
        String content =
                notice.notificationRequests().getFirst().content().getFirst().content();
        for (long ref : notice.shiftIds()) assertThat(content).contains("#" + ref);
        for (long ref : notice.residentIds()) assertThat(content).contains("#" + ref);
        assertThat(jdbc.queryForObject(
                        "SELECT withdrawn_on_resignation_at FROM shift_staff WHERE staff_id=?",
                        java.sql.Timestamp.class,
                        person))
                .isNull();
    }

    @Test
    void FR_009_futureRevocationWaitsUntilItsDateThenAppliesOnce() {
        long id = add(time.today().plusDays(10));
        var cut = time.startOfDay(time.today().plusDays(1));
        long assigned = assignment(cut.minusSeconds(3600), cut.plusSeconds(21600), "SCHEDULED");
        revoke(id, time.today().plusDays(1));
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assigned))
                .isEqualTo("SCHEDULED");
        clock.setTo(cut.plusSeconds(1));
        minute.runNow();
        minute.runNow();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assigned))
                .isEqualTo("ENDED");
        assertThat(tasks.endedAssignments().stream().filter(a -> a == assigned).count())
                .isOne();
    }

    @Test
    void FR_009_careFailureRollsBackCredentialRevocationAndAssignment() {
        long id = add(time.today().plusDays(10));
        long assigned = assignment(now.minusSeconds(7200), now.plusSeconds(21600), "ACTIVE");
        tasks.failOnEnd(true);
        assertThatThrownBy(() -> revoke(id, time.today()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Nguồn công việc từ chối kết thúc");
        assertThat(store.find(id).orElseThrow().revokedFrom()).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assigned))
                .isEqualTo("ACTIVE");
    }

    @Test
    void FR_009_correctingExpiryEarlierEndsAffectedAssignmentWithoutRewritingSourceIdentity() {
        long id = add(time.today().plusDays(10));
        long assigned = assignment(now.minusSeconds(7200), now.plusSeconds(21600), "ACTIVE");
        ReflectionTestUtils.invokeMethod(
                service,
                "correct",
                id,
                input("T-1 đúng", time.today().minusDays(1)),
                new Reason("Sửa ngày trên bản gốc"));
        assertThat(store.find(id).orElseThrow().id()).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assigned))
                .isEqualTo("ENDED");
    }

    @Test
    void FR_009_validAlternativeKeepsAssignmentAndShiftMembership() {
        long id = add(time.today().plusDays(10));
        add(time.today().plusDays(20));
        long assigned = assignment(now.minusSeconds(7200), now.plusSeconds(21600), "ACTIVE");
        revoke(id, time.today());
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assigned))
                .isEqualTo("ACTIVE");
        assertThat(tasks.endedAssignments()).doesNotContain(assigned);
    }

    @Test
    void FR_009_revocationCancelsFutureAssignmentWithoutCallingEndCallback() {
        long id = add(time.today().plusDays(10));
        long assigned = assignment(now.plusSeconds(7200), now.plusSeconds(21600), "SCHEDULED");
        revoke(id, time.today());
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assigned))
                .isEqualTo("CANCELLED");
        assertThat(tasks.endedAssignments()).doesNotContain(assigned);
    }

    @Test
    void FR_008_optionalTrainingDoesNotWarn() {
        long optional = jdbc.queryForObject(
                "SELECT id FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,COALESCE(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacyCode'),'null'),c.code) AS code,NULLIF(JSON_UNQUOTE(c.attributes->'$.credentialGroup'),'null') AS credential_group,(c.attributes->'$.hasExpiry'=CAST('true' AS JSON)) AS has_expiry,c.attributes->'$.requiredForRoles' AS required_for_roles,(c.attributes->'$.hasPracticeScopes'=CAST('true' AS JSON)) AS has_practice_scopes FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE') credential_type WHERE code='FIRST_AID_TRAINING'",
                Long.class);
        long id = service.add(
                        person,
                        new CredentialInput(
                                optional,
                                "OPT",
                                null,
                                Set.of(),
                                time.today().minusDays(1),
                                time.today().plusDays(1),
                                null))
                .id();
        daily.runNow();
        assertThat(events.stream(CredentialExpiring.class)
                        .filter(e -> e.credentialId() == id)
                        .count())
                .isZero();
    }

    @Test
    void FR_009_changingTypeStillSchedulesFutureInvalidityForCorrectedType() {
        long id = add(time.today().plusDays(10));
        long target = jdbc.queryForObject(
                "SELECT id FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,COALESCE(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacyCode'),'null'),c.code) AS code,NULLIF(JSON_UNQUOTE(c.attributes->'$.credentialGroup'),'null') AS credential_group,(c.attributes->'$.hasExpiry'=CAST('true' AS JSON)) AS has_expiry,c.attributes->'$.requiredForRoles' AS required_for_roles,(c.attributes->'$.hasPracticeScopes'=CAST('true' AS JSON)) AS has_practice_scopes FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE') credential_type WHERE code='FIRST_AID_TRAINING'",
                Long.class);
        var code = store.type(target).orElseThrow().code();
        tasks.require(task, new TaskRequirements(Role.NURSING_STAFF, Set.of(code)));
        var expiry = time.today().plusDays(1);
        var cut = time.startOfDay(expiry.plusDays(1));
        long assigned = assignment(cut.minusSeconds(3600), cut.plusSeconds(21600), "SCHEDULED");
        service.correct(
                id,
                new CredentialInput(
                        target, "T-correct", null, Set.of(), time.today().minusDays(1), expiry, null),
                new Reason("Sửa đúng loại trên bản gốc"));
        clock.setTo(cut.plusSeconds(1));
        minute.runNow();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assigned))
                .isEqualTo("ENDED");
    }

    @Test
    void FR_008_trainingReminderIncludesFloorsInsideFutureAreaShift() {
        long area = TestUsers.nextId(), floor = TestUsers.nextId();
        locations.floor(floor, area);
        var expiry = time.today().plusDays(1);
        long credential = add(expiry);
        var from = time.startOfDay(expiry.plusDays(1));
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Ca khu chứng chỉ] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'AREA' AS scope_type,FALSE AS requires_handover) src");
        fixtureTemplates.add(template);
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (?,'AREA',?,'PUBLISHED')",
                YearMonth.from(from.atZone(BusinessTime.ZONE)).toString(),
                area);
        fixtureSchedules.add(schedule);
        long shift = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES (?,?,?,?,'AREA',?,FALSE,'PUBLISHED')",
                schedule,
                template,
                ShiftStore.sql(from),
                ShiftStore.sql(from.plusSeconds(28800)),
                area);
        fixtureShifts.add(shift);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, person);
        daily.runNow();
        var reminder = events.stream(CredentialExpiring.class)
                .filter(e -> e.credentialId() == credential)
                .findFirst()
                .orElseThrow()
                .notificationRequests()
                .getFirst();
        assertThat(reminder.recipientGroups())
                .contains(new com.eldercare.backend.common.notification.RecipientGroup.FloorManager(
                        floor, new com.eldercare.backend.common.notification.Substitution.Default()));
        assertThat(reminder.content().getFirst().content()).contains("[Nhân viên chứng chỉ]", Long.toString(shift));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser current() {
            return new SwitchableUser();
        }
    }
}
