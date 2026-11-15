package com.eldercare.backend.billing.balance.internal.settlement.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Bảng quyết toán và bản ký trên bảng chung {@code record_event} (017 data-model mục 0, 0.1, DBR-38; R-15). Dòng gắn sổ
 * {@code M11.BALANCE} của người cao tuổi (lưu giữ theo sổ, FR-029). Mỗi phiên bản là một dòng đã xác nhận, khóa duy nhất
 * {@code M11.SETTLEMENT_STATEMENT:<người cao tuổi>:<phiên bản>}; phiên bản hiện hành là phiên bản lớn nhất ({@code
 * record_event} chỉ thêm, không đổi cờ hiện hành). Bản ký là dòng riêng, khóa {@code M11.SETTLEMENT_SIGNED_COPY:<bảng>}
 * (một lần mỗi phiên bản). Mã loại cũng là loại chủ sở hữu tệp.
 */
public final class SettlementRecords {

    public static final SharedKind<SettlementStatementRecord> STATEMENT =
            SharedKind.of("M11.SETTLEMENT_STATEMENT", SettlementStatementRecord.class);

    public static final SharedKind<SignedCopyRecord> SIGNED_COPY =
            SharedKind.of("M11.SETTLEMENT_SIGNED_COPY", SignedCopyRecord.class);

    private SettlementRecords() {}

    public static String statementKey(long residentId, int version) {
        return STATEMENT.code() + ":" + residentId + ":" + version;
    }

    public static String signedCopyKey(long statementId) {
        return SIGNED_COPY.code() + ":" + statementId;
    }
}
