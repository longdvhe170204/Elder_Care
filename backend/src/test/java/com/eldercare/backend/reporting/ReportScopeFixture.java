package com.eldercare.backend.reporting;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/** Dựng ca, thành viên, phân công thật của 008 cho test phạm vi báo cáo (SC-004, SC-011). */
final class ReportScopeFixture {

    private final JdbcTemplate jdbc;
    private final TestUsers users;
    private final Map<String, Long> schedules = new HashMap<>();

    ReportScopeFixture(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.users = new TestUsers(jdbc, time);
    }

    TestUsers users() {
        return users;
    }

    TestUser staff(Role... roles) {
        TestUser user = users.staff(roles);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên báo cáo]','Chức danh',?)",
                user.staffId(),
                user.accountId());
        return user;
    }

    /** Ca 8 giờ của tầng; mỗi tầng một lịch tháng (uq_shift_schedule_month). */
    long shift(long floor, Instant start, String status) {
        String month = YearMonth.from(start.atZone(BusinessTime.ZONE)).toString();
        long schedule = schedules.computeIfAbsent(
                floor + "|" + month,
                k -> insert(
                        "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (?,'FLOOR',?,'PUBLISHED')",
                        month,
                        floor));
        long template = insert(
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu báo cáo]','07:00','15:00','FLOOR',FALSE)");
        return insert(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover,status) VALUES (?,?,'FLOOR',?,?,?,FALSE,?)",
                schedule,
                template,
                floor,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plus(Duration.ofHours(8))),
                status);
    }

    void member(long shift, long staff) {
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, staff);
    }

    /** Phân công người cao tuổi trong ca, từ {@code from} tới hết ca, đang hiệu lực. */
    void assignResident(long shift, long staff, long resident, Instant from, Instant shiftEnd) {
        assignment(shift, staff, resident, from, shiftEnd, "ACTIVE", null);
    }

    /** Phân công đã kết thúc sớm tại {@code endedAt} (gỡ khỏi ca, vắng giữa ca). */
    void assignResidentEnded(long shift, long staff, long resident, Instant from, Instant shiftEnd, Instant endedAt) {
        assignment(shift, staff, resident, from, shiftEnd, "ENDED", ShiftStore.sql(endedAt));
    }

    private void assignment(
            long shift, long staff, long resident, Instant from, Instant to, String status, Object actualEnd) {
        insert(
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at,status,actual_end_at,created_by) VALUES (?,?,'RESIDENT',?,'NURSING_STAFF','PRIMARY',?,?,?,?,1)",
                shift,
                staff,
                resident,
                ShiftStore.sql(from),
                ShiftStore.sql(to),
                status,
                actualEnd);
    }

    void lead(long shift, long staff, Instant from) {
        jdbc.update(
                "INSERT INTO shift_lead_history(shift_id,staff_id,starts_at,basis) VALUES (?,?,?,'SCHEDULED')",
                shift,
                staff,
                ShiftStore.sql(from));
    }

    /** Giao Trưởng tầng; {@code to} rỗng là chưa kết thúc. */
    void floorManager(long staff, long floor, Instant from, Instant to) {
        jdbc.update(
                "INSERT INTO floor_manager_assignment(staff_id,floor_area_id,starts_at,ends_at,assigned_by) VALUES (?,?,?,?,1)",
                staff,
                floor,
                ShiftStore.sql(from),
                to == null ? null : ShiftStore.sql(to));
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
}
