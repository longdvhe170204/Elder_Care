package com.eldercare.backend.billing.balance.internal.document.service;

import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.date;
import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.money;

import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.Line;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.PaymentInstructions;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.TypeTotal;
import com.eldercare.backend.common.document.ExcelDocument;
import com.eldercare.backend.common.document.ExcelDocument.Column;
import com.eldercare.backend.common.document.ExcelDocument.Format;
import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.document.PdfDocument;
import java.util.List;

/**
 * Dựng phiếu báo phí PDF và Excel cùng số liệu (017 FR-032, BR-M11-21; research E-10). Hàm thuần: nhận số liệu đã chụp,
 * trả nội dung file; lưu file và nhật ký do lệnh phát hành làm.
 *
 * <p>Ảnh mã QR ({@link QrImage}) chưa chèn được vào thân PDF vì {@link PdfDocument} chưa có hàm chèn ảnh (team-plan mục
 * 8); phiếu tạm in mã nộp tiền, tài khoản, ngân hàng bằng chữ.
 */
public final class PaymentNoticeDocument {

    private static final String TITLE = "PHIẾU BÁO PHÍ";
    private static final String NO_ACCOUNT = "Liên hệ Kế toán để biết tài khoản nộp tiền";

    private PaymentNoticeDocument() {}

    /** @param logo nội dung ảnh logo của viện, {@code null} nếu không có */
    public static byte[] pdf(FacilityInfo facility, byte[] logo, PaymentNoticeData notice) {
        PdfDocument pdf = PdfDocument.a4(facility, logo, TITLE);
        pdf.paragraph("Mã phiếu: " + notice.code());
        pdf.paragraph("Ngày phát hành: " + date(notice.issuedOn()) + "    Người phát hành: " + notice.issuedBy());
        pdf.paragraph("Người cao tuổi: " + notice.residentName());
        if (notice.representativeName() != null) {
            pdf.paragraph("Người đại diện: " + notice.representativeName());
        }
        pdf.paragraph("Kỳ: " + notice.period() + "    Loại bảng: "
                + notice.statementType().label());
        pdf.table(
                List.of("Ngày", "Loại", "Mô tả", "Số lượng", "Đơn giá", "Thành tiền"),
                notice.lines().stream()
                        .map(line -> List.of(
                                date(line.incurredOn()),
                                line.costType(),
                                line.description(),
                                line.quantity().stripTrailingZeros().toPlainString(),
                                money(line.unitPrice()),
                                money(line.amount())))
                        .toList());
        pdf.table(
                List.of("Loại", "Tổng"),
                notice.totalsByType().stream()
                        .map(total -> List.of(total.costType(), money(total.total())))
                        .toList());
        pdf.paragraph("Tổng bảng: " + money(notice.total()) + " đ");
        pdf.paragraph("Số dư ngay trước khi thanh toán bảng: " + money(notice.balanceBefore()) + " đ");
        pdf.paragraph("Số dư ngay sau khi thanh toán bảng: " + money(notice.balanceAfter()) + " đ");
        if (notice.balanceAfter() < 0) {
            pdf.paragraph("Số còn nợ: " + money(-notice.balanceAfter()) + " đ");
            PaymentInstructions payment = notice.payment();
            if (payment == null) {
                pdf.paragraph(NO_ACCOUNT);
            } else {
                pdf.paragraph("Mã nộp tiền (ghi trong nội dung chuyển khoản): " + payment.paymentCode());
                pdf.paragraph("Ngân hàng: " + payment.bankName());
                pdf.paragraph("Số tài khoản: " + payment.accountNumber());
                pdf.paragraph("Chủ tài khoản: " + payment.accountHolder());
            }
        }
        pdf.paragraph("Chứng từ này không thay thế hóa đơn.");
        return pdf.toBytes();
    }

    public static byte[] excel(FacilityInfo facility, PaymentNoticeData notice) {
        ExcelDocument excel = new ExcelDocument();
        ExcelDocument.Sheet sheet = excel.sheet("Phiếu báo phí")
                .header(facility, TITLE)
                .line("Mã phiếu: " + notice.code())
                .line("Ngày phát hành: " + date(notice.issuedOn()) + "    Người phát hành: " + notice.issuedBy())
                .line("Người cao tuổi: " + notice.residentName());
        if (notice.representativeName() != null) {
            sheet.line("Người đại diện: " + notice.representativeName());
        }
        sheet.line("Kỳ: " + notice.period() + "    Loại bảng: "
                        + notice.statementType().label())
                .table(
                        List.of(
                                new Column("Ngày", Format.DATE),
                                new Column("Loại", Format.TEXT),
                                new Column("Mô tả", Format.TEXT),
                                new Column("Số lượng", Format.NUMBER),
                                new Column("Đơn giá", Format.MONEY),
                                new Column("Thành tiền", Format.MONEY)),
                        notice.lines().stream().map(PaymentNoticeDocument::row).toList())
                .table(
                        List.of(new Column("Loại", Format.TEXT), new Column("Tổng", Format.MONEY)),
                        notice.totalsByType().stream()
                                .map(PaymentNoticeDocument::row)
                                .toList())
                .table(List.of(new Column("Khoản", Format.TEXT), new Column("Số tiền", Format.MONEY)), summary(notice));
        PaymentInstructions payment = notice.payment();
        if (notice.balanceAfter() < 0) {
            if (payment == null) {
                sheet.line(NO_ACCOUNT);
            } else {
                sheet.line("Mã nộp tiền (ghi trong nội dung chuyển khoản): " + payment.paymentCode())
                        .line("Ngân hàng: " + payment.bankName())
                        .line("Số tài khoản: " + payment.accountNumber())
                        .line("Chủ tài khoản: " + payment.accountHolder());
            }
        }
        sheet.line("Chứng từ này không thay thế hóa đơn.");
        return excel.toBytes();
    }

    private static List<Object> row(Line line) {
        return List.of(
                line.incurredOn(),
                line.costType(),
                line.description(),
                line.quantity(),
                line.unitPrice(),
                line.amount());
    }

    private static List<Object> row(TypeTotal total) {
        return List.of(total.costType(), total.total());
    }

    private static List<List<Object>> summary(PaymentNoticeData notice) {
        return List.of(
                List.of("Tổng bảng", notice.total()),
                List.of("Số dư trước thanh toán bảng", notice.balanceBefore()),
                List.of("Số dư sau thanh toán bảng", notice.balanceAfter()));
    }
}
