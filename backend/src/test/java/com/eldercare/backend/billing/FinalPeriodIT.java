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

    @Test
    void BR_M11_08_createsOnlyOneDraftInCallerTransaction() {
        long id = port.createDraft(resident, end);
        assertThat(port.createDraft(resident, end)).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT final_period FROM cost_statement WHERE id=?", Boolean.class, id))
                .isTrue();
        assertThat(conditions.finalPeriodCondition(resident).satisfied()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE resident_id=?", Long.class, resident))
                .isZero();
    }

    @Autowired
    org.springframework.test.context.event.ApplicationEvents events;

    @Test
    void FR_044_endDateChangeNotifiesOfficeStaff() {
        port.createDraft(resident, end);
        port.recalculate(resident, end.plusDays(3));
        assertThat(events.stream(com.eldercare.backend.billing.internal.period.domain.CostNotificationEvent.class)
                        .flatMap(e -> e.notificationRequests().stream())
                        .filter(n -> n.eventType().equals("FINAL_PERIOD_CHANGED")))
                .isNotEmpty()
                .allSatisfy(n -> assertThat(n.recipientGroups())
                        .containsExactly(new com.eldercare.backend.common.notification.RecipientGroup.FacilityRole(
                                Role.OFFICE_STAFF)));
    }

    @Test
    void FR_044_recheckWithUnmetCondition_notifiesOfficeStaffOncePerDay() {
        port.createDraft(resident, end);
        service.recheck(resident);
        service.recheck(resident);
        assertThat(events.stream(com.eldercare.backend.billing.internal.period.domain.CostNotificationEvent.class)
                        .flatMap(e -> e.notificationRequests().stream())
                        .filter(n -> n.eventType().equals("FINAL_PERIOD_CONDITION_UNMET")))
                .singleElement()
                .satisfies(n -> assertThat(n.recipientGroups())
                        .containsExactly(new com.eldercare.backend.common.notification.RecipientGroup.FacilityRole(
                                Role.OFFICE_STAFF)));
    }

    @Test
    void FR_034_cancelClearsFinalMarker() {
        long id = port.createDraft(resident, end);
        port.cancelStop(resident);
        assertThat(jdbc.queryForObject("SELECT final_period FROM cost_statement WHERE id=?", Boolean.class, id))
                .isFalse();
        assertThat(conditions.finalPeriodCondition(resident).satisfied()).isFalse();
    }

    @Test
    void FR_034_dateChangeAcrossMonthMovesUnfinalizedMarker() {
        long old = port.createDraft(resident, end);
        LocalDate changed = end.plusMonths(1);
        port.recalculate(resident, changed);
        assertThat(jdbc.queryForObject("SELECT final_period FROM cost_statement WHERE id=?", Boolean.class, old))
                .isFalse();
        assertThat(jdbc.queryForList(
                        "SELECT through_date FROM cost_statement WHERE resident_id=? AND final_period=1",
                        LocalDate.class,
                        resident))
                .containsExactly(changed);
    }

    @Test
    void FR_034_shorterPeriodCancelsOnlyDaysAfterNewEnd() {
        long statement = port.createDraft(resident, end);
        long kept = fee(statement, end.minusDays(2), "DRAFT", "");
        long cancelled = fee(statement, end, "APPROVED", "CREATED_IN_ADVANCE");
        port.recalculate(resident, end.minusDays(1));
        assertThat(status("cost_item", kept)).isEqualTo("DRAFT");
        assertThat(status("cost_item", cancelled)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT cancellation_reason FROM cost_item WHERE id=?", String.class, cancelled))
                .isEqualTo("Đổi ngày kết thúc dự kiến");
        assertThat(jdbc.queryForObject("SELECT unique_key FROM cost_item WHERE id=?", String.class, cancelled))
                .isNull();
    }

    @Test
    void FR_034_finalizedDateChangePreservesMoneyAndRecordsSourceChange() {
        long statement = port.createDraft(resident, end);
        long item = fee(statement, end, "FINALIZED", "CREATED_IN_ADVANCE");
        finalizeTable(statement);
        port.recalculate(resident, end.minusDays(1));
        assertThat(status("cost_item", item)).isEqualTo("FINALIZED");
        assertThat(jdbc.queryForObject(
                        "SELECT through_date FROM cost_statement WHERE id=?", LocalDate.class, statement))
                .isEqualTo(end);
        assertThat(changes.list(
                        com.eldercare.backend.billing.internal.adjustment.domain.PostFinalizationChange.KIND,
                        new com.eldercare.backend.administration.shared.api.dto.EntityRef("M11.COST_ITEM", item)))
                .hasSize(1);
        port.recalculate(resident, end.minusDays(1));
        assertThat(changes.list(
                        com.eldercare.backend.billing.internal.adjustment.domain.PostFinalizationChange.KIND,
                        new com.eldercare.backend.administration.shared.api.dto.EntityRef("M11.COST_ITEM", item)))
                .hasSize(1);
    }

    @Test
    void FR_034a_finalizedDayCoverageAndTerminationFlagsAreRequired() {
        end = time.today().minusDays(1);
        long statement = port.createDraft(resident, end);
        var day = new com.eldercare.backend.admission.api.dto.ResidencyDayView(
                end, 1L, 1L, "PRESENT", null, false, false, false, false);
        doReturn(List.of(day)).when(days).daysBetween(eq(resident), any(), any());
        fee(statement, end, "FINALIZED", "");
        finalizeTable(statement);
        doReturn(List.of(
                        day,
                        new com.eldercare.backend.admission.api.dto.ResidencyDayView(
                                end.minusDays(1), 1L, 1L, "PRESENT", null, false, false, false, false)))
                .when(days)
                .daysBetween(eq(resident), any(), any());
        assertThat(conditions.finalPeriodCondition(resident).satisfied()).isFalse();
        doReturn(List.of(day)).when(days).daysBetween(eq(resident), any(), any());
        assertThat(conditions.finalPeriodCondition(resident).satisfied()).isTrue();
        var supplement = destination.adjustment(resident, current.accountId());
        long item = manual(supplement.id(), "AFFECTS_TERMINATION");
        assertThat(conditions.finalPeriodCondition(resident).satisfied()).isFalse();
        jdbc.update("UPDATE cost_item SET flags='' WHERE id=?", item);
        assertThat(conditions.finalPeriodCondition(resident).satisfied()).isTrue();
    }

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
    void FR_036_deathKeepsWholeDayAndCancelsFollowingDays() {
        end = time.today();
        long statement = port.createDraft(resident, end.plusDays(2));
        long kept = fee(statement, end, "DRAFT", "");
        long cancelled = fee(statement, end.plusDays(1), "DRAFT", "CREATED_IN_ADVANCE");
        var death = end.atTime(10, 0).atZone(BusinessTime.ZONE).toInstant();
        service.death(resident, death);
        assertThat(status("cost_item", kept)).isEqualTo("DRAFT");
        assertThat(status("cost_item", cancelled)).isEqualTo("CANCELLED");
        assertThat(window.afterDeath(resident, end, death.minusSeconds(1), false))
                .isFalse();
        assertThat(window.afterDeath(resident, end, death.plusSeconds(1), false))
                .isTrue();
        assertThat(window.afterDeath(resident, end, null, true)).isFalse();
    }

    @Test
    void FR_036_postDeathCompletionNeverReopensForLaterSupplement() {
        long statement = port.createDraft(resident, end);
        service.death(resident, end.atTime(10, 0).atZone(BusinessTime.ZONE).toInstant());
        assertThat(service.status(resident).status())
                .isEqualTo(com.eldercare.backend.admission.api.dto.PostDeathItemStatus.NOT_COMPLETED);
        finalizeTable(statement);
        assertThat(service.status(resident).status())
                .isEqualTo(com.eldercare.backend.admission.api.dto.PostDeathItemStatus.COMPLETED);
        destination.adjustment(resident, current.accountId());
        assertThat(service.status(resident).status())
                .isEqualTo(com.eldercare.backend.admission.api.dto.PostDeathItemStatus.COMPLETED);
    }

    @Test
    void FR_029_lateCostsGoToSupplementaryStatementEvenInNewMonth() {
        long statement = port.createDraft(resident, end);
        finalizeTable(statement);
        var target = destination.adjustment(resident, current.accountId());
        assertThat(target.statementType()).isEqualTo("SUPPLEMENTARY");
        assertThat(destination.adjustment(resident, current.accountId()).id()).isEqualTo(target.id());
    }

    @Test
    void FR_036_previouslyFinalizedMonthlyStatementIsStillTrackedAsTheFinalPeriod() {
        var regular = destination.regular(resident, YearMonth.from(end), current.accountId());
        finalizeTable(regular.id());
        assertThat(port.createDraft(resident, end)).isEqualTo(regular.id());
        assertThat(jdbc.queryForObject(
                        "SELECT final_period FROM cost_statement WHERE id=?", Boolean.class, regular.id()))
                .isFalse();
        assertThat(finalStore.trackedResidents()).contains(resident);
        assertThat(states.get(com.eldercare.backend.billing.internal.period.domain.BillingResidentState.KIND, resident)
                        .orElseThrow()
                        .finalStatementId())
                .isEqualTo(regular.id());
    }

    @Test
    void KT_A_CHI_PHI_conditionIsWaivable() {
        var context = new com.eldercare.backend.resident.api.dto.CommandContext(
                resident,
                com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE,
                com.eldercare.backend.resident.api.dto.StatusCommand.END_RESIDENCY,
                current,
                time.now(),
                null);
        assertThat(participant.checkConditions(context)).singleElement().satisfies(c -> {
            assertThat(c.code()).isEqualTo("END_A_COSTS");
            assertThat(c.waivable()).isTrue();
        });
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

    @Test
    void FR_029_supplementReminderContinuesAfterFinalStatus() {
        long statement = port.createDraft(resident, end);
        finalizeTable(statement);
        destination.adjustment(resident, current.accountId());
        doReturn(true).when(residents).isFinalStatus(resident);
        doReturn(List.of(resident)).when(finalStore).trackedResidents();
        assertThat(daily.run(time.now())).isEqualTo(1);
        assertThat(daily.run(time.now())).isZero();
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void BR_M11_08_requiresCallerTransaction() {
        assertThatThrownBy(() -> port.createDraft(resident, end))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void FR_034_auditFailureRollsBackStatementAndState() {
        com.eldercare.backend.administration.auditlog.api.AuditLogApi target =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(audit);
        doThrow(new IllegalStateException("audit unavailable")).when(target).write(any());
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        assertThatThrownBy(() -> transaction.execute(s -> port.createDraft(resident, end)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_statement WHERE resident_id=?", Long.class, resident))
                .isZero();
        assertThat(states.get(com.eldercare.backend.billing.internal.period.domain.BillingResidentState.KIND, resident))
                .isEmpty();
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
