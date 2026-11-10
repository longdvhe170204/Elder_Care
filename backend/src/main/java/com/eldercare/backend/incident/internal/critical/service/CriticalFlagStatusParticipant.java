package com.eldercare.backend.incident.internal.critical.service;

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

/**
 * Dòng cuối bảng trạng thái dấu nguy kịch (FR-050a; research S-13 "bean tham gia chuyển trạng thái"): người cao tuổi
 * sang Điều trị tại bệnh viện hoặc trạng thái cuối thì Hệ thống gỡ dấu đang mở trong cùng lệnh của 001.
 */
@Service
@Order(51)
public class CriticalFlagStatusParticipant implements StatusTransitionParticipant {
    private static final Set<ResidentStatus> REMOVES_FLAG = Set.of(
            ResidentStatus.HOSPITALIZED,
            ResidentStatus.RESIDENCY_ENDED,
            ResidentStatus.DECEASED,
            ResidentStatus.ADMISSION_CANCELLED);

    private final CriticalCareService critical;

    public CriticalFlagStatusParticipant(CriticalCareService critical) {
        this.critical = critical;
    }

    @Override
    public String module() {
        return "007";
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(StatusCommand.TRANSFER_OUT, StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        return List.of();
    }

    @Override
    public void applyEffects(TransitionResult result) {
        if (REMOVES_FLAG.contains(result.to())) critical.removeBySystem(result.residentId(), result.at());
    }
}
