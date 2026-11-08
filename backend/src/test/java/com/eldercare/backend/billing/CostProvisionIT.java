package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.billing.api.dto.CostTypeTotal;
import com.eldercare.backend.billing.internal.export.service.CostAccountingExport;
import com.eldercare.backend.billing.internal.provision.service.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.dto.IdentityView;
import com.eldercare.backend.support.TestUsers;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** T029/T036: dùng lại fixture 010, MySQL và lệnh thật. */
class CostProvisionIT extends CostWorkflowIT {
    @Autowired
    CostSummaries summaries;

    @Autowired
    CostForBalance balance;

    @Autowired
    CostAccountingExport exports;

    private void finalizeCosts() {
        statements.submit(statement);
        current.set(manager.user());
        statements.finalizeStatement(
                statement, "\"" + statements.read(statement).version() + "\"");
    }

    @Test
    void FR_038a_totalIncludesDraftEventsButNotUnapprovedManualAndNotesMissingPrices() {
        item("EVENT_BASED", 300L, "");
        item("EVENT_BASED", null, "MISSING_UNIT_PRICE");
        long manual = item("MANUAL", 700L, "");
        var summary = summaries.provisional(resident, null, null);
        assertThat(summary.hasUnpricedItems()).isTrue();
        assertThat(summary.totalsByType()).extracting(CostTypeTotal::total).containsExactly(300L);
        assertThat(balance.unfinalizedTotal(resident)).isEqualTo(300);
        approve(manual);
        assertThat(balance.unfinalizedTotal(resident)).isEqualTo(1000);
        assertThat(summaries
                        .provisional(resident, billedMonth.atDay(2), billedMonth.atEndOfMonth())
                        .totalsByType())
                .isEmpty();
    }

    @Test
    void FR_042_portOnlyReturnsFinalizedItemsAndSavedTotalsWithoutMedicationNames() {
        long id = item("EVENT_BASED", 300L, "");
        jdbc.update(
                "UPDATE cost_item SET cost_type='MEDICATION',description='Thông tin thuốc không được xuất',item_code='VT01' WHERE id=?",
                id);
        assertThat(balance.finalizedStatement(statement)).isEmpty();
        approve(id);
        finalizeCosts();
        var result = balance.finalizedStatement(statement).orElseThrow();
        assertThat(result.periodId()).isEqualTo(period);
        assertThat(result.statement().total()).isEqualTo(300);
        assertThat(result.statement().items()).singleElement().satisfies(i -> {
            assertThat(i.description()).isEqualTo("Thuốc VT01");
            assertThat(i.medicationName()).isNull();
            assertThat(i.recalculationHistory()).isEmpty();
            assertThat(i.adjustments()).isEmpty();
        });
        assertThat(balance.unfinalizedTotal(resident)).isZero();
    }

    @Test
    void FR_039_041_exportIsDeterministicAndNeverExportedExcludesPreviouslyExportedStatements() {
        long id = item("EVENT_BASED", 300L, "");
        approve(id);
        finalizeCosts();
        doReturn(new IdentityView(resident, "NCT-TEST", "Người kiểm thử", Optional.empty()))
                .when(residentApi)
                .identity(resident);
        current.set(new TestUsers(jdbc, time).staff(Role.ACCOUNTANT).user());
        var first = exports.export(period, "ALL", "CSV");
        assertThat(new String(first.bytes(), StandardCharsets.UTF_8))
                .contains("Xuất lần đầu", "1 bảng; 1 khoản; 300 đồng");
        assertThat(exports.export(period, "ALL", "CSV").bytes()).isEqualTo(first.bytes());
        assertThat(new String(exports.export(period, "NEVER_EXPORTED", "CSV").bytes(), StandardCharsets.UTF_8))
                .contains("0 bảng; 0 khoản; 0 đồng")
                .doesNotContain("Người kiểm thử");
        assertThat(exports.export(period, "ALL", "XLSX").bytes()).startsWith((byte) 'P', (byte) 'K');
        assertThat(jdbc.queryForObject("SELECT first_export_id FROM cost_statement WHERE id=?", Long.class, statement))
                .isPositive();
    }

    @Test
    void FR_043_officeCannotExportEvenThoughItCanSubmitStatements() {
        assertThatThrownBy(() -> exports.export(period, "ALL", "CSV")).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_039_threeFinalizedResidentsAndOnePending_exportOnlyNewlyFinalizedOnNextRequest() {
        long[] residents = {resident, TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId()};
        long[] statementIds = {statement, TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId()};
        for (int index = 1; index < statementIds.length; index++) {
            jdbc.update("""
                    INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)
                    VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)
                    """, statementIds[index], period, residents[index],
                    "STATEMENT:" + period + ":" + residents[index] + ":REGULAR",
                    java.sql.Timestamp.from(time.now()));
        }
        for (int index = 0; index < residents.length; index++) {
            doReturn(new IdentityView(residents[index], "NCT-" + index, "Người kiểm thử " + index, Optional.empty()))
                    .when(residentApi).identity(residents[index]);
            if (index == 3) continue;
            resident = residents[index];
            statement = statementIds[index];
            current.set(office.user());
            approve(item("EVENT_BASED", 100L * (index + 1), ""));
            finalizeCosts();
        }
        var accountant = new TestUsers(jdbc, time).staff(Role.ACCOUNTANT).user();
        current.set(accountant);
        String first = new String(exports.export(period, "ALL", "CSV").bytes(), StandardCharsets.UTF_8);
        assertThat(first).contains("3 bảng; 3 khoản; 600 đồng", "NCT-3 — Người kiểm thử 3");
        assertThat(jdbc.queryForObject("SELECT first_export_id FROM cost_statement WHERE id=?",
                Long.class, statementIds[3])).isNull();

        resident = residents[3];
        statement = statementIds[3];
        current.set(office.user());
        approve(item("EVENT_BASED", 400L, ""));
        finalizeCosts();
        current.set(accountant);
        String next = new String(exports.export(period, "NEVER_EXPORTED", "CSV").bytes(), StandardCharsets.UTF_8);
        assertThat(next).contains("1 bảng; 1 khoản; 400 đồng", "Người kiểm thử 3")
                .doesNotContain("Người kiểm thử 0", "Người kiểm thử 1", "Người kiểm thử 2");
    }
}
