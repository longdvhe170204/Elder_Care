package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertResponsibility;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dựng {@link AlertService} với bảng cảnh báo, khóa theo người, dòng nguồn ({@code record_event}) và tham số CFG thật
 * (CFG-M05-01 15 phút, CFG-M05-02 3 lần); trạng thái người cao tuổi, lịch ca, tầng là bản giả. Mỗi test một người cao
 * tuổi mới nên không thấy dữ liệu sót.
 */
abstract class AlertRequestTestBase extends MySqlIntegrationTestBase {
    static final Instant T0 = Instant.parse("2031-03-04T02:00:00Z");

    @Autowired
    AlertStore alerts;

    @Autowired
    MonitoringLock monitoringLock;

    @Autowired
    RecordEventApi recordEvents;

    @Autowired
    ParameterApi parameters;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    final BusinessTime time = mock(BusinessTime.class);
    final AlertResponsibility responsibility = mock(AlertResponsibility.class);
    final List<Object> published = new CopyOnWriteArrayList<>();
    AlertService service;
    long resident;
    long floor;

    @BeforeEach
    void alertService() {
        resident = TestUsers.nextId();
        floor = TestUsers.nextId();
        when(time.now()).thenReturn(T0);
        when(responsibility.floorOf(anyLong(), any())).thenReturn(Optional.of(floor));
        service = new AlertService(
                alerts, published::add, parameters, time, monitoringLock, null, null, recordEvents, responsibility);
    }

    AlertRecord request(String typeLock, IncidentLevel level, Instant at, long sourceId) {
        return transaction.execute(status -> service.requestAlert(new CreateAlertCommand(
                        resident, null, "CHI_SO", typeLock, level, at, "M06.VITAL_SIGN_RECORD", sourceId, null, null))
                .orElseThrow());
    }

    List<Long> openIds(String typeLock) {
        return jdbc.queryForList(
                "SELECT id FROM alert WHERE resident_id = ? AND type_lock = ?"
                        + " AND status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS')",
                Long.class,
                resident,
                typeLock);
    }
}
