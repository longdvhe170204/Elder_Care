package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.care.api.events.ActivityCostSourceBilled;
import com.eldercare.backend.care.api.events.ActivitySessionEnded;
import com.eldercare.backend.care.api.spi.AttendanceTracePort;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T013 (BR-M04-18; FR-027 → FR-030, FR-032; US3 kịch bản 1 → 3): điểm danh Có mặt, Vắng, thêm người lúc điểm danh;
 * mỗi lượt Có mặt ở hoạt động có thu phí là một sự kiện tính phí cho 010; Hoàn tất điểm danh khi mọi đăng ký có kết quả.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import({SwitchableUserConfig.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
class BR_58_SuKienTinhPhiIT extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeResidents residents;

    @Autowired
    ApplicationEvents events;

    @Autowired
    AttendanceTracePort trace;

    SessionAttendanceFixture data;

    @BeforeEach
    void fixture() {
        data = new SessionAttendanceFixture(jdbc, time, assignments, user, residents);
    }

    ResultActions attendance(long session, String results) throws Exception {
        return mvc.perform(post("/api/v1/activity-sessions/" + session + "/attendance")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "attendance-" + TestUsers.nextId())
                .content("{\"results\":[" + results + "]}"));
    }

    ResultActions command(long session, String command) throws Exception {
        return mvc.perform(post("/api/v1/activity-sessions/" + session + "/" + command)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "command-" + TestUsers.nextId())
                .content("{}"));
    }

    static String present(long resident) {
        return "{\"residentId\":" + resident + ",\"result\":\"PRESENT\",\"participationLevel\":\"ACTIVE\"}";
    }

    @Test
    void BR_58_eachPresentAttendanceOfAChargedActivityIsOneBillingSourceEvent() throws Exception {
        long session = data.session(data.activity(true), Duration.ofMinutes(-30));
        var registered = data.residents(session, 4);
        long walkIn = data.resident();
        attendance(
                        session,
                        String.join(
                                ",",
                                present(registered.get(0)),
                                present(registered.get(1)),
                                present(registered.get(2)),
                                "{\"residentId\":" + registered.get(3)
                                        + ",\"result\":\"ABSENT\",\"absenceReason\":\"REFUSED\"}",
                                "{\"residentId\":" + walkIn
                                        + ",\"addedAtAttendance\":true,\"result\":\"PRESENT\",\"participationLevel\":\"LEFT_EARLY\"}"))
                .andExpect(status().isOk());

        var billed = events.stream(ActivityCostSourceBilled.class).toList();
        assertThat(billed).hasSize(4);
        assertThat(billed).allSatisfy(e -> {
            assertThat(e.sourceType()).isEqualTo("ACTIVITY_ATTENDANCE");
            assertThat(e.serviceId()).isEqualTo(Optional.of(data.serviceId));
            assertThat(e.quantity()).isEqualByComparingTo(BigDecimal.ONE);
        });
        assertThat(billed)
                .extracting(ActivityCostSourceBilled::residentId)
                .doesNotContain(registered.get(3))
                .contains(walkIn);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM activity_registration WHERE session_id=? AND status='ATTENDED'",
                        Integer.class,
                        session))
                .isEqualTo(5);
        assertThat(jdbc.queryForObject(
                        "SELECT source FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        session,
                        walkIn))
                .isEqualTo("ADDED_AT_ATTENDANCE");

        command(session, "complete-attendance").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM activity_session WHERE id=?", String.class, session))
                .isEqualTo("ATTENDANCE_TAKEN");
        assertThat(events.stream(ActivitySessionEnded.class).map(ActivitySessionEnded::sessionId))
                .contains(session);

        // T014 (007 FR-060 (b)): nguồn truy vết trả buổi chồng khoảng và người cùng Có mặt hiện hành; không có người
        // Vắng, không có chính người được truy vết; Hủy ghi nhận qua đính chính thì không còn là người cùng Có mặt.
        var from = time.now().minus(Duration.ofHours(2));
        var to = time.now().plus(Duration.ofHours(2));
        var traced = trace.coAttendance(registered.get(0), from, to);
        assertThat(traced).singleElement().satisfies(v -> {
            assertThat(v.sessionId()).isEqualTo(session);
            assertThat(v.to()).isAfter(v.from());
            assertThat(v.residentIds()).containsExactlyInAnyOrder(registered.get(1), registered.get(2), walkIn);
        });
        assertThat(trace.coAttendance(registered.get(3), from, to)).isEmpty();
        assertThat(trace.coAttendance(registered.get(0), from, time.now().minus(Duration.ofMinutes(31))))
                .isEmpty();
        long walkInAttendance = jdbc.queryForObject(
                "SELECT id FROM record_event WHERE kind='M04.SESSION_ATTENDANCE' AND unique_key=?",
                Long.class,
                "M04.SESSION_ATTENDANCE:"
                        + jdbc.queryForObject(
                                "SELECT id FROM activity_registration WHERE session_id=? AND resident_id=?",
                                Long.class,
                                session,
                                walkIn));
        mvc.perform(post("/api/v1/corrections/M04.SESSION_ATTENDANCE/" + walkInAttendance)
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"VOID\",\"reason\":\"Ghi nhầm người\"}"))
                .andExpect(status().is2xxSuccessful());
        assertThat(trace.coAttendance(registered.get(0), from, to))
                .singleElement()
                .satisfies(v ->
                        assertThat(v.residentIds()).containsExactlyInAnyOrder(registered.get(1), registered.get(2)));
        assertThat(trace.coAttendance(walkIn, from, to)).isEmpty();
    }
}
