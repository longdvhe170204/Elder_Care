package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.roombedallocation.api.DayCareCapacityApi;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** T049/T051: sức chứa qua API thật, nguồn lịch và khoanh vùng bằng fake đúng chủ. */
@Transactional
class BR_M03_03_DayCareCapacityIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    FakeAllocationInfectionStatus infection;

    @MockitoSpyBean
    ParameterApi parameters;

    long morning, afternoon;

    @BeforeEach
    void dayCareFixture() {
        infection.reset();
        doReturn(new ParameterValue(
                        ParameterCode.CFG_M03_01,
                        ParameterKind.INTEGER,
                        JsonMapper.builder().build().readTree("3")))
                .when(parameters)
                .read(ParameterCode.CFG_M03_01);
        jdbc.update("INSERT INTO day_care_rest_area(code,name,floor_id) VALUES('D1','Khu nghỉ',?)", floor);
        morning = TestUsers.nextId();
        afternoon = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO day_care_session(id,name,start_time,end_time) VALUES(?,'Sáng','07:00','12:00'),(?,'Chiều','12:00','17:00')",
                morning,
                afternoon);
    }

    private DayCareCapacityApi api() {
        var beans = context.getBeansOfType(DayCareCapacityApi.class);
        assertThat(beans).as("T051 cần bản thật DayCareCapacityApi").hasSize(1);
        return beans.values().iterator().next();
    }

    @Test
    void BR_M03_03_limitUsesAfterCountButAllowsReductionWhileAlreadyOverCapacity() {
        var api = api();
        assertThat(api.lock().capacity()).isEqualTo(3);
        var key = new SessionDate(time.today().plusDays(1), morning);
        assertThat(api.checkCapacity(Map.of(key, new RegistrationCount(2, 3)))).isEmpty();
        assertThat(api.checkCapacity(Map.of(key, new RegistrationCount(3, 4))))
                .singleElement()
                .satisfies(value -> {
                    assertThat(value.code()).isEqualTo("DAY_CARE_OVER_CAPACITY");
                    assertThat(value.remaining()).isEqualTo(-1);
                });
        assertThat(api.checkCapacity(Map.of(key, new RegistrationCount(4, 4)))).isEmpty();
        assertThat(api.checkCapacity(Map.of(key, new RegistrationCount(4, 3)))).isEmpty();
    }

    @Test
    void BR_M03_25_parameterChangeRecountsAndWarnsOnlyOverCapacitySessions(@Autowired FakeDayCareArrivals arrivals) {
        arrivals.reset();
        var day = time.today().plusDays(2);
        arrivals.count(new SessionDate(day, morning), 3).count(new SessionDate(day, afternoon), 2);
        doReturn(new ParameterValue(
                        ParameterCode.CFG_M03_01,
                        ParameterKind.INTEGER,
                        JsonMapper.builder().build().readTree("2")))
                .when(parameters)
                .read(ParameterCode.CFG_M03_01);
        // DayCareParameterListener (@ApplicationModuleListener, chạy sau commit) chỉ chuyển CFG-M03-01 sang recount().
        context.getBean(com.eldercare.backend.roombedallocation.internal.daycare.service.DayCareRecounting.class)
                .recount();
        var rest = jdbc.queryForObject("SELECT id FROM day_care_rest_area WHERE code='D1'", Long.class);
        assertThat(jdbc.queryForObject(
                        "SELECT details FROM audit_log WHERE action='M03_DAY_CARE_OVER_CAPACITY' AND entity_id=?",
                        String.class,
                        String.valueOf(rest)))
                .contains("\"sessionId\": " + morning)
                .doesNotContain("\"sessionId\": " + afternoon);
        arrivals.reset();
    }
}
