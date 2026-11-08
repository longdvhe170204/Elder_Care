package com.eldercare.backend.billing.balance;

import static com.eldercare.backend.billing.balance.BalanceEntries.entry;
import static com.eldercare.backend.billing.balance.DocumentReaders.excelCells;
import static com.eldercare.backend.billing.balance.DocumentReaders.pdfText;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.balance.export.domain.BalanceStatement;
import com.eldercare.backend.billing.internal.balance.export.domain.IncomeExpenseReport;
import com.eldercare.backend.billing.internal.balance.export.domain.IncomeExpenseReport.Debtor;
import com.eldercare.backend.billing.internal.balance.export.service.BalanceExportDocument;
import com.eldercare.backend.billing.internal.balance.export.service.BalanceExportDocument.ResidentStatement;
import com.eldercare.backend.billing.internal.balance.transaction.domain.BalanceEntry;
import com.eldercare.backend.billing.internal.balance.transaction.domain.Ledger;
import com.eldercare.backend.billing.internal.balance.transaction.domain.TransactionType;
import com.eldercare.backend.common.document.FacilityInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 017 T038 phần dựng file (FR-025, SC-009; quickstart Q29, Q30): sao kê và báo cáo thu chi, Excel và PDF cùng số liệu. */
class BalanceExportDocumentTest {

    private static final FacilityInfo FACILITY = new FacilityInfo(
            "Viện dưỡng lão Bình An", "12 đường Hoà Bình, Hà Nội", "0101234567", "024 3999 8888", null);
    private static final LocalDate FROM = LocalDate.parse("2026-10-01");
    private static final LocalDate TO = LocalDate.parse("2026-10-31");

    private static ResidentStatement statement() {
        BalanceEntry paid =
                entry(7, Ledger.BALANCE, TransactionType.BANK_TRANSFER_PAYMENT, 2_000_000, "2026-10-05", "08:30");
        BalanceEntry deducted =
                entry(7, Ledger.BALANCE, TransactionType.STATEMENT_PAYMENT, -1_300_000, "2026-10-31", "22:00");
        BalanceEntry before = entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 1_000_000, "2026-09-20", "09:00");
        return new ResidentStatement("Nguyễn Thị Ánh", BalanceStatement.of(List.of(before, paid, deducted), FROM, TO));
    }

    @Test
    void FR_025_statementPdfAndExcelCarryTheSameFigures() {
        List<ResidentStatement> statements = List.of(statement());

        String pdf = pdfText(BalanceExportDocument.statementPdf(FACILITY, null, statements, FROM, TO));
        List<String> excel = excelCells(BalanceExportDocument.statementExcel(FACILITY, statements, FROM, TO));

        assertThat(pdf)
                .contains("Viện dưỡng lão Bình An")
                .contains("SAO KÊ SỐ DƯ")
                .contains("01/10/2026")
                .contains("31/10/2026")
                .contains("Nguyễn Thị Ánh")
                .contains("1.000.000")
                .contains("2.000.000")
                .contains("1.300.000")
                .contains("1.700.000");
        assertThat(excel).contains("1000000", "2000000", "-1300000", "1700000");
        assertThat(String.join("\n", excel)).contains("Nguyễn Thị Ánh");
    }

    @Test
    void FR_025_incomeExpensePdfAndExcelCarryTheSameFigures() {
        IncomeExpenseReport report = new IncomeExpenseReport(
                55_000_000, 48_000_000, 2_000_000, List.of(new Debtor(8, 1_500_000), new Debtor(9, 300_000)));
        Map<Long, String> names = Map.of(8L, "Lê Văn Bình", 9L, "Phạm Thị Cúc");

        String pdf = pdfText(BalanceExportDocument.incomeExpensePdf(FACILITY, null, report, names, FROM, TO));
        List<String> excel = excelCells(BalanceExportDocument.incomeExpenseExcel(FACILITY, report, names, FROM, TO));

        assertThat(pdf)
                .contains("BÁO CÁO THU CHI")
                .contains("55.000.000")
                .contains("48.000.000")
                .contains("2.000.000")
                .contains("Lê Văn Bình")
                .contains("1.500.000")
                .contains("Phạm Thị Cúc");
        assertThat(excel)
                .contains("55000000", "48000000", "2000000", "Lê Văn Bình", "1500000", "Phạm Thị Cúc", "300000");
    }
}
