package com.cambae.backend.m14;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * FR-020, US1 #4 (016 T024): khối "Tỷ lệ phục vụ ca hiện tại" của dashboard và danh sách chi tiết không khoảng chỉ gồm ca
 * đang diễn ra, theo giá trị 008 đã lưu, trong tầng được giao của Trưởng tầng; ca đã kết thúc trong ngày không vào. Bàn
 * giao Đã lập của ca đã kết thúc từ lâu vẫn nằm trong "Bàn giao chưa xác nhận".
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class FR_020_StaffingDashboardIT extends MySqlIntegrationTestBase {

    private static final String RATIO = "$.blocks[?(@.group == 'STAFFING_RATIO')]";
    private static final String HANDOVERS = "$.blocks[?(@.group == 'HANDOVERS')]";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeAssignments assignments;

    @AfterEach
    void reset() {
        clock.reset();
    }

    private long insert(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Cột DATETIME(6) lưu giờ UTC. */
    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    private Long schedule;

    private long shift(long floor, Instant start, String status) {
        long template = insert(
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu dashboard]','07:00','15:00','FLOOR',TRUE)");
        // Một lịch tháng cho tầng của test (uq_shift_schedule_month); thống kê đọc theo giờ ca, không theo tháng lịch.
        if (schedule == null) {
            schedule = insert(
                    "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (?,'FLOOR',?,'PUBLISHED')",
                    time.today().toString().substring(0, 7),
                    floor);
        }
        return insert(
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES (?,?,?,?,'FLOOR',?,TRUE,?)",
                schedule,
                template,
                utc(start),
                utc(start.plus(Duration.ofHours(8))),
                floor,
                status);
    }

    private void calculation(long shift, Instant at, boolean passed) {
        jdbc.update(
                "INSERT INTO staffing_ratio_calculation(shift_id,calculated_at,event,total_weight,staff_count,ratio,threshold,meets_threshold) VALUES (?,?,'CARE_LEVEL_CHANGED',35.00,4,8.75,8.00,?)",
                shift,
                utc(at),
                passed);
    }

    @Test
    void US1_4_floorManager_currentShiftRatioOnly_andOldPreparedHandover() throws Exception {
        clock.setTo(time.startOfDay(time.today()).plus(Duration.ofHours(10)));
        Instant now = time.now();
        long floor = TestUsers.nextId();
        TestUsers users = new TestUsers(jdbc, time);
        TestUser floorManager = users.staff(Role.FLOOR_MANAGER);
        assignments.assign(
                floorManager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER, floor, null, null, null, now.minus(Duration.ofDays(60)), null));
        long current = shift(floor, now.minus(Duration.ofHours(3)), "IN_PROGRESS");
        calculation(current, now.minus(Duration.ofHours(1)), false);
        long ended = shift(floor, now.minus(Duration.ofHours(11)), "CLOSED");
        calculation(ended, now.minus(Duration.ofHours(10)), true);
        long old = shift(floor, now.minus(Duration.ofDays(30)), "CLOSED");
        insert(
                "INSERT INTO shift_handover(shift_id,handover_staff_id,status,created_at,finalized_at) VALUES (?,?,'PREPARED',?,?)",
                old,
                insert("INSERT INTO staff_profile(full_name,position) VALUES ('[Nhân viên bàn giao]','Chăm sóc')"),
                utc(now.minus(Duration.ofDays(30))),
                utc(now.minus(Duration.ofDays(30))));
        String token = TestUsers.bearer(users.session(floorManager.accountId()));

        mvc.perform(get("/api/v1/dashboard").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath(RATIO + ".values[0].metricCode").value(hasItem("SHIFT.STAFFING_RATIO")))
                .andExpect(jsonPath(RATIO + ".values[0].value").value(hasItem(1)))
                .andExpect(jsonPath(RATIO + ".values[0].breakdown[0].key").value(hasItem("NOT_MET")))
                .andExpect(jsonPath(HANDOVERS + ".values[0].value").value(hasItem(1)));
        mvc.perform(get("/api/v1/metric-details/SHIFT.STAFFING_RATIO").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].sourceKey").value("M09.WORK_SHIFT:" + current));
    }
}
