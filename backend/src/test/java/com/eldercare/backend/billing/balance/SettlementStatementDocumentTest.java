package com.eldercare.backend.billing.balance;

import static com.eldercare.backend.billing.balance.BalanceEntries.entry;
import static com.eldercare.backend.billing.balance.DocumentReaders.pdfText;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.TypeTotal;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementStatementData;
import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementStatementDocument;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.common.document.FacilityInfo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 017 T032 phần dựng file (FR-034, BR-M11-22; quickstart Q17 → Q21): bảng quyết toán PDF. */
class SettlementStatementDocumentTest {

    private static final FacilityInfo FACILITY = new FacilityInfo(
            "Viện dưỡng lão Bình An", "12 đường Hoà Bình, Hà Nội", "0101234567", "024 3999 8888", null);

    private static SettlementStatementData data(boolean exception, long remaining) {
        return new SettlementStatementData(
                3,
                Instant.parse("2026-11-05T03:00:00Z"),
                "Trần Thị Kế Toán",
                "Nguyễn Thị Ánh",
                "Nguyễn Văn Con",
                "Nội trú dài hạn",
                "HD-2026-0042",
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 12, 31),
                "Hết hạn hợp đồng",
                List.of(new TypeTotal("Phí lưu trú", 4_500_000), new TypeTotal("Thuốc", 120_000)),
                4_620_000,
                6_000_000,
                3_000_000,
                1_000_000,
                500_000,
                List.of(
                        entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 2_000_000, "2026-10-28", "09:00"),
                        entry(7, Ledger.DEPOSIT, TransactionType.DEPOSIT_REFUND, -500_000, "2026-11-04", "10:00")),
                remaining,
                exception);
    }

    @Test
    void BR_M11_22_pdfCarriesPartiesAmountsVersionAndNotAnInvoiceLine() {
        String pdf = pdfText(SettlementStatementDocument.pdf(FACILITY, null, data(false, 0)));

        assertThat(pdf)
                .contains("Viện dưỡng lão Bình An")
                .contains("BẢNG QUYẾT TOÁN")
                .contains("Nguyễn Thị Ánh")
                .contains("Nguyễn Văn Con")
                .contains("Nội trú dài hạn")
                .contains("HD-2026-0042")
                .contains("15/01/2026")
                .contains("31/12/2026")
                .contains("Hết hạn hợp đồng")
                .contains("Phí lưu trú")
                .contains("4.620.000")
                .contains("6.000.000")
                .contains("3.000.000")
                .contains("2.000.000")
                .contains("Trần Thị Kế Toán")
                .contains("05/11/2026")
                .contains("Phiên bản 3")
                .contains("Chứng từ này không thay thế hóa đơn");
    }

    @Test
    void BR_M11_22_remainingAndExceptionAreShownOnlyWhenPresent() {
        String clean = pdfText(SettlementStatementDocument.pdf(FACILITY, null, data(false, 0)));
        String debt = pdfText(SettlementStatementDocument.pdf(FACILITY, null, data(true, -1_500_000)));
        String refund = pdfText(SettlementStatementDocument.pdf(FACILITY, null, data(true, 800_000)));

        assertThat(clean).doesNotContain("ngoại lệ").doesNotContain("Còn nợ").doesNotContain("Còn phải hoàn");
        assertThat(debt).contains("ngoại lệ").contains("Còn nợ").contains("1.500.000");
        assertThat(refund).contains("Còn phải hoàn").contains("800.000");
    }
}
