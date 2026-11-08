package com.eldercare.backend.billing.balance.internal.export.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.LocalDate;
import java.util.List;

/**
 * Lần xuất sao kê, báo cáo thu chi trên bảng chung {@code record_event} (017 data-model mục 0.1 "lan_xuat_so_du"; FR-027;
 * research E-11). Chỉ thêm, file lưu một lần nên tải lại luôn đúng file đã dựng. Dòng gắn người xuất (loại cha {@code
 * M11.BALANCE_EXPORT_BY}); chủ sở hữu tệp là chính dòng này. Mã loại cũng là loại chủ sở hữu tệp.
 */
public final class BalanceExportRecords {

    public static final SharedKind<BalanceExportRecord> KIND =
            SharedKind.of("M11.BALANCE_EXPORT", BalanceExportRecord.class);
    public static final String PARENT_TYPE = "M11.BALANCE_EXPORT_BY";

    private BalanceExportRecords() {}

    public enum Type {
        BALANCE_STATEMENT,
        INCOME_EXPENSE
    }

    public record BalanceExportRecord(
            Type type,
            ExportFormat format,
            LocalDate from,
            LocalDate to,
            List<Long> residentIds,
            int rowCount,
            String role,
            long fileId) {}
}
