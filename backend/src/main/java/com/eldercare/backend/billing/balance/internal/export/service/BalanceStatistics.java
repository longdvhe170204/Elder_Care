package com.eldercare.backend.billing.balance.internal.export.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.BalanceStatisticsApi;
import com.eldercare.backend.billing.api.dto.BalanceStatusView;
import com.eldercare.backend.billing.api.dto.IncomeExpenseReportView;
import com.eldercare.backend.billing.balance.internal.export.domain.IncomeExpenseReport;
import com.eldercare.backend.billing.balance.internal.export.persistence.ConfirmedEntries;
import com.eldercare.backend.billing.balance.internal.status.persistence.StatusReads;
import com.eldercare.backend.billing.balance.internal.transaction.domain.BalanceEntry;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link BalanceStatisticsApi} (017 T038; module-interfaces 017 mục 2.3; Q-386, 18.4, 18.5). {@code incomeExpense} và báo
 * cáo thu chi xuất file dùng cùng một cách tính ({@link #report}); {@code statuses} đọc tình trạng đã lưu ở sổ và lọc theo
 * phạm vi đã được 002 đổi ra danh sách hồ sơ.
 */
@Service
public class BalanceStatistics implements BalanceStatisticsApi {

    private final ConfirmedEntries entries;
    private final StatusReads statuses;
    private final BusinessTime time;

    BalanceStatistics(ConfirmedEntries entries, StatusReads statuses, BusinessTime time) {
        this.entries = entries;
        this.statuses = statuses;
        this.time = time;
    }

    /** Báo cáo thu chi của mọi hồ sơ trong khoảng ngày (gồm cả hai đầu). */
    IncomeExpenseReport report(LocalDate from, LocalDate to) {
        var all = entries.upTo(null, time.startOfDay(to.plusDays(1)));
        Map<Long, Long> balanceAtEnd = new HashMap<>();
        for (BalanceEntry entry : all) {
            if (entry.ledger() == Ledger.BALANCE) {
                balanceAtEnd.merge(entry.residentId(), entry.amount(), Long::sum);
            }
        }
        return IncomeExpenseReport.of(all, balanceAtEnd, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<BalanceStatusView> statuses(ScopeFilter scope) {
        return statuses.lowOrOwing().stream()
                .filter(view -> scope.facilityWide() || scope.residentIds().contains(view.residentId()));
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeExpenseReportView incomeExpense(LocalDate from, LocalDate to) {
        IncomeExpenseReport report = report(from, to);
        return new IncomeExpenseReportView(
                from,
                to,
                report.totalPaid(),
                report.totalDeducted(),
                report.totalRefunded(),
                report.debtors().stream()
                        .map(d -> new IncomeExpenseReportView.Debtor(d.residentId(), -d.debt()))
                        .toList());
    }
}
