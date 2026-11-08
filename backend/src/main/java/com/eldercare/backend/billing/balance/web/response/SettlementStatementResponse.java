package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Schema {@code SettlementStatement} của api-017: một phiên bản bảng quyết toán. */
@Schema(name = "SettlementStatement")
public record SettlementStatementResponse(
        long id,
        int version,
        boolean current,
        boolean hasException,
        long debtOrRefundAmount,
        String confirmedBy,
        Instant confirmedAt,
        boolean hasSignedCopy) {

    public static SettlementStatementResponse of(SettlementService.Statement s) {
        return new SettlementStatementResponse(
                s.id(),
                s.version(),
                s.current(),
                s.hasException(),
                s.debtOrRefundAmount(),
                s.confirmedBy(),
                s.confirmedAt(),
                s.hasSignedCopy());
    }
}
