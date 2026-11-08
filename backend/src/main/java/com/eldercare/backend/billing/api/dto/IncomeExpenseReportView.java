package com.eldercare.backend.billing.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo thu chi thu gọn (tên logic {@code BaoCaoThuChiView}; Q-386): chỉ giao dịch Đã xác nhận trong khoảng; tiền
 * nguyên đồng, {@code totalRefunded} là số dương; {@code debtors} theo số dư cuối khoảng.
 */
public record IncomeExpenseReportView(
        LocalDate from,
        LocalDate to,
        long totalPaid,
        long totalStatementDeductions,
        long totalRefunded,
        List<Debtor> debtors) {

    /** Hồ sơ còn nợ; {@code balance} âm. */
    public record Debtor(long residentId, long balance) {}
}
