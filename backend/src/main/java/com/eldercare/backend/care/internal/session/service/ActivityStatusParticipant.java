package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.util.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T011 (FR-022; module-interfaces 014 mục 1.1 {@code ThamGiaChuyenTrangThai}): Kết thúc lưu trú, Ghi nhận qua đời,
 * Hủy tiếp nhận hủy đăng ký chưa diễn ra, gỡ người thường xuyên. Không có điều kiện chặn. {@code @Order} sau 005 (30).
 */
@Component
@Order(31)
class ActivityStatusParticipant implements StatusTransitionParticipant {
    private final RegistrationAutoCancels autoCancels;

    ActivityStatusParticipant(RegistrationAutoCancels autoCancels) {
        this.autoCancels = autoCancels;
    }

    @Override
    public String module() {
        return "014";
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return EnumSet.of(StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH, StatusCommand.CANCEL_ADMISSION);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        return List.of();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyEffects(TransitionResult result) {
        autoCancels.cancelForFinalStatus(result.residentId(), result.at());
    }
}
