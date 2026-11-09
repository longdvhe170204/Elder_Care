package com.eldercare.backend.billing.balance.internal.export.service;

import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.date;
import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.dateTime;
import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.money;

import com.eldercare.backend.billing.balance.internal.export.domain.BalanceStatement;
import com.eldercare.backend.billing.balance.internal.export.domain.IncomeExpenseReport;
import com.eldercare.backend.billing.balance.internal.export.domain.IncomeExpenseReport.Debtor;
import com.eldercare.backend.common.document.ExcelDocument;
import com.eldercare.backend.common.document.ExcelDocument.Column;
import com.eldercare.backend.common.document.ExcelDocument.Format;
import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.document.PdfDocument;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Dựng sao kê và báo cáo thu chi dạng Excel và PDF cùng số liệu (017 FR-025, SC-009; research E-11). Hàm thuần: nhận số
 * liệu đã tính ({@link BalanceStatement}, {@link IncomeExpenseReport}), trả nội dung file; lưu file, ghi lần xuất do lệnh
 * xuất làm.
 */
public final class BalanceExportDocument {

    private static final String STATEMENT_TITLE = "SAO KÊ SỐ DƯ";
    private static final String REPORT_TITLE = "BÁO CÁO THU CHI";

    /** Sao kê của một người cao tuổi kèm họ tên để in. */
    public record ResidentStatement(String residentName, BalanceStatement statement) {}

    private BalanceExportDocument() {}

    public static byte[] statementPdf(
            FacilityInfo facility, byte[] logo, List<ResidentStatement> statements, LocalDate from, LocalDate to) {
        PdfDocument pdf = PdfDocument.a4(facility, logo, STATEMENT_TITLE);
        pdf.paragraph("Từ ngày " + date(from) + " đến ngày " + date(to));
        for (ResidentStatement item : statements) {
            BalanceStatement statement = item.statement();
            pdf.paragraph("Người cao tuổi: " + item.residentName());
            pdf.paragraph("Số dư đầu kỳ: " + money(statement.openingBalance()) + " đ    Số dư cọc đầu kỳ: "
                    + money(statement.openingDeposit()) + " đ");
            pdf.table(
                    List.of("Thời điểm", "Sổ", "Loại", "Số tiền", "Phương thức", "Tham chiếu"),
                    statement.lines().stream()
                            .map(line -> List.of(
                                    dateTime(line.at()),
                                    line.ledger().name(),
                                    line.type().name(),
                                    money(line.amount()),
                                    line.method().name(),
                                    line.reference() == null ? "" : line.reference()))
                            .toList());
            pdf.paragraph("Số dư cuối kỳ: " + money(statement.closingBalance()) + " đ    Số dư cọc cuối kỳ: "
                    + money(statement.closingDeposit()) + " đ");
        }
        return pdf.toBytes();
    }

    public static byte[] statementExcel(
            FacilityInfo facility, List<ResidentStatement> statements, LocalDate from, LocalDate to) {
        ExcelDocument excel = new ExcelDocument();
        ExcelDocument.Sheet sheet = excel.sheet("Sao kê")
                .header(facility, STATEMENT_TITLE)
                .line("Từ ngày " + date(from) + " đến ngày " + date(to));
        for (ResidentStatement item : statements) {
            BalanceStatement statement = item.statement();
            List<List<Object>> rows = new ArrayList<>();
            for (var line : statement.lines()) {
                rows.add(Arrays.asList(
                        LocalDateTime.ofInstant(line.at(), BusinessTime.ZONE),
                        line.ledger().name(),
                        line.type().name(),
                        line.amount(),
                        line.method().name(),
                        line.reference()));
            }
            sheet.line("Người cao tuổi: " + item.residentName())
                    .table(
                            List.of(new Column("Khoản", Format.TEXT), new Column("Số tiền", Format.MONEY)),
                            List.of(
                                    List.of("Số dư đầu kỳ", statement.openingBalance()),
                                    List.of("Số dư cọc đầu kỳ", statement.openingDeposit())))
                    .table(
                            List.of(
                                    new Column("Thời điểm", Format.DATE_TIME),
                                    new Column("Sổ", Format.TEXT),
                                    new Column("Loại", Format.TEXT),
                                    new Column("Số tiền", Format.MONEY),
                                    new Column("Phương thức", Format.TEXT),
                                    new Column("Tham chiếu", Format.TEXT)),
                            rows)
                    .table(
                            List.of(new Column("Khoản", Format.TEXT), new Column("Số tiền", Format.MONEY)),
                            List.of(
                                    List.of("Số dư cuối kỳ", statement.closingBalance()),
                                    List.of("Số dư cọc cuối kỳ", statement.closingDeposit())));
        }
        return excel.toBytes();
    }

    public static byte[] incomeExpensePdf(
            FacilityInfo facility,
            byte[] logo,
            IncomeExpenseReport report,
            Map<Long, String> residentNames,
            LocalDate from,
            LocalDate to) {
        PdfDocument pdf = PdfDocument.a4(facility, logo, REPORT_TITLE);
        pdf.paragraph("Từ ngày " + date(from) + " đến ngày " + date(to));
        pdf.paragraph("Tổng tiền gia đình nộp: " + money(report.totalPaid()) + " đ");
        pdf.paragraph("Tổng đã trừ theo bảng chi phí: " + money(report.totalDeducted()) + " đ");
        pdf.paragraph("Tổng tiền hoàn: " + money(report.totalRefunded()) + " đ");
        pdf.table(
                List.of("Người cao tuổi còn nợ", "Số còn nợ"),
                report.debtors().stream()
                        .map(debtor -> List.of(name(residentNames, debtor), money(debtor.debt())))
                        .toList());
        return pdf.toBytes();
    }

    public static byte[] incomeExpenseExcel(
            FacilityInfo facility,
            IncomeExpenseReport report,
            Map<Long, String> residentNames,
            LocalDate from,
            LocalDate to) {
        ExcelDocument excel = new ExcelDocument();
        excel.sheet("Thu chi")
                .header(facility, REPORT_TITLE)
                .line("Từ ngày " + date(from) + " đến ngày " + date(to))
                .table(
                        List.of(new Column("Khoản", Format.TEXT), new Column("Số tiền", Format.MONEY)),
                        List.of(
                                List.of("Tổng tiền gia đình nộp", report.totalPaid()),
                                List.of("Tổng đã trừ theo bảng chi phí", report.totalDeducted()),
                                List.of("Tổng tiền hoàn", report.totalRefunded())))
                .table(
                        List.of(
                                new Column("Người cao tuổi còn nợ", Format.TEXT),
                                new Column("Số còn nợ", Format.MONEY)),
                        report.debtors().stream()
                                .map(debtor -> List.<Object>of(name(residentNames, debtor), debtor.debt()))
                                .toList());
        return excel.toBytes();
    }

    private static String name(Map<Long, String> residentNames, Debtor debtor) {
        return residentNames.getOrDefault(debtor.residentId(), "Mã hồ sơ " + debtor.residentId());
    }
}
