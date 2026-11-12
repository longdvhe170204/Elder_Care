package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeDayCareArrivals;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** T049/T052: cấu hình, đếm lại và chặn ngừng hiệu lực qua HTTP thật. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class FR_038_DayCareHttpIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

    @Autowired
    FakeDayCareArrivals arrivals;

    @MockitoSpyBean
    ParameterApi parameters;

    long rest, morning;

    @BeforeEach
    void fixture() {
        arrivals.reset();
        doReturn(new ParameterValue(
                        ParameterCode.CFG_M03_01,
                        ParameterKind.INTEGER,
                        JsonMapper.builder().build().readTree("3")))
                .when(parameters)
                .read(ParameterCode.CFG_M03_01);
        rest = TestUsers.nextId();
        morning = TestUsers.nextId();
        // Schema test giữ dữ liệu của lớp khác (test chạy ngoài transaction có thể để lại khu nghỉ Hiệu lực); chỉ được
        // một
        // khu Hiệu lực, nên ngừng khu sót trong transaction của test (rollback sau ca).
        jdbc.update("UPDATE day_care_rest_area SET status='INACTIVE' WHERE status='ACTIVE'");
        jdbc.update("INSERT INTO day_care_rest_area(id,code,name,floor_id) VALUES(?,'D1','Khu nghỉ',?)", rest, floor);
        jdbc.update(
                "INSERT INTO day_care_session(id,name,start_time,end_time) VALUES(?,'Sáng','07:00','12:00')", morning);
    }

    @Test
    void FR_038_secondActiveAreaAndOverlappingSessionAreRejected() throws Exception {
        mvc.perform(post("/api/v1/day-care-rest-areas")
                        .contentType("application/json")
                        .content("{\"code\":\"D2\",\"name\":\"Khu khác\",\"areaId\":" + area + "}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/day-care-sessions")
                        .contentType("application/json")
                        .content("{\"name\":\"Giao giờ\",\"startTime\":\"11:00\",\"endTime\":\"13:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DAY_CARE_SESSION_OVERLAP"));
        mvc.perform(post("/api/v1/day-care-sessions")
                        .contentType("application/json")
                        .content("{\"name\":\"Chiều\",\"startTime\":\"12:00\",\"endTime\":\"17:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.session.name").value("Chiều"));
    }

    @Test
    void FR_040_sessionChangeRecountsAndAreaWithFutureArrivalsCannotBeDeactivated() throws Exception {
        var day = time.today().plusDays(7);
        arrivals.count(new com.eldercare.backend.roombedallocation.api.dto.SessionDate(day, morning), 4);
        mvc.perform(get("/api/v1/day-care-rest-areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + rest + ")].code").value("D1"));
        mvc.perform(get("/api/v1/day-care-sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + morning + ")].name").value("Sáng"));
        mvc.perform(get("/api/v1/day-care-rest-areas/registration-counts")
                        .param("from", day.toString())
                        .param("to", day.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sessionId==" + morning + ")].registrations")
                        .value(4))
                .andExpect(
                        jsonPath("$[?(@.sessionId==" + morning + ")].remaining").value(-1));
        mvc.perform(put("/api/v1/day-care-sessions/" + morning)
                        .header("If-Match", com.eldercare.backend.common.web.VersionEtag.of(0))
                        .contentType("application/json")
                        .content("{\"name\":\"Sáng\",\"startTime\":\"07:30\",\"endTime\":\"11:30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.startTime").value("07:30"))
                .andExpect(jsonPath("$.overCapacitySessions[0].sessionId").value(morning))
                .andExpect(jsonPath("$.overCapacitySessions[0].remaining").value(-1));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='M03_DAY_CARE_OVER_CAPACITY' AND entity_id=?",
                        Integer.class,
                        rest))
                .isEqualTo(1);
        mvc.perform(post("/api/v1/day-care-rest-areas/" + rest + "/deactivate")
                        .header("If-Match", com.eldercare.backend.common.web.VersionEtag.of(0))
                        .contentType("application/json")
                        .content("{\"reason\":\"Đóng khu\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DAY_CARE_FUTURE_ARRIVALS"));
    }
}
