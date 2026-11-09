package com.eldercare.backend.support.fake.billing;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.BalanceStatisticsApi;
import com.eldercare.backend.billing.api.dto.BalanceStatusView;
import com.eldercare.backend.billing.api.dto.IncomeExpenseReportView;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Bản giả {@link BalanceStatisticsApi} (017, A giữ) cho 016: fixture đặt sẵn dòng đã thuộc từng phạm vi, không mô phỏng
 * lọc phạm vi hay tính tình trạng thật; báo cáo thu chi chưa đặt thì mọi tổng bằng 0.
 */
public class FakeBalanceStatistics implements BalanceStatisticsApi {

    private final Map<ScopeFilter, List<BalanceStatusView>> statuses = new HashMap<>();
    private IncomeExpenseReportView report;

    public FakeBalanceStatistics statuses(ScopeFilter scope, List<BalanceStatusView> rows) {
        statuses.put(scope, List.copyOf(rows));
        return this;
    }

    public FakeBalanceStatistics incomeExpense(IncomeExpenseReportView value) {
        report = value;
        return this;
    }

    public void reset() {
        statuses.clear();
        report = null;
    }

    @Override
    public Stream<BalanceStatusView> statuses(ScopeFilter scope) {
        return statuses.getOrDefault(scope, List.of()).stream();
    }

    @Override
    public IncomeExpenseReportView incomeExpense(LocalDate from, LocalDate to) {
        return report != null ? report : new IncomeExpenseReportView(from, to, 0, 0, 0, List.of());
    }
}
