package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRules;
import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.stream.Stream;

/**
 * Schema {@code Settlement} của api-017: hai mã điều kiện kèm lý do, có ngoại lệ đã duyệt của 004, các phiên bản bảng
 * quyết toán.
 */
@Schema(name = "Settlement")
public record SettlementResponse(
        List<Condition> conditions, boolean hasException, List<SettlementStatementResponse> versions) {

    /** Schema {@code SettlementCondition}. */
    @Schema(name = "SettlementCondition")
    public record Condition(String code, String status, boolean waivable, List<String> reasons) {}

    public static SettlementResponse of(SettlementService.View view) {
        SettlementConditions.Result result = view.evaluation().result();
        return new SettlementResponse(
                Stream.of(result.settlement(), result.confirmation())
                        .map(c -> new Condition(
                                c.code().name(),
                                c.status().name(),
                                c.waivable(),
                                c.reasons().stream()
                                        .map(r -> SettlementRules.text(List.of(r)))
                                        .toList()))
                        .toList(),
                view.evaluation().exceptionApproved(),
                view.versions().stream().map(SettlementStatementResponse::of).toList());
    }
}
