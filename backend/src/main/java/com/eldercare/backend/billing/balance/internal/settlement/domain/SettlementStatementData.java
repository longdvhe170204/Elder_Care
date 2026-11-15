package com.eldercare.backend.billing.balance.internal.settlement.domain;

import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.TypeTotal;
import com.eldercare.backend.billing.balance.internal.transaction.domain.BalanceEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Số liệu chụp lúc Xác nhận quyết toán (017 FR-034; research E-09), đầu vào của bộ dựng PDF bảng quyết toán. Tiền theo
 * đồng. {@code remaining} có dấu: âm là còn nợ, dương là còn phải hoàn, 0 khi đã quyết toán đủ (khác 0 chỉ khi có ngoại lệ
 * đã duyệt).
 *
 * @param balanceBeforeSettlement số dư ngay trước giao dịch thanh toán bảng kỳ cuối
 * @param transactions các giao dịch nộp, hoàn kể từ thanh toán bảng kỳ cuối
 */
public record SettlementStatementData(
        int version,
        Instant confirmedAt,
        String confirmedBy,
        String residentName,
        String representativeName,
        String stayType,
        String contractCode,
        LocalDate startDate,
        LocalDate expectedEndDate,
        String endCase,
        List<TypeTotal> finalPeriodTotalsByType,
        long finalPeriodTotal,
        long balanceBeforeSettlement,
        long depositReceived,
        long depositOffset,
        long depositRefunded,
        List<BalanceEntry> transactions,
        long remaining,
        boolean hasException) {}
