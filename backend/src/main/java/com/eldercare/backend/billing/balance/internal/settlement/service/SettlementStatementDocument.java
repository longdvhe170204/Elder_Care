package com.eldercare.backend.billing.balance.internal.settlement.service;

import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.date;
import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.dateTime;
import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.money;

import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementStatementData;
import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.document.PdfDocument;
import java.util.List;

/**
 * Dựng bảng quyết toán PDF (017 FR-034, BR-M11-22, DBR-38; research E-09). Hàm thuần: nhận số liệu chụp lúc xác nhận,
 * trả nội dung file; phiên bản, lưu file, nhật ký do lệnh Xác nhận quyết toán làm.
 */
public final class SettlementStatementDocument {

    private SettlementStatementDocument() {}

    /** @param logo nội dung ảnh logo của viện, {@code null} nếu không có */
    public static byte[] pdf(FacilityInfo facility, byte[] logo, SettlementStatementData data) {
        PdfDocument pdf = PdfDocument.a4(facility, logo, "BẢNG QUYẾT TOÁN");
        pdf.paragraph("Phiên bản " + data.version());
        pdf.paragraph("Người cao tuổi: " + data.residentName());
        if (data.representativeName() != null) {
            pdf.paragraph("Người đại diện: " + data.representativeName());
        }
        pdf.paragraph("Loại lưu trú: " + orDash(data.stayType()) + "    Hợp đồng: " + orDash(data.contractCode()));
        // hồ sơ chưa có hợp đồng đã ký, hoặc xác nhận lại sau trạng thái cuối: 004 không trả ngày
        pdf.paragraph("Ngày bắt đầu: " + (data.startDate() == null ? "—" : date(data.startDate()))
                + "    Ngày kết thúc dự kiến: "
                + (data.expectedEndDate() == null ? "—" : date(data.expectedEndDate())));
        pdf.paragraph("Trường hợp kết thúc: " + orDash(data.endCase()));
        pdf.table(
                List.of("Bảng kỳ cuối, theo loại", "Tổng"),
                data.finalPeriodTotalsByType().stream()
                        .map(total -> List.of(total.costType(), money(total.total())))
                        .toList());
        pdf.paragraph("Tổng bảng kỳ cuối: " + money(data.finalPeriodTotal()) + " đ");
        pdf.paragraph("Số dư trước quyết toán: " + money(data.balanceBeforeSettlement()) + " đ");
        pdf.paragraph("Tiền cọc đã thu: " + money(data.depositReceived()) + " đ    đã cấn trừ: "
                + money(data.depositOffset()) + " đ    đã hoàn: " + money(data.depositRefunded()) + " đ");
        pdf.table(
                List.of("Thời điểm", "Loại giao dịch", "Số tiền"),
                data.transactions().stream()
                        .map(entry -> List.of(dateTime(entry.at()), entry.type().name(), money(entry.amount())))
                        .toList());
        if (data.remaining() < 0) {
            pdf.paragraph("Còn nợ: " + money(-data.remaining()) + " đ");
        } else if (data.remaining() > 0) {
            pdf.paragraph("Còn phải hoàn: " + money(data.remaining()) + " đ");
        }
        if (data.hasException()) {
            pdf.paragraph("Quyết toán theo ngoại lệ đã được duyệt.");
        }
        pdf.paragraph("Người xác nhận: " + data.confirmedBy() + "    Thời điểm: " + dateTime(data.confirmedAt()));
        pdf.paragraph("Chứng từ này không thay thế hóa đơn.");
        pdf.signatures("Kế toán", "Người đại diện");
        return pdf.toBytes();
    }

    private static String orDash(String text) {
        return text == null ? "—" : text;
    }
}
