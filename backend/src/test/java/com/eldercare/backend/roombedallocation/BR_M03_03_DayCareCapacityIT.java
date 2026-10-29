package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.roombedallocation.api.DayCareCapacityApi;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.roombedallocation.api.spi.CordonedArea;
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
    void BR_M03_03_weeklySeriesReportsOnlyTheDateThatExceedsCapacity() {
        var api = api();
        api.lock();
        var first = new SessionDate(time.today().plusDays(1), morning);
        var second = new SessionDate(first.date().plusWeeks(1), morning);
        var third = new SessionDate(first.date().plusWeeks(2), morning);
        assertThat(api.checkCapacity(Map.of(
                        first, new RegistrationCount(2, 3),
                        second, new RegistrationCount(3, 4),
                        third, new RegistrationCount(1, 2))))
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.session()).isEqualTo(second);
                    assertThat(violation.capacity()).isEqualTo(3);
                    assertThat(violation.remaining()).isEqualTo(-1);
                    assertThat(violation.code()).isEqualTo("DAY_CARE_OVER_CAPACITY");
                });
    }

    @Test
    void FR_039_arrivalUsesEveryIntersectingSessionAndExcludesTouchingEndpoint() {
        var api = api();
        var day = time.today().plusDays(1);
        assertThat(api.occupiedSessions(day, LocalTime.of(7, 0), LocalTime.of(12, 0)))
                .containsExactly(new SessionDate(day, morning));
        assertThat(api.occupiedSessions(day, LocalTime.of(7, 0), LocalTime.of(17, 0)))
                .containsExactly(new SessionDate(day, morning), new SessionDate(day, afternoon));
        assertThatThrownBy(() -> api.occupiedSessions(day, LocalTime.NOON, LocalTime.NOON))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
    }

    @Test
    void FR_042_cordonOfParentAreaBlocksIncreaseButAllowsReduction() {
        infection.cordon(time.now().minusSeconds(1), new CordonedArea(Set.of(), Set.of(area)));
        var api = api();
        var key = new SessionDate(time.today().plusDays(1), morning);
        api.lock();
        assertThat(api.checkCapacity(Map.of(key, new RegistrationCount(0, 1))))
                .singleElement()
                .satisfies(value -> assertThat(value.code()).isEqualTo("DAY_CARE_CORDONED"));
        assertThat(api.checkCapacity(Map.of(key, new RegistrationCount(2, 1)))).isEmpty();
    }

    @Test
    void FR_038_unsetCapacityIsNotReplacedWithHardcodedNumber() {
        doReturn(new ParameterValue(
                        ParameterCode.CFG_M03_01,
                        ParameterKind.INTEGER,
                        JsonMapper.builder().build().readTree("null")))
                .when(parameters)
                .read(ParameterCode.CFG_M03_01);
        assertThatThrownBy(() -> api().lock()).isInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
    }
}
