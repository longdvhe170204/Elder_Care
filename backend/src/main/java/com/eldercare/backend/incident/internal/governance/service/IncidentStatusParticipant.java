package com.eldercare.backend.incident.internal.governance.service;

import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
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
 * Bean tham gia lệnh trạng thái của {@code incident} (research S-17; {@code @Order} sau {@code health}). Kết thúc lưu
 * trú: điều kiện {@code END_D_ALERTS_INCIDENTS} miễn được khi còn cảnh báo đang mở hoặc sự cố Mới, Đang xử lý, Đang theo
 * dõi (004 FR-063 (d), CHK061). Trạng thái cuối: cảnh báo, sự cố đang mở dừng leo thang và nhắc, trạng thái giữ để người
 * có quyền đóng (FR-038); khoảng theo dõi sau ngã Đã hết; danh sách tiếp xúc giữ. Gỡ dấu nguy kịch ở
 * {@code CriticalFlagStatusParticipant}, cảnh báo vùng khoanh ở {@code ContainmentStatusParticipant}.
 */
@Service
@Order(51)
public class IncidentStatusParticipant implements StatusTransitionParticipant {
    public static final String END_D_ALERTS_INCIDENTS = "END_D_ALERTS_INCIDENTS";

    private static final Set<ResidentStatus> FINAL =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final AlertStore alerts;
    private final IncidentStore incidents;
    private final FallMonitoringStore fallWindows;
    private final MonitoringLock lock;

    public IncidentStatusParticipant(
            AlertStore alerts, IncidentStore incidents, FallMonitoringStore fallWindows, MonitoringLock lock) {
        this.alerts = alerts;
        this.incidents = incidents;
        this.fallWindows = fallWindows;
        this.lock = lock;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH, StatusCommand.CANCEL_ADMISSION);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        if (context.command() != StatusCommand.END_RESIDENCY) return List.of();
        long residentId = context.residentId();
        if (!alerts.hasOpenAlert(residentId) && !incidents.hasOpen(residentId)) return List.of();
        return List.of(new Condition(
                END_D_ALERTS_INCIDENTS, "Còn cảnh báo hoặc sự cố đang mở", "004 FR-063 (d), CHK061", true));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyEffects(TransitionResult result) {
        if (!FINAL.contains(result.to())) return;
        long residentId = result.residentId();
        lock.lock(residentId);
        alerts.stopEscalation(residentId);
        incidents.stopEscalation(residentId);
        fallWindows
                .lockActive(residentId)
                .ifPresent(window -> fallWindows.finish(
                        window, FallMonitoringStore.EXPIRED, window.content().ended(null, null)));
    }
}
