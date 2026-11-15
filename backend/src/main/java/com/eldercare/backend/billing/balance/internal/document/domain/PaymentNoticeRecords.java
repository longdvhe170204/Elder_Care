package com.eldercare.backend.billing.balance.internal.document.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Phiếu báo phí trên bảng chung {@code record_event} (017 data-model mục 5, 0.1; DBR-37; research E-10). Dòng gắn sổ
 * {@code M11.BALANCE} của người cao tuổi (lưu giữ theo sổ, FR-029), khóa duy nhất {@code
 * M11.PAYMENT_NOTICE:<bảng chi phí>} nên mỗi bảng tối đa một phiếu. Mã loại cũng là loại chủ sở hữu tệp.
 */
public final class PaymentNoticeRecords {

    public static final SharedKind<PaymentNoticeRecord> KIND =
            SharedKind.of("M11.PAYMENT_NOTICE", PaymentNoticeRecord.class);

    private PaymentNoticeRecords() {}

    public static String key(long costStatementId) {
        return KIND.code() + ":" + costStatementId;
    }

    /** Mã phiếu {@code PBP-<yyyyMM của kỳ>-<bảng>}. */
    public static String code(YearMonth period, long costStatementId) {
        return "PBP-" + period.format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + costStatementId;
    }

    /** Số liệu cố định của phiếu đã phát hành; hai tệp (PDF, Excel) lưu qua {@code FileApi}. */
    public record PaymentNoticeRecord(
            String code,
            long costStatementId,
            String period,
            StatementType statementType,
            long total,
            long balanceBefore,
            long balanceAfter,
            String issuedBy,
            long pdfFileId,
            long xlsxFileId) {}
}
