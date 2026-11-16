package com.eldercare.backend.incident;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * FR-060 (b), 014 T014: nhóm "Hoạt động" của tra cứu tiếp xúc đọc nguồn điểm danh thật của 014
 * ({@code ActivityAttendanceTraceSource}, không còn bản giả ở {@link ContactTracingTestBase}): người cùng Có mặt ở buổi
 * chồng khoảng truy vết được trả, người Vắng thì không.
 */
class FR_060b_ActivityTraceIT extends ContactTracingTestBase {

    @Test
    void FR_060b_activityGroupListsResidentsPresentAtTheSameSession() throws Exception {
        long together = TestUsers.nextId();
        long absent = TestUsers.nextId();
        // giữa khoảng truy vết (CFG-M05-07 tính ngược từ lúc phát hiện)
        Instant start = traceFrom.plus(Duration.between(traceFrom, discoveredAt).dividedBy(2));
        long session = session(start, start.plus(Duration.ofHours(1)));
        attend(session, resident, "PRESENT", start);
        attend(session, together, "PRESENT", start);
        attend(session, absent, "ABSENT", start);

        mvc.perform(as(nurse, get(LOOKUP, incidentId).param("group", "ACTIVITY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group", hasSize(1)))
                .andExpect(jsonPath("$.group[0].group").value("ACTIVITY"))
                .andExpect(jsonPath("$.group[0].noData").value(false))
                .andExpect(jsonPath("$.group[0].people[*].subjectId", contains((int) together)));
    }

    private long session(Instant from, Instant to) {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement(
                            "INSERT INTO activity_session (activity_id, starts_at, ends_at, source, status, created_by,"
                                    + " created_at) VALUES (?, ?, ?, 'SINGLE', 'ATTENDANCE_TAKEN', ?, ?)",
                            new String[] {"id"});
                    ps.setLong(1, TestUsers.nextId());
                    ps.setTimestamp(2, local(from));
                    ps.setTimestamp(3, local(to));
                    ps.setLong(4, nurse.accountId());
                    ps.setTimestamp(5, local(from));
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    /** Đăng ký và bản gốc điểm danh {@code M04.SESSION_ATTENDANCE} như 014 ghi (khóa theo lượt đăng ký). */
    private void attend(long session, long residentId, String result, Instant at) {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement(
                            "INSERT INTO activity_registration (session_id, resident_id, source, registered_at, status)"
                                    + " VALUES (?, ?, 'MANUAL', ?, 'ATTENDED')",
                            new String[] {"id"});
                    ps.setLong(1, session);
                    ps.setLong(2, residentId);
                    ps.setTimestamp(3, local(at));
                    return ps;
                },
                key);
        long registration = key.getKey().longValue();
        jdbc.update(
                "INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system,"
                        + " occurred_at, unique_key) VALUES ('M01.RESIDENT', ?, 'M04', 'M04.SESSION_ATTENDANCE',"
                        + " JSON_OBJECT('sessionId', ?, 'residentId', ?, 'result', ?), ?, FALSE, ?, ?)",
                residentId,
                session,
                residentId,
                result,
                nurse.accountId(),
                local(at),
                "M04.SESSION_ATTENDANCE:" + registration);
    }

    private static Timestamp local(Instant at) {
        return Timestamp.valueOf(LocalDateTime.ofInstant(at, BusinessTime.ZONE));
    }
}
