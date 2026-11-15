package com.eldercare.backend.health.internal.governance.service;

import com.eldercare.backend.health.internal.schedule.service.MeasurementScheduleService;
import com.eldercare.backend.health.internal.threshold.service.ThresholdFinalStatus;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bean tham gia lệnh trạng thái của {@code health} (research S-17; {@code @Order} sau 006, trước {@code incident}):
 * Kết thúc lưu trú, Ghi nhận qua đời, Hủy tiếp nhận thì lịch đo đang mở Đã ngừng (FR-006) và ngưỡng cá nhân hết hiệu lực
 * hoặc bị hủy (FR-022) trong cùng lệnh của 001. Không có điều kiện chặn.
 */
@Service
@Order(50)
public class HealthStatusParticipant implements StatusTransitionParticipant {
    private static final Set<ResidentStatus> FINAL =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final MeasurementScheduleService schedules;
    private final ThresholdFinalStatus thresholds;

    public HealthStatusParticipant(MeasurementScheduleService schedules, ThresholdFinalStatus thresholds) {
        this.schedules = schedules;
        this.thresholds = thresholds;
    }

    @Override
    public String module() {
        return "007";
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
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyEffects(TransitionResult result) {
        if (!FINAL.contains(result.to())) return;
        schedules.stopForFinalStatus(result.residentId(), result.at());
        thresholds.end(result.residentId(), result.at());
    }
}
