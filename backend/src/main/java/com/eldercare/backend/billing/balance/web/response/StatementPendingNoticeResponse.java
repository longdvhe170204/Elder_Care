package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.billing.balance.internal.document.service.PaymentNoticeIssuance;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Schema {@code StatementPendingNotice} của api-017: bảng Đã chốt chưa phát hành phiếu. */
@Schema(name = "StatementPendingNotice")
public record StatementPendingNoticeResponse(
        long costStatementId,
        long residentId,
        String fullName,
        String period,
        String statementType,
        long total,
        Instant finalizedAt) {

    public static StatementPendingNoticeResponse of(PaymentNoticeIssuance.Pending p) {
        return new StatementPendingNoticeResponse(
                p.costStatementId(),
                p.residentId(),
                p.fullName(),
                p.period(),
                p.statementType(),
                p.total(),
                p.finalizedAt());
    }
}
