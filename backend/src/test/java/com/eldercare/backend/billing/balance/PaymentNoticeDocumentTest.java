package com.eldercare.backend.billing.balance;

import static com.eldercare.backend.billing.balance.DocumentReaders.excelCells;
import static com.eldercare.backend.billing.balance.DocumentReaders.pdfText;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.Line;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.PaymentInstructions;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.TypeTotal;
import com.eldercare.backend.billing.balance.internal.document.domain.StatementType;
import com.eldercare.backend.billing.balance.internal.document.service.PaymentNoticeDocument;
import com.eldercare.backend.common.document.FacilityInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 017 T035 phần dựng file (FR-032, BR-M11-21; quickstart Q25 → Q27): phiếu báo phí PDF và Excel cùng số liệu. */
class PaymentNoticeDocumentTest {

    private static final FacilityInfo FACILITY = new FacilityInfo(
            "Viện dưỡng lão Bình An", "12 đường Hoà Bình, Hà Nội", "0101234567", "024 3999 8888", null);
    private static final PaymentInstructions PAYMENT =
            new PaymentInstructions("Vietcombank", "0011004000123", "VIEN DUONG LAO BINH AN", "NCT7K2M9X4A");

    private static PaymentNoticeData notice(long balanceBefore, long balanceAfter, PaymentInstructions payment) {
        return new PaymentNoticeData(
                "PBP-202610-42",
                LocalDate.of(2026, 11, 3),
                "Trần Thị Kế Toán",
                "Nguyễn Thị Ánh",
                "Nguyễn Văn Con",
                "10/2026",
                StatementType.REGULAR,
                List.of(
                        new Line(
                                LocalDate.of(2026, 10, 31),
                                "Phí lưu trú",
                                "Phí lưu trú tháng 10",
                                BigDecimal.ONE,
                                9_000_000,
                                9_000_000),
                        new Line(
                                LocalDate.of(2026, 10, 12),
                                "Thuốc",
                                "Thuốc, mã vật phẩm MED-0042",
                                new BigDecimal("2"),
                                125_000,
                                250_000)),
                List.of(new TypeTotal("Phí lưu trú", 9_000_000), new TypeTotal("Thuốc", 250_000)),
                9_250_000,
                balanceBefore,
                balanceAfter,
                payment);
    }

    @Test
    void BR_63_pdfCarriesHeaderLinesTotalsAndBalances() {
        String pdf = pdfText(PaymentNoticeDocument.pdf(FACILITY, null, notice(8_000_000, -1_250_000, PAYMENT)));

        assertThat(pdf)
                .contains("Viện dưỡng lão Bình An")
                .contains("PHIẾU BÁO PHÍ")
                .contains("PBP-202610-42")
                .contains("03/11/2026")
                .contains("Trần Thị Kế Toán")
                .contains("Nguyễn Thị Ánh")
                .contains("Nguyễn Văn Con")
                .contains("10/2026")
                .contains("Phí lưu trú tháng 10")
                .contains("0042")
                .contains("9.250.000")
                .contains("8.000.000")
                .contains("1.250.000");
    }

    @Test
    void BR_63_negativeBalanceAfterShowsDebtAndPaymentInstructions() {
        String pdf = pdfText(PaymentNoticeDocument.pdf(FACILITY, null, notice(8_000_000, -1_250_000, PAYMENT)));

        assertThat(pdf)
                .contains("Số còn nợ")
                .contains("NCT7K2M9X4A")
                .contains("0011004000123")
                .contains("Vietcombank")
                .contains("VIEN DUONG LAO BINH AN");
    }

    @Test
    void BR_63_excelHasTheSameFiguresAsPdf() {
        List<String> cells = excelCells(PaymentNoticeDocument.excel(FACILITY, notice(8_000_000, -1_250_000, PAYMENT)));

        assertThat(cells).contains("Phí lưu trú tháng 10", "9250000", "250000", "8000000", "-1250000");
        assertThat(String.join("\n", cells))
                .contains("PBP-202610-42")
                .contains("Nguyễn Thị Ánh")
                .contains("NCT7K2M9X4A")
                .contains("Chứng từ này không thay thế hóa đơn");
    }
}
