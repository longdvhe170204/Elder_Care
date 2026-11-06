package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.events.ActivitySessionDayEnded;
import com.eldercare.backend.care.api.events.ActivitySessionEnded;
import com.eldercare.backend.care.internal.activity.service.ActivityScheduling;
import com.eldercare.backend.care.internal.scheduling.service.CareDailyWork;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T007 (FR-014a, Q-169; US3 kịch bản 9): hết ngày, buổi còn Đã lên lịch sang Đã điểm danh (dấu "điểm danh không
 * đủ") hoặc Không điểm danh; đăng ký chưa có kết quả nhận "Không ghi nhận". Ngày lấy riêng mỗi lần chạy để không đụng dữ
 * liệu sót của test khác.
 */
@Transactional
@RecordApplicationEvents
class FR_014a_HetNgayIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    List<CareDailyWork> work;

    @Autowired
    ApplicationEvents events;

    @Autowired
    com.eldercare.backend.common.time.BusinessTime time;

    CareDailyWork dayEnd() {
        return work.stream()
                .filter(w -> w.name().equals(ActivityScheduling.PREFIX + "SESSION_DAY_END"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu bước hết ngày của buổi"));
    }

    long session(LocalDate day, long floor) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,floor_area_id,responsible_staff_id,"
                        + "status,created_at,created_by) VALUES(?,?,?,?,'TEMPLATE',?,?,'SCHEDULED',?,1)",
                id,
                TestUsers.nextId(),
                day.atTime(9, 0),
                day.atTime(10, 0),
                floor,
                TestUsers.nextId(),
                day.atTime(0, 0));
        return id;
    }

    long registration(long session, String status) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO activity_registration(id,session_id,resident_id,source,registered_at,status)"
                        + " VALUES(?,?,?,'MANUAL',?,?)",
                id,
                session,
                TestUsers.nextId(),
                LocalDateTime.of(2000, 1, 1, 0, 0),
                status);
        return id;
    }

    String status(String table, long id) {
        return jdbc.queryForObject("SELECT status FROM " + table + " WHERE id=?", String.class, id);
    }

    @Test
    void FR_014a_sessionsStillScheduledAtDayEndAreClosedAndOpenRegistrationsGetNotRecorded() {
        var day = LocalDate.of(2001, 1, 1).plusDays(TestUsers.nextId() % 5000);
        long floor = TestUsers.nextId();
        long partial = session(day, floor);
        long attended = registration(partial, "ATTENDED");
        long open = registration(partial, "REGISTERED");
        long cancelled = registration(partial, "CANCELLED");
        long none = session(day, floor);
        long noneOpen = registration(none, "REGISTERED");
        long later = session(day.plusDays(1), floor);

        int changed = dayEnd().run(
                        day,
                        day.plusDays(1)
                                .atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh"))
                                .toInstant());

        assertThat(changed).isEqualTo(2);
        assertThat(status("activity_session", partial)).isEqualTo("ATTENDANCE_TAKEN");
        assertThat(jdbc.queryForObject(
                        "SELECT incomplete_attendance FROM activity_session WHERE id=?", Boolean.class, partial))
                .isTrue();
        assertThat(status("activity_session", none)).isEqualTo("NO_ATTENDANCE");
        assertThat(status("activity_session", later)).isEqualTo("SCHEDULED");
        assertThat(status("activity_registration", attended)).isEqualTo("ATTENDED");
        assertThat(status("activity_registration", open)).isEqualTo("ATTENDED");
        assertThat(status("activity_registration", noneOpen)).isEqualTo("ATTENDED");
        assertThat(status("activity_registration", cancelled)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.SESSION_ATTENDANCE' AND unique_key IN (?,?)"
                                + " AND JSON_UNQUOTE(JSON_EXTRACT(content,'$.result'))='NOT_RECORDED'",
                        Integer.class,
                        "M04.SESSION_ATTENDANCE:" + open,
                        "M04.SESSION_ATTENDANCE:" + noneOpen))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.SESSION_ATTENDANCE' AND unique_key=?",
                        Integer.class,
                        "M04.SESSION_ATTENDANCE:" + attended))
                .isZero();
        assertThat(events.stream(ActivitySessionEnded.class).map(ActivitySessionEnded::sessionId))
                .contains(partial, none);
        assertThat(events.stream(ActivitySessionDayEnded.class).map(ActivitySessionDayEnded::sessionId))
                .contains(partial, none);
        // Chạy lại cùng ngày không đổi gì thêm.
        assertThat(dayEnd().run(day, Instant.now())).isZero();
    }

    @Test
    void FR_014a_dueDatesStartFromOldestScheduledSessionBeforeToday() {
        // Chỉ kiểm hình dạng: ngày đến hạn không vượt hôm qua và có hôm qua.
        var today = time.today();
        assertThat(dayEnd().dueDates(today))
                .isNotEmpty()
                .allSatisfy(d -> assertThat(d).isBefore(today));
        assertThat(dayEnd().dueDates(today)).contains(today.minusDays(1));
    }
}
