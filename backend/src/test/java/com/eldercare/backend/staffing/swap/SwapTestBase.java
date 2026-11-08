package com.eldercare.backend.staffing.swap;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.admission.FakeM02Config;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/** Nền IT của 015: HTTP và quyền thật, ca Đã công bố ở tương lai; nguồn của B, C dùng bản giả chung. */
@AutoConfigureMockMvc
@Import({FakeM02Config.class, FakeLocations.Config.class, FakeM04Config.class})
abstract class SwapTestBase extends MySqlIntegrationTestBase {

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JsonMapper json;

    @Autowired
    protected BusinessTime time;

    @Autowired
    protected TestClockControl clock;

    @Autowired
    protected FakeLocations locations;

    /** Tác động kèm lệnh của 005 (bản giả chung chưa có fixture cho onApprovedLeave): ghi lại lệnh gọi. */
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.eldercare.backend.care.api.ShiftTaskApi shiftTasks;

    protected TestUsers users;
    protected TestUsers.TestUser manager;
    protected TestUsers.TestUser head;
    protected TestUsers.TestUser giver;
    protected TestUsers.TestUser recipient;
    protected TestUsers.TestUser third;
    protected long floor;
    protected long templateId;
    protected long scheduleId;
    protected Instant base;

    /** Lấy khóa trên chính connection thực hiện INSERT (như AssignmentFixtures của 008). */
    protected static long insertRow(JdbcTemplate jdbc, String sql, Object... args) {
        var keys = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    private long insert(JdbcTemplate jdbc, String sql, Object... args) {
        return insertRow(jdbc, sql, args);
    }

    protected TestUsers.TestUser person(Role role) {
        long used = jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM staff_profile", Long.class);
        while (TestUsers.nextId() <= used) {
            /* Giữ id không trùng các hồ sơ import ở test trước. */
        }
        TestUsers.TestUser user = users.staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id,status) VALUES (?,'[Nhân viên kiểm thử đổi ca]','Chăm sóc',?,'WORKING')",
                user.staffId(),
                user.accountId());
        return user;
    }

    @BeforeEach
    void setUpSwap() {
        clock.reset();
        users = new TestUsers(jdbc, time);
        manager = person(Role.NURSING_HOME_MANAGER);
        head = person(Role.FLOOR_MANAGER);
        giver = person(Role.NURSING_STAFF);
        recipient = person(Role.NURSING_STAFF);
        third = person(Role.NURSING_STAFF);
        floor = TestUsers.nextId();
        locations.floor(floor, null);
        // Ba ngày sau, giữa ngày, để mọi ca của test nằm trong tương lai và cùng một tháng khi có thể.
        base = time.now().plus(Duration.ofDays(3)).truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        templateId = insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu ca đổi ca] #"
                        + TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type,FALSE AS requires_handover) src");
        scheduleId = insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (?,'FLOOR',?,'PUBLISHED')",
                YearMonth.from(base.atZone(BusinessTime.ZONE)).toString(),
                floor);
        jdbc.update(
                "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,assigned_by,reason,status) SELECT 'FLOOR_MANAGER',src.staff_id,src.floor_area_id,src.starts_at,1,'[Khởi tạo fixture]','ACTIVE' FROM (SELECT ? AS staff_id,? AS floor_area_id,? AS starts_at) src",
                head.staffId(),
                floor,
                ShiftStore.sql(time.now().minus(Duration.ofDays(30))));
        // Phạm vi ASSIGNED của 002 chỉ tính phân công hiện tại: cho người lập, người nhận một phân công tầng trong ca
        // đang diễn
        // ra để qua kiểm quyền lập của yêu cầu phê duyệt (xem team-plan mục 8, dòng 015).
        Instant now = time.now();
        long current = insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status,lead_staff_id) VALUES (?,?,?,?,'FLOOR',?,FALSE,'IN_PROGRESS',NULL)",
                scheduleId,
                templateId,
                ShiftStore.sql(now.minus(Duration.ofHours(1))),
                ShiftStore.sql(now.plus(Duration.ofHours(1))),
                floor);
        for (TestUsers.TestUser u : java.util.List.of(giver, recipient, third)) {
            jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", current, u.staffId());
            jdbc.update(
                    "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at,status) VALUES (?,?,'FLOOR',?,'NURSING_STAFF','PRIMARY',?,?,'ACTIVE')",
                    current,
                    u.staffId(),
                    floor,
                    ShiftStore.sql(now.minus(Duration.ofHours(1))),
                    ShiftStore.sql(now.plus(Duration.ofHours(1))));
        }
    }

    /** Ca Đã công bố của tầng; {@code hours} giờ kể từ {@code start}; thành viên vào ca từ trước. */
    protected long shift(Instant start, int hours, Long lead, TestUsers.TestUser... members) {
        long id = insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status,lead_staff_id) VALUES (?,?,?,?,'FLOOR',?,FALSE,'PUBLISHED',?)",
                scheduleId,
                templateId,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plus(Duration.ofHours(hours))),
                floor,
                lead);
        for (TestUsers.TestUser m : members) {
            jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", id, m.staffId());
        }
        if (lead != null) {
            jdbc.update(
                    "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,command,by_system) SELECT 'M09.SHIFT',src.shift_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,'SCHEDULED',FALSE FROM (SELECT ? AS shift_id,? AS staff_id,? AS starts_at) src",
                    id,
                    lead,
                    ShiftStore.sql(start));
        }
        return id;
    }

    protected String token(TestUsers.TestUser user) {
        return TestUsers.bearer(users.session(user.accountId()));
    }

    protected ResultActions send(String path, TestUsers.TestUser user, Object body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .header("Authorization", token(user))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body == null ? "{}" : json.writeValueAsString(body)));
    }

    protected ResultActions fetch(String path, TestUsers.TestUser user) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                .header("Authorization", token(user)));
    }

    /** Lập yêu cầu đổi hai chiều và trả mã yêu cầu. */
    protected long createMutual(TestUsers.TestUser by, long x, TestUsers.TestUser to, long y) throws Exception {
        var response = send(
                        "/api/v1/shift-swaps",
                        by,
                        Map.of(
                                "kind",
                                "MUTUAL",
                                "shiftXId",
                                x,
                                "recipientId",
                                to.staffId(),
                                "shiftYId",
                                y,
                                "reason",
                                "[Lý do riêng tư đổi ca]"))
                .andReturn()
                .getResponse();
        String location = response.getHeader("Location");
        if (location == null)
            throw new AssertionError(
                    "Lập yêu cầu thất bại: " + response.getStatus() + " " + response.getContentAsString());
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    protected long createTakeOver(TestUsers.TestUser by, long x, TestUsers.TestUser to) throws Exception {
        var response = send(
                        "/api/v1/shift-swaps",
                        by,
                        Map.of(
                                "kind",
                                "TAKE_OVER",
                                "shiftXId",
                                x,
                                "recipientId",
                                to.staffId(),
                                "reason",
                                "[Lý do riêng tư nhận thay]"))
                .andReturn()
                .getResponse();
        String location = response.getHeader("Location");
        if (location == null)
            throw new AssertionError(
                    "Lập yêu cầu thất bại: " + response.getStatus() + " " + response.getContentAsString());
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    protected boolean inShift(long shiftId, TestUsers.TestUser user) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM shift_staff WHERE shift_id=? AND staff_id=? AND left_at IS NULL AND approved_leave_request_id IS NULL",
                Integer.class,
                shiftId,
                user.staffId());
        return n != null && n > 0;
    }

    protected Map<String, Object> noBody() {
        return Map.of();
    }
}
