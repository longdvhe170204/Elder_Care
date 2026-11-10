package com.eldercare.backend.billing.internal.finalperiod.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.time.*;
import java.util.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** T024: sau các participant nguồn; điều kiện chi phí có thể miễn theo hồ sơ 004. */
@Component
@Order(60)
public class CostStatusParticipant implements StatusTransitionParticipant {
    private final FinalPeriodService costs;

    public CostStatusParticipant(FinalPeriodService costs) {
        this.costs = costs;
    }

    @Override
    public String module() {
        return "010";
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH, StatusCommand.CANCEL_ADMISSION);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        if (context.command() != StatusCommand.END_RESIDENCY) return List.of();
        var result = costs.finalPeriodCondition(context.residentId());
        return result.satisfied()
                ? List.of()
                : List.of(new Condition("END_A_COSTS", String.join("; ", result.reasons()), "010.FR-034a", true));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyEffects(TransitionResult result) {
        switch (result.command()) {
            case RECORD_DEATH ->
                costs.death(
                        result.residentId(),
                        result.details() != null && result.details().hasNonNull("deathAt")
                                ? Instant.parse(result.details().get("deathAt").asString())
                                : result.at());
            case CANCEL_ADMISSION ->
                costs.cancelAfter(
                        result.residentId(),
                        LocalDate.ofInstant(result.at(), BusinessTime.ZONE),
                        "ADMISSION_CANCELLED",
                        result.historyId());
            default -> {}
        }
    }
}
