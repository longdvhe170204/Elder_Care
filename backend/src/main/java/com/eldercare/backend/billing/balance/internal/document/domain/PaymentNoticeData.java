package com.eldercare.backend.billing.balance.internal.document.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Số liệu đã chụp của một phiếu báo phí (017 FR-032; research E-10), đầu vào của bộ dựng PDF và Excel. Tiền theo đồng.
 * Khoản thuốc chỉ ghi "Thuốc" và mã vật phẩm, không tên thuốc (người dựng số liệu bảo đảm).
 *
 * @param representativeName người đại diện, {@code null} nếu hồ sơ không có
 * @param payment tài khoản thu; {@code null} khi CFG-M11-10 chưa có giá trị
 */
public record PaymentNoticeData(
        String code,
        LocalDate issuedOn,
        String issuedBy,
        String residentName,
        String representativeName,
        String period,
        StatementType statementType,
        List<Line> lines,
        List<TypeTotal> totalsByType,
        long total,
        long balanceBefore,
        long balanceAfter,
        PaymentInstructions payment) {

    public record Line(
            LocalDate incurredOn,
            String costType,
            String description,
            BigDecimal quantity,
            long unitPrice,
            long amount) {}

    public record TypeTotal(String costType, long total) {}

    public record PaymentInstructions(
            String bankName, String accountNumber, String accountHolder, String paymentCode) {}
}
