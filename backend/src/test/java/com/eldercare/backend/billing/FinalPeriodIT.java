package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.admission.api.spi.FinalPeriodPort;
import com.eldercare.backend.billing.api.FinalPeriodCostApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.sql.Timestamp;
import java.time.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Import(CostWorkflowIT.Users.class)
@org.springframework.test.context.event.RecordApplicationEvents
class FinalPeriodIT extends MySqlIntegrationTestBase {
    @Autowired
    FinalPeriodPort port;

    @Autowired
    FinalPeriodCostApi conditions;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser current;

    @Autowired
    com.eldercare.backend.billing.internal.finalperiod.service.FinalPeriodService service;

    @Autowired
    com.eldercare.backend.billing.internal.finalperiod.service.CostStatusParticipant participant;

    @Autowired
    com.eldercare.backend.billing.internal.finalperiod.service.FinalBillingWindow window;

    @Autowired
    com.eldercare.backend.administration.shared.api.ResidentModuleStateApi states;

    @Autowired
    com.eldercare.backend.administration.shared.api.RecordEventApi changes;

    @Autowired
    com.eldercare.backend.billing.internal.period.persistence.CostStatementStore statements;

    @Autowired
    com.eldercare.backend.billing.internal.item.service.CostDestination destination;

    @MockitoSpyBean
    com.eldercare.backend.resident.api.ResidentApi residents;

    @MockitoSpyBean
    com.eldercare.backend.admission.api.ResidencyDayApi days;

    @MockitoSpyBean
    com.eldercare.backend.admission.api.ResidencyEndingReadApi endings;

    @MockitoSpyBean
    com.eldercare.backend.administration.auditlog.api.AuditLogApi audit;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    @Autowired
    com.eldercare.backend.billing.internal.finalperiod.service.FinalPeriodDailyTask daily;

    @MockitoSpyBean
    com.eldercare.backend.billing.internal.finalperiod.persistence.FinalPeriodStore finalStore;

    long resident;
    LocalDate end;

    @BeforeEach
    void setup() {
        current.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        resident = TestUsers.nextId();
        end = time.today().plusDays(5);
        doReturn(false).when(residents).isFinalStatus(resident);
        doReturn(List.of()).when(days).daysBetween(eq(resident), any(), any());
    }

    @Autowired
    org.springframework.test.context.event.ApplicationEvents events;

    @Test
    void FR_035_overdueDaysRemainRequiredAndBillableOnlyForPreparingEnding() {
        end = time.today().minusDays(2);
        long statement = port.createDraft(resident, end);
        fee(statement, end, "FINALIZED", "");
        finalizeTable(statement);
        var todayEnd = end.plusDays(1);
        var required = List.of(
                new com.eldercare.backend.admission.api.dto.ResidencyDayView(
                        end, 1L, 1L, "PRESENT", null, false, false, false, false),
                new com.eldercare.backend.admission.api.dto.ResidencyDayView(
                        todayEnd, 1L, 1L, "PRESENT", null, false, false, false, true));
        doReturn(required).when(days).daysBetween(eq(resident), any(), any());
        doReturn(Optional.of(new com.eldercare.backend.admission.api.dto.ResidencyEndingView(
                        1, resident, com.eldercare.backend.admission.api.dto.EndCase.DISCHARGE, end, end.plusDays(1))))
                .when(endings)
                .preparing(resident);
        doReturn(Optional.of(end)).when(days).billingStopDate(resident);
        assertThat(conditions.finalPeriodCondition(resident).satisfied()).isFalse();
        assertThat(window.overdueBillable(resident, todayEnd)).isTrue();
        assertThat(window.overdueBillable(resident, time.today())).isFalse();
        doReturn(true).when(residents).isFinalStatus(resident);
        assertThat(window.overdueBillable(resident, todayEnd)).isFalse();
    }

    @Test
    void FR_035_dailyReminderIsIdempotentAndNeverCreatesCharges() {
        end = time.today().minusDays(1);
        port.createDraft(resident, end);
        doReturn(List.of(resident)).when(finalStore).trackedResidents();
        assertThat(daily.run(time.now())).isEqualTo(1);
        assertThat(daily.run(time.now())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE resident_id=?", Long.class, resident))
                .isZero();
        doReturn(true).when(residents).isFinalStatus(resident);
        assertThat(daily.run(time.now().plusSeconds(86400))).isZero();
    }

    String status(String table, long id) {
        return jdbc.queryForObject("SELECT status FROM " + table + " WHERE id=?", String.class, id);
    }

    void finalizeTable(long statement) {
        statements.finalizeStatement(statement, current.accountId(), time.now(), Map.of());
    }

    long fee(long statement, LocalDate day, String status, String flags) {
        long id = TestUsers.nextId();
        long period = statements.statement(statement).periodId();
        jdbc.update(
                """
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,source_type,source_id,
                  billing_unit,description,incurred_on,quantity,unit,unit_price,amount,source_values,flags,unique_key,status,locked_at,created_at,created_by)
                VALUES (?,?,?,?,'RESIDENCY_FEE','EVENT_BASED','RESIDENCY_DAY',?,'DAY','Phí lưu trú',?,1,'ngày',100,100,'{}',?,?,?, ?,?,?)
                """,
                id,
                resident,
                statement,
                period,
                resident + ":" + day,
                day,
                flags,
                "RESIDENCY_DAY:" + resident + ":" + day + ":DAY",
                status,
                status.equals("FINALIZED") ? Timestamp.from(time.now()) : null,
                Timestamp.from(time.now()),
                current.accountId());
        return id;
    }

    long manual(long statement, String flags) {
        long id = TestUsers.nextId();
        jdbc.update(
                """
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,description,incurred_on,
                  quantity,unit,amount,reason,flags,created_at,created_by)
                VALUES (?,?,?,?,'OTHER','MANUAL','Khoản kiểm thử',?,1,'khoản',100,'Kiểm thử',?,?,?)
                """,
                id,
                resident,
                statement,
                statements.statement(statement).periodId(),
                end,
                flags,
                Timestamp.from(time.now()),
                current.accountId());
        return id;
    }
}
