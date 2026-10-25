package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.api.dto.DepositStatus;
import com.cambae.backend.m02.internal.deposit.service.DepositViewService;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Khoản cần đặt cọc và trạng thái do 017 tính (contracts/api-004.yaml, schema {@code Deposit}; FR-030, FR-031). */
@Schema(name = "Deposit")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DepositBody(boolean notRequired, long requiredAmount, DepositStatus status, Basis basis) {

    /**
     * {@code receiptNumber} có trong hợp đồng api-004 nhưng {@code DepositStatusView} không mang số phiếu thu (Q-385 bỏ
     * số phiếu thu chi), nên luôn rỗng và không ghi ra; chờ chủ dự án quyết định bỏ khỏi hợp đồng.
     */
    @Schema(name = "DepositBasis")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Basis(Instant at, Long transactionId, String accountant, String receiptNumber) {}

    public static DepositBody of(DepositViewService.View view) {
        DepositViewService.Basis basis = view.basis();
        return new DepositBody(
                view.notRequired(),
                view.requiredAmount().longValueExact(),
                view.status(),
                basis == null ? null : new Basis(basis.at(), basis.transactionId(), basis.accountant(), null));
    }
}
