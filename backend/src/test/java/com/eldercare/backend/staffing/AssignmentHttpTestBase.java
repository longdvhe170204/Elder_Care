package com.eldercare.backend.staffing;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.admission.FakeM02Config;
import com.eldercare.backend.support.fake.care.*;
import com.eldercare.backend.support.fake.resident.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/** Fixture riêng của T054/T055, dùng khóa INSERT chung và nguồn giả đúng chủ. */
@AutoConfigureMockMvc
@Import({
    FakeM01Config.class,
    FakeLocations.Config.class,
    FakeM04Config.class,
    FakeM02Config.class,
    PostPublicationShiftIT.Time.class
})
abstract class AssignmentHttpTestBase extends MySqlIntegrationTestBase {
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
    FakeResidents residents;

    @Autowired
    FakeCareRequirements tasks;

    TestUsers users;
    final List<Long> fixtureAccounts = new ArrayList<>();
    TestUsers.TestUser manager, worker, other;
    String bearer;
    long floor, room, resident, shift, schedule, assignment;
    final Instant from = Instant.parse("2026-11-07T15:00:00Z"), to = from.plusSeconds(28800);

    @BeforeEach
    void fixture() {
        // Khép cả khoảng vai trò tương lai của dữ liệu do chính fixture này để lại.
        jdbc.update(
                "UPDATE permission_assignment r JOIN staff_assignment x ON x.created_by=r.account_id JOIN work_shift s ON s.id=x.shift_id JOIN catalog_item t ON t.catalog_type='M09.SHIFT_TEMPLATE' AND t.id=s.template_id SET r.effective_to=TIMESTAMPADD(MICROSECOND,1,r.effective_from) WHERE r.kind='ROLE_ASSIGNMENT' AND r.effective_to IS NULL AND t.name LIKE '[Ca test hợp đồng]%' AND s.starts_at=? AND s.ends_at=?",
                ShiftStore.sql(from), ShiftStore.sql(to));
        jdbc.update(
                "UPDATE permission_assignment r JOIN staff_profile p ON p.account_id=r.account_id JOIN shift_staff m ON m.staff_id=p.id JOIN work_shift s ON s.id=m.shift_id JOIN catalog_item t ON t.catalog_type='M09.SHIFT_TEMPLATE' AND t.id=s.template_id SET r.effective_to=TIMESTAMPADD(MICROSECOND,1,r.effective_from) WHERE r.kind='ROLE_ASSIGNMENT' AND r.effective_to IS NULL AND t.name LIKE '[Ca test hợp đồng]%' AND s.starts_at=? AND s.ends_at=?",
                ShiftStore.sql(from), ShiftStore.sql(to));
        // Tài khoản do các lượt trước của chính fixture này tạo không nhận thông báo nhóm ở test sau.
        jdbc.update(
                "UPDATE account a JOIN staff_assignment x ON x.created_by=a.id JOIN work_shift s ON s.id=x.shift_id JOIN catalog_item t ON t.catalog_type='M09.SHIFT_TEMPLATE' AND t.id=s.template_id SET a.status='INACTIVE' WHERE t.name LIKE '[Ca test hợp đồng]%' AND s.starts_at=? AND s.ends_at=?",
                ShiftStore.sql(from), ShiftStore.sql(to));
        jdbc.update(
                "UPDATE account a JOIN staff_profile p ON p.account_id=a.id JOIN shift_staff m ON m.staff_id=p.id JOIN work_shift s ON s.id=m.shift_id JOIN catalog_item t ON t.catalog_type='M09.SHIFT_TEMPLATE' AND t.id=s.template_id SET a.status='INACTIVE' WHERE t.name LIKE '[Ca test hợp đồng]%' AND s.starts_at=? AND s.ends_at=?",
                ShiftStore.sql(from), ShiftStore.sql(to));
        // Cô lập cả dữ liệu do chính fixture này để lại từ các lượt đỏ/xanh trước.
        jdbc.update(
                "UPDATE staff_assignment a JOIN work_shift s ON s.id=a.shift_id JOIN catalog_item t ON t.catalog_type='M09.SHIFT_TEMPLATE' AND t.id=s.template_id SET a.status='CANCELLED' WHERE a.status NOT IN ('ENDED','CANCELLED') AND t.name LIKE '[Ca test hợp đồng]%' AND s.starts_at=? AND s.ends_at=?",
                ShiftStore.sql(from), ShiftStore.sql(to));
        jdbc.update(
                "UPDATE work_shift s JOIN catalog_item t ON t.catalog_type='M09.SHIFT_TEMPLATE' AND t.id=s.template_id SET s.status='CANCELLED' WHERE s.status NOT IN ('CLOSED','CANCELLED') AND t.name LIKE '[Ca test hợp đồng]%' AND s.starts_at=? AND s.ends_at=?",
                ShiftStore.sql(from), ShiftStore.sql(to));
        clock.setTo(from.minusSeconds(3600));
        long used = jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM staff_profile", Long.class);
        while (TestUsers.nextId() <= used) {
            /* Không đụng dữ liệu import còn lại. */
        }
        users = new TestUsers(jdbc, time);
        manager = person(Role.NURSING_HOME_MANAGER);
        worker = person(Role.CAREGIVER);
        other = person(Role.CAREGIVER);
        bearer = token(manager);
        floor = TestUsers.nextId();
        room = TestUsers.nextId();
        resident = TestUsers.nextId();
        locations.floor(floor, null).room(room, floor).stay(resident, room, from.minusSeconds(3600), null);
        residents.resident(resident, "[Họ tên NCT kiểm thử]", null, ResidentStatus.IN_RESIDENCE);
        long template = insert(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES ('M09.SHIFT_TEMPLATE','M09',?,'ACTIVE',JSON_OBJECT('createdAt','2026-11-07T14:00:00Z','startTime','22:00','endTime','06:00','scopeType','FLOOR','requiresHandover',CAST('false' AS JSON),'originalName','[Ca test hợp đồng]'))",
                "[Ca test hợp đồng] #" + floor);
        schedule = insert(
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2026-11','FLOOR',?,'DRAFT')",
                floor);
        shift = insert(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover,status) VALUES (?,?,'FLOOR',?,?,?,FALSE,'DRAFT')",
                schedule,
                template,
                floor,
                ShiftStore.sql(from),
                ShiftStore.sql(to));
        jdbc.update(
                "INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?),(?,?)",
                shift,
                worker.staffId(),
                shift,
                other.staffId());
        assignment = assign(worker.staffId(), "FLOOR", floor, "CAREGIVER", "PRIMARY", false, null);
    }

    @AfterEach
    void reset() {
        // Không để ca giả còn hiệu lực cho các job quét toàn schema ở lớp test sau.
        jdbc.update(
                "UPDATE staff_assignment SET status='CANCELLED' WHERE shift_id=? AND status NOT IN ('ENDED','CANCELLED')",
                shift);
        jdbc.update(
                "UPDATE work_shift SET status='CANCELLED' WHERE id=? AND status NOT IN ('CLOSED','CANCELLED')", shift);
        for (long account : fixtureAccounts) {
            jdbc.update(
                    "UPDATE permission_assignment SET effective_to=TIMESTAMPADD(MICROSECOND,1,effective_from) WHERE kind='ROLE_ASSIGNMENT' AND account_id=? AND effective_to IS NULL",
                    account);
            jdbc.update("UPDATE account SET status='INACTIVE' WHERE id=?", account);
        }
        clock.reset();
        tasks.failOnEnd(false);
    }

    TestUsers.TestUser person(Role role) {
        var user = users.staff(role);
        fixtureAccounts.add(user.accountId());
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên kiểm thử]','Chức danh',?)",
                user.staffId(),
                user.accountId());
        return user;
    }

    String token(TestUsers.TestUser user) {
        return TestUsers.bearer(users.session(user.accountId()));
    }

    long insert(String sql, Object... args) {
        return AssignmentFixtures.insert(jdbc, sql, args);
    }

    long assign(long staff, String type, long target, String role, String kind, boolean nurse, Long task) {
        return insert(
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,task_type_id,role,assignment_kind,assigned_nurse,starts_at,ends_at,status,source,created_by) VALUES (?,?,?,?,?,?,?,?,?,?,'DRAFT','MANUAL',?)",
                shift,
                staff,
                type,
                target,
                task,
                role,
                kind,
                nurse,
                ShiftStore.sql(from),
                ShiftStore.sql(to),
                manager.accountId());
    }

    Map<String, Object> body() {
        return new LinkedHashMap<>(Map.of(
                "staffId",
                worker.staffId(),
                "targetType",
                "FLOOR",
                "targetId",
                floor,
                "role",
                "CAREGIVER",
                "assignmentKind",
                "PRIMARY",
                "assignedNurse",
                false));
    }

    void publish() {
        jdbc.update("UPDATE shift_schedule SET status='PUBLISHED' WHERE id=?", schedule);
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
        jdbc.update("UPDATE staff_assignment SET status='SCHEDULED',version=version+1 WHERE shift_id=?", shift);
    }
}
