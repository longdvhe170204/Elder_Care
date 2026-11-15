package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.billing.balance.internal.document.service.PaymentNoticeService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Schema {@code PaymentNotice} của api-017: một phiếu báo phí đã phát hành. */
@Schema(name = "PaymentNotice")
public record PaymentNoticeResponse(
        long id,
        String noticeCode,
        long costStatementId,
        String period,
        String statementType,
        long total,
        long balanceBefore,
        long balanceAfter,
        String issuedBy,
        Instant issuedAt) {

    public static PaymentNoticeResponse of(PaymentNoticeService.Notice n) {
        return new PaymentNoticeResponse(
                n.id(),
                n.noticeCode(),
                n.costStatementId(),
                n.period(),
                n.statementType(),
                n.total(),
                n.balanceBefore(),
                n.balanceAfter(),
                n.issuedBy(),
                n.issuedAt());
    }
}
