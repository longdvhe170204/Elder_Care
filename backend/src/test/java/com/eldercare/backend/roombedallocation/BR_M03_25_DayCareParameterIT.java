package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.administration.parameter.api.events.*;
import com.eldercare.backend.roombedallocation.api.dto.SessionDate;
import com.eldercare.backend.roombedallocation.api.events.DayCareOverCapacity;
import com.eldercare.backend.roombedallocation.internal.daycare.service.DayCareParameterListener;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeDayCareArrivals;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.*;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Đổi CFG phải đọc giá trị mới và phát cảnh báo mà không hủy lịch. */
@Transactional
@RecordApplicationEvents
class BR_M03_25_DayCareParameterIT extends StructureTestSupport {
    @Autowired
    DayCareParameterListener listener;

    @Autowired
    FakeDayCareArrivals arrivals;

    @Autowired
    ApplicationEvents events;

    @MockitoSpyBean
    ParameterApi parameters;

    @Test
    void BR_M03_25_capacityChangeRecountsFutureSessionsWithCurrentValue() {
        arrivals.reset();
        var json = JsonMapper.builder().build();
        long session = TestUsers.nextId();
        jdbc.update("INSERT INTO day_care_rest_area(code,name,floor_id) VALUES('D1','Khu nghỉ',?)", floor);
        jdbc.update(
                "INSERT INTO day_care_session(id,name,start_time,end_time) VALUES(?,'Sáng','07:00','12:00')", session);
        arrivals.count(new SessionDate(time.today().plusDays(1), session), 4);
        doReturn(new ParameterValue(ParameterCode.CFG_M03_01, ParameterKind.INTEGER, json.readTree("3")))
                .when(parameters)
                .read(ParameterCode.CFG_M03_01);
        user.set(com.eldercare.backend.support.FakePermissions.user(0, com.eldercare.backend.common.Role.SCHEDULER));
        DayCareParameterListener target = AopTestUtils.getTargetObject(listener);
        target.changed(
                new ParameterChanged(ParameterCode.CFG_M03_01, json.readTree("5"), json.readTree("3"), time.now()));
        assertThat(events.stream(DayCareOverCapacity.class))
                .singleElement()
                .satisfies(event -> assertThat(event.sessions())
                        .containsExactly(new SessionDate(time.today().plusDays(1), session)));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='M03_DAY_CARE_OVER_CAPACITY'", Integer.class))
                .isPositive();
        target.changed(
                new ParameterChanged(ParameterCode.CFG_M03_02, json.readTree("2"), json.readTree("3"), time.now()));
        assertThat(events.stream(DayCareOverCapacity.class).count()).isEqualTo(1);
    }
}
