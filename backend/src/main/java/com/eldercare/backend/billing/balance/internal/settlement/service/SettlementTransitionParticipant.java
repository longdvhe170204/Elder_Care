package com.eldercare.backend.billing.balance.internal.settlement.service;

import com.eldercare.backend.billing.api.dto.SettlementConditionCode;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRules;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bean {@code ThamGiaChuyenTrangThai} của 017 (T031; FR-022, BR-M11-22; module-interfaces mục 2.1; 004 T-09): lệnh Kết
 * thúc lưu trú có hai mã dòng (h). {@code END_H_SETTLEMENT} (ba ý tiền) miễn được bằng ngoại lệ đã duyệt của 004;
 * {@code END_H_SETTLEMENT_CONFIRMATION} (không còn Chờ duyệt, đã xác nhận quyết toán, số liệu không đổi) không miễn được.
 * Thứ tự sau 013. Không có tác động.
 */
@Component
@Order(90)
class SettlementTransitionParticipant implements StatusTransitionParticipant {

    private static final String BASIS = "FR-022, BR-M11-22";

    private final SettlementService settlement;

    SettlementTransitionParticipant(SettlementService settlement) {
        this.settlement = settlement;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(StatusCommand.END_RESIDENCY);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        if (context.command() != StatusCommand.END_RESIDENCY) return List.of();
        SettlementConditions.Result result =
                settlement.evaluate(context.residentId()).result();
        return Stream.of(result.settlement(), result.confirmation())
                .filter(c -> c.status() == SettlementConditions.Status.NOT_MET)
                .map(c -> new Condition(
                        SettlementConditionCode.valueOf(c.code().name()).name(),
                        SettlementRules.text(c.reasons()),
                        BASIS,
                        c.waivable()))
                .toList();
    }

    @Override
    public void applyEffects(TransitionResult result) {}
}
