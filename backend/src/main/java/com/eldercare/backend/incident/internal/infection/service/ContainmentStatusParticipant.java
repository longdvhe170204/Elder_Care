package com.eldercare.backend.incident.internal.infection.service;

import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.dto.Warning;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * BR-M05-23, FR-065 (CHK008): Cho tạm vắng và Chuyển viện với người cao tuổi trong vùng đang khoanh vùng không bị chặn,
 * chỉ cảnh báo người thực hiện trước khi xác nhận lệnh (research S-15 "Cảnh báo không chặn").
 */
@Service
@Order(51)
public class ContainmentStatusParticipant implements StatusTransitionParticipant {
    public static final String IN_ISOLATION_ZONE = "IN_ISOLATION_ZONE";

    private final IncidentAllocationStatus containments;
    private final ResidentLocationApi locations;

    public ContainmentStatusParticipant(IncidentAllocationStatus containments, ResidentLocationApi locations) {
        this.containments = containments;
        this.locations = locations;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(StatusCommand.GRANT_TEMPORARY_ABSENCE, StatusCommand.TRANSFER_OUT);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        return List.of();
    }

    @Override
    public void applyEffects(TransitionResult result) {}

    @Override
    public List<Warning> warnings(CommandContext context) {
        var location = locations.current(context.residentId());
        if (location.isEmpty()) return List.of();
        var zone = containments.cordonedAt(context.at());
        boolean inside = (location.get().floorId() != null
                        && zone.floorIds().contains(location.get().floorId()))
                || (location.get().areaId() != null
                        && zone.areaIds().contains(location.get().areaId()));
        return inside
                ? List.of(new Warning(IN_ISOLATION_ZONE, "Người cao tuổi thuộc vùng đang khoanh vùng"))
                : List.of();
    }
}
