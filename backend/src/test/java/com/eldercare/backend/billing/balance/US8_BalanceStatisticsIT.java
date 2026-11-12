package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.dto.IncomeExpenseReportView;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.billing.balance.internal.export.service.BalanceStatistics;
import com.eldercare.backend.billing.balance.internal.status.domain.Trigger;
import com.eldercare.backend.billing.balance.internal.status.service.BalanceStatusService;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.other.service.BalanceLowOrOwingMetric;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 017 T038 ({@code BalanceStatisticsApi} cho 016; Q-386, 18.4): bản thật đọc tình trạng đã lưu theo phạm vi và tính báo
 * cáo thu chi cùng cách với file xuất. Dùng bean thật vì bản giả của A là {@code @Primary}; dữ liệu cả viện nên chỉ kiểm
 * dòng của hồ sơ trong ca.
 */
class US8_BalanceStatisticsIT extends BalanceTestBase {

    @Autowired
    BalanceStatistics statistics;

    @Autowired
    BalanceStatusService statusService;

    @Autowired
    BalanceLowOrOwingMetric lowOrOwing;

    @Test
    void owingResident_isInStatusesOfItsScope_andInTheIncomeExpenseDebtors() throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        long other = sentForSignature(TestUsers.nextId(), null);
        postAs(
                        accountant,
                        "/api/v1/residents/" + a + "/cash-payments",
                        "{\"ledger\":\"BALANCE\",\"amount\":1000000,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\""
                                + time.now() + "\"}")
                .andExpect(status().isCreated());
        as(accountant);
        tx.executeWithoutResult(s -> publisher.publishEvent(new CostStatementFinalized(
                TestUsers.nextId(),
                CostStatementType.REGULAR,
                TestUsers.nextId(),
                a,
                1_700_000,
                time.now(),
                List.of())));

        tx.executeWithoutResult(s -> statusService.recalculate(a, Trigger.PAYMENT, "test"));
        var inScope = statistics
                .statuses(new ScopeFilter(false, Set.of(), Set.of(), Set.of(a)))
                .toList();
        assertThat(inScope).singleElement().satisfies(view -> {
            assertThat(view.residentId()).isEqualTo(a);
            assertThat(view.status()).isEqualTo(BalanceStatus.IN_DEBT);
            assertThat(view.balance()).isEqualTo(-700_000);
        });
        assertThat(statistics
                        .statuses(new ScopeFilter(false, Set.of(), Set.of(), Set.of(other)))
                        .toList())
                .isEmpty();
        assertThat(statistics.statuses(new ScopeFilter(true, Set.of(), Set.of(), Set.of())))
                .extracting(view -> view.residentId())
                .contains(a);

        // 016 `BALANCE.LOW_OR_OWING` đọc bản thật (không có bản giả @Primary trong context này).
        assertThat(lowOrOwing
                        .records(
                                new ScopeFilter(false, Set.of(), Set.of(), Set.of(a, other)),
                                null,
                                null,
                                ReportFilters.NONE)
                        .map(r -> r.view()))
                .singleElement()
                .satisfies(view -> assertThat(view.status()).isEqualTo(BalanceStatus.IN_DEBT));

        LocalDate today = time.today();
        IncomeExpenseReportView report = statistics.incomeExpense(today.minusDays(1), today);
        assertThat(report.debtors()).contains(new IncomeExpenseReportView.Debtor(a, -700_000));
        assertThat(report.totalPaid()).isGreaterThanOrEqualTo(1_000_000);
        assertThat(report.totalStatementDeductions()).isGreaterThanOrEqualTo(1_700_000);
    }
}
