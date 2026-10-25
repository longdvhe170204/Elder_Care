package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.m03.api.dto.SessionDate;
import com.cambae.backend.m15.parameter.api.*;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.FakeDayCareArrivals;
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
    void BR_M03_25_editSessionRecountsAndWarnsWithoutDeletingArrivals() throws Exception {
        arrivals.count(new SessionDate(time.today().plusDays(1), morning), 4);
        mvc.perform(put("/api/v1/day-care-sessions/" + morning)
                        .header("If-Match", "\"0\"")
                        .contentType("application/json")
                        .content("{\"name\":\"Sáng\",\"startTime\":\"08:00\",\"endTime\":\"12:00\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.overCapacitySessions[0].remaining").value(-1));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='M03_DAY_CARE_OVER_CAPACITY'", Integer.class))
                .isPositive();
        mvc.perform(get("/api/v1/day-care-rest-areas/registration-counts")
                        .param("from", time.today().toString())
                        .param("to", time.today().plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.registrations == 4)]").isNotEmpty());
    }

    @Test
    void FR_038_futureArrivalsPreventAreaDeactivationAndStaleVersionFails() throws Exception {
        arrivals.count(new SessionDate(time.today().plusDays(1), morning), 1);
        mvc.perform(post("/api/v1/day-care-rest-areas/" + rest + "/deactivate")
                        .header("If-Match", "\"0\"")
                        .contentType("application/json")
                        .content("{\"reason\":\"Sửa khu nghỉ\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/day-care-sessions/" + morning)
                        .header("If-Match", "\"8\"")
                        .contentType("application/json")
                        .content("{\"name\":\"Sáng\",\"startTime\":\"08:00\",\"endTime\":\"12:00\"}"))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void FR_038_floorManagerReadsButCannotConfigure() throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER).user());
        mvc.perform(get("/api/v1/day-care-sessions")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/day-care-sessions")
                        .contentType("application/json")
                        .content("{\"name\":\"Chiều\",\"startTime\":\"12:00\",\"endTime\":\"17:00\"}"))
                .andExpect(status().isForbidden());
    }
}
