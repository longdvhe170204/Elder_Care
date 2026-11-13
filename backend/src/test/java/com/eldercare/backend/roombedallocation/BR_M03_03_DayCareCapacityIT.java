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
        // Schema test giữ dữ liệu của lớp khác (test chạy ngoài transaction có thể để lại khu nghỉ Hiệu lực); chỉ được
        // một
        // khu Hiệu lực, nên ngừng khu sót trong transaction của test (rollback sau ca).
        jdbc.update(
                "UPDATE catalog_item SET status='INACTIVE' WHERE catalog_type='M03.DAY_CARE_REST_AREA' AND status='ACTIVE'");
        jdbc.update(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES('M03.DAY_CARE_REST_AREA',"
                        + "'M03','Khu nghỉ','ACTIVE',JSON_OBJECT('code','D1','floorId',?,'areaId',NULL))",
                floor);
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
}
