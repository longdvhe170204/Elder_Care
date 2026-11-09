package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.admission.FakeM02Config;
import com.eldercare.backend.support.fake.care.*;
import com.eldercare.backend.support.fake.roombedallocation.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/** T039: HTTP/quyền thật, chỉ nguồn chưa có của B/C dùng bản giả chung. */
@AutoConfigureMockMvc
@Import({FakeM02Config.class, FakeLocations.Config.class, FakeM04Config.class, PostPublicationShiftIT.Time.class})
class PostPublicationShiftIT extends MySqlIntegrationTestBase {
    @Autowired
    com.eldercare.backend.staffing.internal.handover.persistence.HandoverStore embeddedStore;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeLocations locations;

    @Autowired
    FakeCareRequirements tasks;

    @Autowired
    FieldVisibilityApi visibility;

    @Autowired
    com.eldercare.backend.administration.parameter.api.ParameterApi parameters;

    private final Map<String, tools.jackson.databind.JsonNode> originalParameters = new LinkedHashMap<>();

    final Instant from = Instant.parse("2026-10-25T00:00:00Z"), to = from.plusSeconds(28800);
    TestUsers users;
    TestUsers.TestUser manager, head, lead, worker, replacement, outside;
    long floor, shift, member, assignment;
    String managerToken, headToken, leadToken, workerToken;

    private TestUsers.TestUser person(Role role) {
        long used = jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM staff_profile", Long.class);
        while (TestUsers.nextId() <= used) {
            /* Giữ id không trùng các hồ sơ import ở test trước. */
        }
        var user = users.staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên kiểm thử lệnh ca]','Chăm sóc',?)",
                user.staffId(),
                user.accountId());
        return user;
    }

    @BeforeEach
    void setup() {
        clock.setTo(from.plusSeconds(1200));
        users = new TestUsers(jdbc, time);
        manager = person(Role.NURSING_HOME_MANAGER);
        head = person(Role.FLOOR_MANAGER);
        lead = person(Role.NURSING_STAFF);
        worker = person(Role.CAREGIVER);
        replacement = person(Role.NURSING_STAFF);
        outside = person(Role.FLOOR_MANAGER);
        managerToken = token(manager);
        headToken = token(head);
        leadToken = token(lead);
        workerToken = token(worker);
        floor = TestUsers.nextId();
        locations.floor(floor, null);
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu lệnh ca] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type,TRUE AS requires_handover) src");
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2026-10','FLOOR',?,'PUBLISHED')",
                floor);
        shift = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status,lead_staff_id) VALUES (?,?,?,?,'FLOOR',?,TRUE,'IN_PROGRESS',?)",
                schedule,
                template,
                ShiftStore.sql(from),
                ShiftStore.sql(to),
                floor,
                lead.staffId());
        jdbc.update(
                "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,assigned_by,reason,status) SELECT 'FLOOR_MANAGER',src.staff_id,src.floor_area_id,src.starts_at,1,'[Khởi tạo fixture]','ACTIVE' FROM (SELECT ? AS staff_id,? AS floor_area_id,? AS starts_at) src",
                head.staffId(),
                floor,
                ShiftStore.sql(from.minusSeconds(86400)));
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, lead.staffId());
        member = AssignmentFixtures.insert(
                jdbc, "INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, worker.staffId());
        jdbc.update(
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,command,by_system) SELECT 'M09.SHIFT',src.shift_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,src.basis,FALSE FROM (SELECT ? AS shift_id,? AS staff_id,? AS starts_at,'SCHEDULED' AS basis) src",
                shift,
                lead.staffId(),
                ShiftStore.sql(from));
        assignment = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at,status) VALUES (?,?,'FLOOR',?,'CAREGIVER','PRIMARY',?,?,'ACTIVE')",
                shift,
                worker.staffId(),
                floor,
                ShiftStore.sql(from),
                ShiftStore.sql(to));
        tasks.onAbsent((staff, ca) -> {});
        tasks.failOnEnd(false);
    }

    @AfterEach
    void reset() throws Exception {
        tasks.onAbsent((staff, ca) -> {});
        for (var original : originalParameters.entrySet())
            changeParameter(
                    original.getKey(),
                    original.getValue().isNull()
                            ? Map.of("restoreDefault", true)
                            : Map.of("value", original.getValue()));
        originalParameters.clear();
        clock.reset();
    }

    private void parameter(String code, Object value) throws Exception {
        originalParameters.put(
                code,
                parameters
                        .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.valueOf(code))
                        .raw());
        changeParameter(code, Map.of("value", value));
    }

    private void changeParameter(String code, Map<String, Object> value) throws Exception {
        clock.advanceBy(Duration.ofMillis(1));
        var body = new HashMap<String, Object>(value);
        body.put("reason", "[Cấu hình tỷ lệ kiểm thử]");
        var apiCode = code.replace('_', '-');
        long version = jdbc.queryForObject("SELECT version FROM parameter WHERE code=?", Long.class, apiCode);
        mvc.perform(post("/api/v1/parameters/{code}/change-value", apiCode)
                        .header("Authorization", token(manager))
                        .header("If-Match", "\"" + version + "\"")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private String token(TestUsers.TestUser user) {
        return TestUsers.bearer(users.session(user.accountId()));
    }

    private ResultActions command(String action, String token, Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/v1/shifts/{id}/" + action, shift)
                .header("Authorization", token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private Map<String, Object> reason(long staff) {
        return Map.of("staffId", staff, "reason", "[Lý do riêng tư kiểm thử]");
    }

    @Test
    void FR_020_absenceEndsOnlyThisShiftAssignments_andCallsTasksInSameTransaction() throws Exception {
        parameter(
                "CFG_M09_01",
                Map.of(
                        jdbc.queryForObject("SELECT template_id FROM work_shift WHERE id=?", Long.class, shift)
                                .toString(),
                        8));
        parameter("CFG_M09_12", Map.of("BASIC", 1, "REGULAR", 1.5, "SPECIAL", 2.5));
        var called = new AtomicInteger();
        tasks.onAbsent((staff, ca) -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .isTrue();
            assertThat(staff).isEqualTo(worker.staffId());
            assertThat(ca).isEqualTo(shift);
            called.incrementAndGet();
        });
        command("record-absence", leadToken, reason(worker.staffId())).andExpect(status().isOk());
        assertThat(called).hasValue(1);
        assertThat(jdbc.queryForObject("SELECT absent_at FROM shift_staff WHERE id=?", Object.class, member))
                .isNotNull();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assignment))
                .isEqualTo("ENDED");
        assertThat(jdbc.queryForObject(
                        "SELECT actual_end_at FROM staff_assignment WHERE id=?", LocalDateTime.class, assignment))
                .isEqualTo(LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(e.content->'$.legacy.id'),'null') AS UNSIGNED),e.id) AS id,e.entity_id AS shift_id,e.occurred_at AS calculated_at,NULLIF(JSON_UNQUOTE(e.content->'$.event'),'null') AS event,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.totalWeight'),'null') AS DECIMAL(8,2)) AS total_weight,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.staffCount'),'null') AS SIGNED) AS staff_count,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.ratio'),'null') AS DECIMAL(8,2)) AS ratio,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.threshold'),'null') AS DECIMAL(8,2)) AS threshold,(e.content->'$.meetsThreshold'=CAST('true' AS JSON)) AS meets_threshold FROM record_event e WHERE e.kind='M09.STAFFING_RATIO_CALCULATION') staffing_ratio_calculation WHERE shift_id=? AND event='STAFF_ABSENT'",
                        Integer.class,
                        shift))
                .isEqualTo(1);
        command("record-absence", leadToken, reason(worker.staffId())).andExpect(status().isConflict());
    }

    @Test
    void FR_020_taskFailureRollsBackAbsenceAssignmentsAuditAndRatio() throws Exception {
        var called = new AtomicInteger();
        tasks.onAbsent((staff, ca) -> {
            called.incrementAndGet();
            throw new IllegalStateException("[Nguồn công việc lỗi kiểm thử]");
        });
        try {
            command("record-absence", headToken, reason(worker.staffId()));
        } catch (jakarta.servlet.ServletException expected) {
        }
        assertThat(called).hasValue(1);
        assertThat(jdbc.queryForObject("SELECT absent_at FROM shift_staff WHERE id=?", Object.class, member))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assignment))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.STAFF_ABSENCE' AND entity_id=?",
                        Integer.class,
                        Long.toString(member)))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(e.content->'$.legacy.id'),'null') AS UNSIGNED),e.id) AS id,e.entity_id AS shift_id,e.occurred_at AS calculated_at,NULLIF(JSON_UNQUOTE(e.content->'$.event'),'null') AS event,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.totalWeight'),'null') AS DECIMAL(8,2)) AS total_weight,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.staffCount'),'null') AS SIGNED) AS staff_count,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.ratio'),'null') AS DECIMAL(8,2)) AS ratio,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.threshold'),'null') AS DECIMAL(8,2)) AS threshold,(e.content->'$.meetsThreshold'=CAST('true' AS JSON)) AS meets_threshold FROM record_event e WHERE e.kind='M09.STAFFING_RATIO_CALCULATION') staffing_ratio_calculation WHERE shift_id=?",
                        Integer.class,
                        shift))
                .isZero();
    }

    @Test
    void FR_020_twoConcurrentAbsenceCommandsApplyExactlyOnce() throws Exception {
        var latch = new java.util.concurrent.CountDownLatch(1);
        var calls = new AtomicInteger();
        tasks.onAbsent((staff, ca) -> calls.incrementAndGet());
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> {
                latch.await();
                return command("record-absence", headToken, reason(worker.staffId()))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            var b = pool.submit(() -> {
                latch.await();
                return command("record-absence", headToken, reason(worker.staffId()))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            latch.countDown();
            assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(calls).hasValue(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.STAFF_ABSENCE' AND entity_id=?",
                        Integer.class,
                        Long.toString(member)))
                .isEqualTo(1);
    }

    /** Giữ chính xác biên giờ để kiểm [bắt đầu-CFG, kết thúc], không sửa đồng hồ chung của A. */
    static class FrozenClock extends TestClockControl {
        private volatile Instant fixed;

        @Override
        public Instant instant() {
            return fixed == null ? super.instant() : fixed;
        }

        @Override
        public void setTo(Instant at) {
            fixed = at;
        }

        @Override
        public void advanceBy(Duration duration) {
            fixed = instant().plus(duration);
        }

        @Override
        public void reset() {
            fixed = null;
            super.reset();
        }
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class Time {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        FrozenClock phase8Clock() {
            return new FrozenClock();
        }
    }
}
