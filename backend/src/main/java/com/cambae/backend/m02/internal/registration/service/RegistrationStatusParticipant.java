package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.m01.api.dto.CommandContext;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m01.api.spi.StatusTransitionParticipant;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bean của 018 tham gia lệnh trạng thái của 001 (T014; research Z-05; plan "Đồng bộ với hợp đồng của feature khác" mục 2):
 * không có điều kiện chặn (SC-004); tác động của Kết thúc lưu trú, Ghi nhận qua đời, Hủy tiếp nhận ở {@link
 * RegistrationDeregistration#finalStatusReached}. {@code @Order(1)} cùng nhóm 004.
 */
@Component
@Order(1)
class RegistrationStatusParticipant implements StatusTransitionParticipant {

    private final RegistrationDeregistration deregistration;

    RegistrationStatusParticipant(RegistrationDeregistration deregistration) {
        this.deregistration = deregistration;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH, StatusCommand.CANCEL_ADMISSION);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        return List.of();
    }

    @Override
    public void applyEffects(TransitionResult result) {
        deregistration.finalStatusReached(result);
    }
}
