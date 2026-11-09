package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionView;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Mô tả schema {@code DuplicateCodeProblem} của api-017 (Problem kèm {@code duplicateTransactions}) cho OpenAPI; thân lỗi
 * thật do {@code ProblemDetailsHandler} dựng từ thuộc tính của lỗi {@code BANK_TRANSACTION_CODE_USED}.
 */
@Schema(name = "DuplicateCodeProblem")
public record DuplicateCodeProblemBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri")
        String type,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer status,
        String detail,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String errorCode,
        List<com.eldercare.backend.common.error.UnmetCondition> unmetConditions,
        Object currentValue,
        List<Object> candidates,

        @Schema(allowableValues = {"ROLE", "SCOPE", "LEGAL", "SOURCE_DATA"})
        String failedLayer,

        ProblemBody.License license,
        java.time.Instant unlocksAt,
        List<BalanceTransactionView> duplicateTransactions) {}
