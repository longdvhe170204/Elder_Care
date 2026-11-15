package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.billing.internal.item.domain.CostItemSelection;
import com.eldercare.backend.billing.internal.item.service.CostItemService;
import com.eldercare.backend.billing.internal.period.service.CostStatementService;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.*;
import org.springframework.transaction.annotation.Transactional;

/** T016: trạng thái/quyền thật, MySQL thật; không lập nguồn hoặc khoản bằng job. */
@Transactional
@Import(CostWorkflowIT.Users.class)
@RecordApplicationEvents
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class CostWorkflowIT extends MySqlIntegrationTestBase {
    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser costWorkflowUser() {
            return new SwitchableUser();
        }
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser current;

    @Autowired
    CostItemService items;

    @Autowired
    CostStatementService statements;

    @Autowired
    ApplicationEvents events;

    @Autowired
    org.springframework.test.web.servlet.MockMvc mvc;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    @Autowired
    com.eldercare.backend.billing.internal.period.service.CostLock costLock;

    @Autowired
    com.eldercare.backend.billing.internal.period.service.CostDailyTask calendar;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    AuditLogApi audit;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.resident.api.ResidentApi residentApi;

    TestUsers.TestUser office, manager;
    long period, statement, resident;
    YearMonth billedMonth;

    @BeforeEach
    void fixture() {
        // Fixture chỉ tạo bảng/khoản 010, không tạo hồ sơ 001. Không quét hồ sơ sót của IT khác.
        org.mockito.Mockito.doReturn(List.of()).when(residentApi).byStatus(org.mockito.ArgumentMatchers.anySet());
        var users = new TestUsers(jdbc, time);
        office = users.staff(Role.OFFICE_STAFF);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        current.set(office.user());
        resident = TestUsers.nextId();
        period = TestUsers.nextId();
        statement = TestUsers.nextId();
        long seed = Math.floorMod(resident, 12000);
        var month = YearMonth.of(1000 + (int) seed / 12, 1 + (int) seed % 12);
        billedMonth = month;
        var at = Timestamp.from(time.now());
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,
                  status,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,'AWAITING_FINALIZATION',?,?)
                """, period, month.toString(), month.atDay(1), month.atEndOfMonth(), "PERIOD:" + month, at);
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)
                VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)
                """, statement, period, resident, "STATEMENT:" + period + ":" + resident + ":REGULAR", at);
    }

    long item(String origin, Long amount, String flags) {
        long id = TestUsers.nextId();
        String source = "source-" + id;
        jdbc.update(
                """
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,
                  source_type,source_id,billing_unit,description,incurred_on,quantity,unit,unit_price,
                  amount,flags,unique_key,reason,created_at,created_by)
                VALUES (?,?,?,?,'SERVICE',?,?,?,?, 'Dịch vụ kiểm thử',?,1,'UNIT',?,?,?,?, 'Kiểm thử',?,?)
                """,
                id,
                resident,
                statement,
                period,
                origin,
                origin.equals("EVENT_BASED") ? "CARE_TASK_RESULT" : null,
                origin.equals("EVENT_BASED") ? source : null,
                origin.equals("EVENT_BASED") ? "UNIT" : null,
                billedMonth.atDay(1),
                amount,
                amount,
                flags,
                origin.equals("EVENT_BASED") ? "CARE_TASK_RESULT:" + source + ":UNIT" : null,
                Timestamp.from(time.now()),
                office.accountId());
        return id;
    }

    CostItemSelection selection(long... ids) {
        return new CostItemSelection(Arrays.stream(ids).boxed().toList(), null);
    }

    String status(String table, long id) {
        return jdbc.queryForObject("SELECT status FROM " + table + " WHERE id=?", String.class, id);
    }

    void approve(long id) {
        items.check(selection(id));
        current.set(manager.user());
        assertThat(items.approve(selection(id), null).succeededIds()).containsExactly(id);
        current.set(office.user());
    }

    @Test
    void FR_015_returningAStatementDoesNotAcceptItemsFromAnotherStatement() {
        long id = item("EVENT_BASED", 300L, "");
        items.check(selection(id));
        statements.submit(statement);
        current.set(manager.user());
        assertThatThrownBy(() ->
                        statements.returnStatement(statement, List.of(TestUsers.nextId()), new Reason("Đối chiếu lại")))
                .isInstanceOf(BusinessException.class);
        assertThat(status("cost_statement", statement)).isEqualTo("AWAITING_FINALIZATION");
        assertThat(status("cost_item", id)).isEqualTo("CHECKED");
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void FR_033_aConcurrentDraftCannotBeHiddenByAnOlderTransactionSnapshot() throws Exception {
        long id = item("EVENT_BASED", 300L, "");
        approve(id);
        statements.submit(statement);
        current.set(manager.user());
        int version = statements.read(statement).version();
        var inserted = new java.util.concurrent.CountDownLatch(1);
        var oldSnapshot = new java.util.concurrent.CountDownLatch(1);
        var committed = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var insertion = pool.submit(() -> {
                new org.springframework.transaction.support.TransactionTemplate(transactions)
                        .executeWithoutResult(tx -> {
                            costLock.statement(resident, statement);
                            item("EVENT_BASED", 100L, "");
                            inserted.countDown();
                            await(oldSnapshot);
                        });
                committed.countDown();
            });
            assertThat(inserted.await(15, java.util.concurrent.TimeUnit.SECONDS))
                    .isTrue();
            var finalization = pool.submit(() -> new org.springframework.transaction.support.TransactionTemplate(
                            transactions)
                    .execute(tx -> {
                        jdbc.queryForObject("SELECT version FROM cost_statement WHERE id=?", Integer.class, statement);
                        oldSnapshot.countDown();
                        await(committed);
                        statements.finalizeStatement(statement, "\"" + version + "\"");
                        return true;
                    }));
            insertion.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertThatThrownBy(() -> finalization.get(20, java.util.concurrent.TimeUnit.SECONDS))
                    .rootCause()
                    .isInstanceOf(VersionMismatchException.class);
        }
        assertThat(status("cost_statement", statement)).isEqualTo("OPEN");
        assertThat(status("cost_item", id)).isEqualTo("APPROVED");
    }

    private void await(java.util.concurrent.CountDownLatch latch) {
        try {
            if (!latch.await(15, java.util.concurrent.TimeUnit.SECONDS))
                throw new IllegalStateException("Hết thời gian chờ transaction");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
