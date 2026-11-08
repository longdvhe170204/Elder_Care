package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.incident.api.spi.NotificationLogPort;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.IncidentDetails;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc sự cố cho màn hình (FR-045, FR-048b, FR-080): danh sách theo phạm vi quyền "Sự cố, khẩn cấp", chi tiết kèm diễn
 * biến, người xử lý, người được thông báo (009) và lệnh được phép theo bảng FR-045. Ngoài phạm vi trả 404 như lệnh.
 */
@Service
public class IncidentQueryService {
    private static final String ENTITY = "M05.INCIDENT";
    private static final PermissionKey VIEW = new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.VIEW);
    private static final PermissionKey UPDATE = new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE);
    private static final PermissionKey CREATE = new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE);
    private static final Set<String> OPEN = Set.of("NEW", "IN_PROGRESS", "MONITORING");
    private static final Set<Role> HANDLER_ROLES = Set.of(Role.NURSING_STAFF, Role.FLOOR_MANAGER, Role.DOCTOR);
    private static final Set<Role> SERIOUS_CLOSER_ROLES = Set.of(Role.NURSING_STAFF, Role.DOCTOR);
    private static final Set<Role> ASSIGNER_ROLES = Set.of(Role.FLOOR_MANAGER, Role.DOCTOR, Role.NURSING_HOME_MANAGER);

    private final IncidentStore incidents;
    private final IncidentProgressionStore progressions;
    private final PermissionApi permissions;
    private final ScopeApi scopes;
    private final AccountApi accounts;
    private final Optional<NotificationLogPort> notifications;

    public IncidentQueryService(
            IncidentStore incidents,
            IncidentProgressionStore progressions,
            PermissionApi permissions,
            ScopeApi scopes,
            AccountApi accounts,
            @Autowired(required = false) NotificationLogPort notifications) {
        this.incidents = incidents;
        this.progressions = progressions;
        this.permissions = permissions;
        this.scopes = scopes;
        this.accounts = accounts;
        this.notifications = Optional.ofNullable(notifications);
    }

    @Transactional(readOnly = true)
    public List<IncidentDetails> list(
            Collection<String> statuses, Long residentId, Long floorAreaId, CurrentUser user) {
        var scope = scopes.filter(user, VIEW);
        return incidents
                .search(
                        statuses,
                        residentId,
                        floorAreaId,
                        scope.facilityWide(),
                        scope.floorAreaIds(),
                        scope.residentIds())
                .stream()
                .map(incident -> new IncidentDetails(
                        incident, assigneeName(incident), List.of(), List.of(), List.of(), List.of(), Set.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentDetails detail(long incidentId, CurrentUser user) {
        var incident = incidents.view(incidentId).orElseThrow(NotFoundException::new);
        var view = permissions.hasPermission(user, VIEW, target(incident));
        if (!view.allowed()) {
            if (view.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xem sự cố");
        }
        var handlers = progressions.handlerAccountIds(incidentId).stream()
                .map(id -> accounts.fullName(id).orElse(Long.toString(id)))
                .toList();
        var notified =
                notifications.map(port -> port.bySource(ENTITY, incidentId)).orElse(List.of());
        return new IncidentDetails(
                incident,
                assigneeName(incident),
                progressions.list(incidentId),
                progressions.levelChanges(incidentId),
                handlers,
                notified,
                allowedCommands(incident, user));
    }

    /** Tên lệnh trong đường dẫn (glossary {@code allowedCommands}); chỉ lệnh đã có route. */
    private Set<String> allowedCommands(IncidentView incident, CurrentUser user) {
        var commands = new LinkedHashSet<String>();
        if (!OPEN.contains(incident.status())) return commands;
        var roles = user.roles();
        boolean manager = roles.contains(Role.NURSING_HOME_MANAGER);
        var target = target(incident);
        boolean handler = !manager
                && HANDLER_ROLES.stream().anyMatch(roles::contains)
                && permissions.hasPermission(user, UPDATE, target).allowed();
        boolean current = Objects.equals(incident.assigneeId(), user.accountId());
        String status = incident.status();
        if (status.equals("NEW") && handler) commands.add("receive");
        boolean assigner = ASSIGNER_ROLES.stream().anyMatch(roles::contains) || current;
        if (assigner && (manager || handler)) commands.add("assign-handler");
        if (handler || incident.recordedBy() == user.accountId()) commands.add("progressions");
        if (handler) commands.add("change-level");
        if (status.equals("IN_PROGRESS") && handler) commands.add("move-to-monitoring");
        if (status.equals("MONITORING") && handler) commands.add("reopen");
        var closers = incident.level() == IncidentLevel.LOW ? HANDLER_ROLES : SERIOUS_CLOSER_ROLES;
        if (!status.equals("NEW") && handler && closers.stream().anyMatch(roles::contains)) commands.add("close");
        if (incident.level() == IncidentLevel.CRITICAL
                && !manager
                && permissions.hasPermission(user, CREATE, target).allowed()) {
            commands.add("join-handling");
        }
        return commands;
    }

    private String assigneeName(IncidentView incident) {
        return incident.assigneeId() == null
                ? null
                : accounts.fullName(incident.assigneeId()).orElse(null);
    }

    private static Target target(IncidentView incident) {
        return new Target(ENTITY, Long.toString(incident.id()), incident.residentId(), incident.floorAreaId());
    }
}
