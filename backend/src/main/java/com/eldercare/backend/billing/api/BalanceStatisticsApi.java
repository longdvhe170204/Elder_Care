package com.eldercare.backend.billing.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.dto.BalanceStatusView;
import com.eldercare.backend.billing.api.dto.IncomeExpenseReportView;
import java.time.LocalDate;
import java.util.stream.Stream;

/** Số liệu số dư cho 016 (tên logic {@code SoDuThongKeApi}; module-interfaces 017 mục 2.3; 18.4, 18.5). */
public interface BalanceStatisticsApi {

    /** Hồ sơ đang Sắp hết tiền hoặc Còn nợ trong phạm vi (dashboard, chỉ tiêu {@code BALANCE.LOW_OR_OWING}). */
    Stream<BalanceStatusView> statuses(ScopeFilter scope);

    /** Q-386: tổng nộp, tổng đã trừ theo bảng, tổng hoàn và danh sách còn nợ theo số dư cuối khoảng. */
    IncomeExpenseReportView incomeExpense(LocalDate from, LocalDate to);
}
