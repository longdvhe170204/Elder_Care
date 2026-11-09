package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;

import com.eldercare.backend.billing.internal.period.domain.CostJobCadence;
import com.eldercare.backend.billing.scheduling.M11DailyJob;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Ca lịch thật bổ sung ở 010; không khôi phục các ca khung đã tinh gọn trên develop. */
class CostCalendarSchedulerIT extends MySqlIntegrationTestBase {
    @Autowired
    M11DailyJob daily;

    @Autowired
    TestClockControl clock;

    @Autowired
    JdbcTemplate calendarJdbc;

    @MockitoSpyBean
    ResidentApi residentApi;

    private final Instant first =
            LocalDate.of(2026, 11, 4).atTime(0, 20).atZone(BusinessTime.ZONE).toInstant();

    @BeforeEach
    void setup() {
        doReturn(List.of()).when(residentApi).byStatus(anySet());
        clock.setTo(first);
    }

    @AfterEach
    void reset() {
        clock.reset();
    }

    @Test
    void FR_001_realCalendarTaskCommitsItsPeriodThroughTheScheduler() {
        int statementsBefore = calendarJdbc.queryForObject(
                "SELECT COUNT(*) FROM cost_statement WHERE kind='STATEMENT'", Integer.class);
        daily.runNow();
        assertThat(calendarJdbc.queryForObject(
                        "SELECT COUNT(*) FROM cost_statement WHERE kind='PERIOD' AND period_month='2026-11'",
                        Integer.class))
                .isEqualTo(1);
        assertThat(calendarJdbc.queryForObject(
                        "SELECT COUNT(*) FROM cost_statement WHERE kind='STATEMENT'", Integer.class))
                .isEqualTo(statementsBefore);
        assertThat(calendarJdbc.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M11.JOB_DONE' AND entity_type=? AND entity_id=? AND marker=?",
                        Integer.class,
                        com.eldercare.backend.billing.internal.period.persistence.CostJobMarkers.entityType(
                                "COST_PERIOD_CALENDAR"),
                        CostJobCadence.DAILY.markerId(),
                        first.toString()))
                .isEqualTo(1);
    }
}
