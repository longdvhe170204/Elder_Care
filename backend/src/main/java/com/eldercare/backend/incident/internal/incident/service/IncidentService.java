package com.eldercare.backend.incident.internal.incident.service;

import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.LEVEL_CHANGED;
import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.RECEIVED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.CallReminderStopRequested;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lệnh vòng đời sự cố (T035); phần đầu là đổi mức FR-044/BR-M05-22. */
@Service
public class IncidentService {
    private static final Set<Role> RECEIVER_ROLES = Set.of(Role.NURSING_STAFF, Role.FLOOR_MANAGER, Role.DOCTOR);

    private final IncidentStore incidents;
    private final PermissionApi permissions;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public IncidentService(
            IncidentStore incidents,
            PermissionApi permissions,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.incidents = incidents;
        this.permissions = permissions;
        this.audit = audit;
        this.events = events;
        this.time = time;
    }

    /** Tiếp nhận xử lý: Mới → Đang xử lý, người tiếp nhận thành người xử lý, dừng leo thang (FR-044, FR-045). */
    @RequiresPermission(function = Function.INCIDENT_EMERGENCY, operation = Operation.UPDATE)
    @Transactional
    public void receive(long incidentId, CurrentUser user) {
        var incident = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        var target =
                new Target("M05.INCIDENT", Long.toString(incidentId), incident.residentId(), incident.floorAreaId());
        var permission = permissions.hasPermission(
                user, new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE), target);
        if (!permission.allowed()) {
            if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền tiếp nhận sự cố");
        }
        if (!RECEIVER_ROLES.stream().anyMatch(user.roles()::contains)) {
            throw new PermissionDeniedException("Chỉ Điều dưỡng, Trưởng tầng, Bác sĩ tiếp nhận sự cố");
        }
        if (!"NEW".equals(incident.status())) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_RECEIVED", "Sự cố đã có người tiếp nhận");
        }
        incidents.receive(incidentId, user.accountId(), time.now(), incident.version());
        audit.write(AuditLogEntry.builder()
                .action(RECEIVED)
                .entity("M05.INCIDENT", incidentId)
                .resident(incident.residentId())
                .before(Map.of("status", incident.status()))
                .after(Map.of("status", "IN_PROGRESS", "receivedBy", user.accountId()))
                .build());
    }

    @BusinessCommand
    @RequiresPermission(function = Function.INCIDENT_EMERGENCY, operation = Operation.UPDATE)
    @Transactional
    public void changeLevel(long incidentId, IncidentLevel newLevel, Reason reason, CurrentUser user) {
        var incident = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        var target =
                new Target("M05.INCIDENT", Long.toString(incidentId), incident.residentId(), incident.floorAreaId());
        var permission = permissions.hasPermission(
                user, new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE), target);
        if (!permission.allowed()) {
            if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền đổi mức sự cố");
        }
        if (List.of("CLOSED", "CANCELLED").contains(incident.status())) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-045", "Sự cố đã kết thúc")));
        }
        if (incident.level() == newLevel) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-044", "Mức mới phải khác mức hiện tại")));
        }
        boolean loweringCritical = incident.level() == IncidentLevel.CRITICAL && newLevel != IncidentLevel.CRITICAL;
        if (loweringCritical && !user.roles().contains(Role.DOCTOR)) {
            throw new PermissionDeniedException("Chỉ Bác sĩ được hạ sự cố khỏi mức Khẩn cấp");
        }

        boolean wasEmergency = incident.wasEmergency()
                || incident.level() == IncidentLevel.CRITICAL
                || newLevel == IncidentLevel.CRITICAL;
        incidents.changeLevel(incidentId, newLevel, wasEmergency, incident.version());
        audit.write(AuditLogEntry.builder()
                .action(LEVEL_CHANGED)
                .entity("M05.INCIDENT", incidentId)
                .resident(incident.residentId())
                .before(Map.of("level", incident.level().name()))
                .after(Map.of("level", newLevel.name()))
                .reason(reason)
                .build());
        if (loweringCritical) {
            events.publishEvent(new CallReminderStopRequested("M05.INCIDENT", incidentId));
        }
    }
}
