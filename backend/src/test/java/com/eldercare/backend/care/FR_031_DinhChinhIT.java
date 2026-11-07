package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.billing.api.events.CostSourceBilled;
import com.eldercare.backend.billing.api.events.CostSourceChanged;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.time.Duration;
import java.util.Map;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T013 (FR-031, FR-032; US3 kịch bản 4): điểm danh nhóm 3 không sửa, chỉ đính chính theo 000; đổi lượt Có mặt ở hoạt
 * động có thu phí thành Vắng hoặc Hủy ghi nhận thì 010 nhận hủy nguồn, đính chính thêm lượt Có mặt thì có nguồn mới.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
class FR_031_DinhChinhIT extends MySqlIntegrationTestBase {
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
    CorrectionApi corrections;

    @Autowired
    ApplicationEvents events;

    SessionAttendanceFixture data;

    @BeforeEach
    void fixture() {
        data = new SessionAttendanceFixture(jdbc, time, assignments, user, residents);
    }

    long record(long session, long resident, String result) throws Exception {
        String extra =
                result.equals("PRESENT") ? ",\"participationLevel\":\"NORMAL\"" : ",\"absenceReason\":\"HEALTH\"";
        mvc.perform(post("/api/v1/activity-sessions/" + session + "/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "attendance-" + TestUsers.nextId())
                        .content("{\"results\":[{\"residentId\":" + resident + ",\"result\":\"" + result + "\"" + extra
                                + "}]}"))
                .andExpect(status().isOk());
        return jdbc.queryForObject(
                "SELECT id FROM record_event WHERE kind='M04.SESSION_ATTENDANCE' AND JSON_EXTRACT(content,'$.sessionId')=?"
                        + " AND JSON_EXTRACT(content,'$.residentId')=?",
                Long.class,
                session,
                resident);
    }

    @Test
    void FR_031_presentCorrectedToAbsentCancelsTheBillingSource() throws Exception {
        long session = data.session(data.activity(true), Duration.ofMinutes(-30));
        long resident = data.residents(session, 1).getFirst();
        long record = record(session, resident, "PRESENT");
        var billed = events.stream(CostSourceBilled.class).toList().getFirst();

        corrections.create(
                "M04.SESSION_ATTENDANCE",
                record,
                CorrectionType.AMEND_CONTENT,
                Map.of("result", "ABSENT", "absenceReason", "HEALTH"),
                new Reason("Ghi nhầm người"));

        assertThat(events.stream(CostSourceChanged.class)).singleElement().satisfies(e -> {
            assertThat(e.sourceId()).isEqualTo(billed.sourceId());
            assertThat(e.cancelled()).isTrue();
        });
        // Bản gốc nhóm 3 không đổi.
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.result')) FROM record_event WHERE id=?",
                        String.class,
                        record))
                .isEqualTo("PRESENT");
    }

    @Test
    void FR_031_absentCorrectedToPresentCreatesANewBillingSource() throws Exception {
        long session = data.session(data.activity(true), Duration.ofMinutes(-30));
        long resident = data.residents(session, 1).getFirst();
        long record = record(session, resident, "ABSENT");
        assertThat(events.stream(CostSourceBilled.class)).isEmpty();

        corrections.create(
                "M04.SESSION_ATTENDANCE",
                record,
                CorrectionType.AMEND_CONTENT,
                Map.of("result", "PRESENT", "participationLevel", "NORMAL"),
                new Reason("Người cao tuổi có tham gia"));

        assertThat(events.stream(CostSourceBilled.class))
                .singleElement()
                .satisfies(e -> assertThat(e.residentId()).isEqualTo(resident));
    }

    @Test
    void FR_031_voidingAPresentAttendanceCancelsTheBillingSource() throws Exception {
        long session = data.session(data.activity(true), Duration.ofMinutes(-30));
        long resident = data.residents(session, 1).getFirst();
        long record = record(session, resident, "PRESENT");

        corrections.create("M04.SESSION_ATTENDANCE", record, CorrectionType.VOID, null, new Reason("Ghi nhầm buổi"));

        assertThat(events.stream(CostSourceChanged.class))
                .singleElement()
                .satisfies(e -> assertThat(e.cancelled()).isTrue());
    }
}
