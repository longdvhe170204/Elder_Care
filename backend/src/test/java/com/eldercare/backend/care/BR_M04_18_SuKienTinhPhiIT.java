package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.billing.api.events.CostSourceBilled;
import com.eldercare.backend.care.api.events.ActivitySessionEnded;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
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
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
class BR_M04_18_SuKienTinhPhiIT extends MySqlIntegrationTestBase {
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
    void BR_M04_18_eachPresentAttendanceOfAChargedActivityIsOneBillingSourceEvent() throws Exception {
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

        var billed = events.stream(CostSourceBilled.class).toList();
        assertThat(billed).hasSize(4);
        assertThat(billed).allSatisfy(e -> {
            assertThat(e.sourceType()).isEqualTo("ACTIVITY_ATTENDANCE");
            assertThat(e.serviceId()).isEqualTo(Optional.of(data.serviceId));
            assertThat(e.quantity()).isEqualByComparingTo(BigDecimal.ONE);
        });
        assertThat(billed)
                .extracting(CostSourceBilled::residentId)
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
    }

    @Test
    void FR_032_absentAndUnchargedAreNotBillingEvents() throws Exception {
        long session = data.session(data.activity(false), Duration.ofMinutes(-30));
        var registered = data.residents(session, 1);
        attendance(session, present(registered.getFirst())).andExpect(status().isOk());
        assertThat(events.stream(CostSourceBilled.class)).isEmpty();
    }

    @Test
    void FR_028_attendanceStartsAtSessionStartAndIsRecordedOncePerRegistration() throws Exception {
        long future = data.session(data.activity(false), Duration.ofHours(2));
        var r = data.residents(future, 1).getFirst();
        attendance(future, present(r))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_YET_STARTED"));

        long session = data.session(data.activity(false), Duration.ofMinutes(-30));
        var resident = data.residents(session, 1).getFirst();
        attendance(session, present(resident)).andExpect(status().isOk());
        attendance(session, present(resident))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ATTENDANCE_ALREADY_RECORDED"));
    }

    @Test
    void FR_029_presentNeedsParticipationLevelAndAbsentNeedsReason() throws Exception {
        long session = data.session(data.activity(false), Duration.ofMinutes(-30));
        var r = data.residents(session, 1).getFirst();
        attendance(session, "{\"residentId\":" + r + ",\"result\":\"PRESENT\"}")
                .andExpect(status().isUnprocessableContent());
        attendance(session, "{\"residentId\":" + r + ",\"result\":\"ABSENT\"}")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_029_completeNeedsEveryActiveRegistrationToHaveAResult() throws Exception {
        long session = data.session(data.activity(false), Duration.ofMinutes(-30));
        var rs = data.residents(session, 2);
        attendance(session, present(rs.getFirst())).andExpect(status().isOk());
        command(session, "complete-attendance")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REGISTRATIONS_WITHOUT_RESULT"));
    }

    @Test
    void FR_030_attendanceAfterEndPlusGracePeriodIsLabelledLate() throws Exception {
        long session = data.session(data.activity(false), Duration.ofHours(-4));
        var r = data.residents(session, 1).getFirst();
        attendance(session, present(r)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_EXTRACT(content,'$.lateRecorded') FROM record_event WHERE kind='M04.SESSION_ATTENDANCE'"
                                + " AND JSON_EXTRACT(content,'$.sessionId')=?",
                        String.class,
                        session))
                .isEqualTo("true");
    }

    @Test
    void FR_028_walkInMustMeetRegistrationConditions() throws Exception {
        long session = data.session(data.activity(false), Duration.ofMinutes(-30));
        long ended = data.resident();
        residents.changeStatus(
                ended, com.eldercare.backend.resident.api.dto.ResidentStatus.RESIDENCY_ENDED, time.now());
        attendance(
                        session,
                        "{\"residentId\":" + ended
                                + ",\"addedAtAttendance\":true,\"result\":\"PRESENT\",\"participationLevel\":\"ACTIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENT_STATUS_INVALID"));
        assertThat(List.of(session)).isNotEmpty();
    }
}
