package com.eldercare.backend.incident.internal.incident.service;

import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.ASSIGNED;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.IncidentAssigned;
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh "Giao người xử lý" sự cố (FR-045a, Q-339; research S-11 "Cách kiểm cụ thể"). Người giao: Trưởng tầng trong
 * phạm vi, Bác sĩ, Quản lý viện, hoặc người xử lý hiện tại khi Đang xử lý, Đang theo dõi. Người được giao: có quyền xử
 * lý sự cố với đối tượng ({@code ApproverApi.approvers}, luôn xét phạm vi) và vai trò Điều dưỡng, Trưởng tầng, Bác sĩ.
 */
@Service
public class IncidentAssignmentService {
    private static final String ENTITY = "M05.INCIDENT";
    private static final Set<Role> ASSIGNER_ROLES = Set.of(Role.FLOOR_MANAGER, Role.DOCTOR, Role.NURSING_HOME_MANAGER);
    /** Bác sĩ, Quản lý viện giao trong toàn viện (002 A-04). */
    private static final Set<Role> FACILITY_WIDE = Set.of(Role.DOCTOR, Role.NURSING_HOME_MANAGER);
    /** Khóa 002 cho thao tác xử lý sự cố: ô "T" của dòng "Sự cố, khẩn cấp". */
    private static final PermissionKey HANDLING = new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE);

    private final IncidentStore incidents;
    private final IncidentProgressionStore records;
    private final ScopeApi scopes;
    private final ApproverApi approvers;
    private final AccountApi accounts;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public IncidentAssignmentService(
            IncidentStore incidents,
            IncidentProgressionStore records,
            ScopeApi scopes,
            ApproverApi approvers,
            AccountApi accounts,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.incidents = incidents;
        this.records = records;
        this.scopes = scopes;
        this.approvers = approvers;
        this.accounts = accounts;
        this.audit = audit;
        this.events = events;
        this.time = time;
    }

    @Transactional
    public void assign(long incidentId, long assigneeId, String note, CurrentUser user) {
        if (note == null || note.isBlank()) throw new InvalidValueException("Giao người xử lý phải có ghi chú");
        var incident = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        var target = new Target(ENTITY, Long.toString(incidentId), incident.residentId(), incident.floorAreaId());
        Long current = incidents.assignee(incidentId).orElse(null);
        boolean handling = List.of("IN_PROGRESS", "MONITORING").contains(incident.status());
        if (!handling && !"NEW".equals(incident.status())) throw blocked("Sự cố đã kết thúc");

        boolean currentHandler = handling && current != null && current == user.accountId();
        boolean byRole = ASSIGNER_ROLES.stream().anyMatch(user.roles()::contains);
        if (!byRole && !currentHandler) throw new PermissionDeniedException("Không được giao người xử lý sự cố");
        boolean facilityWide = FACILITY_WIDE.stream().anyMatch(user.roles()::contains);
        if (!facilityWide && !scopes.inScope(user, target)) {
            throw new PermissionDeniedException("Sự cố ngoài phạm vi của người giao");
        }

        if (Objects.equals(current, assigneeId)) throw blocked("Người được giao phải khác người xử lý hiện tại");
        boolean eligible = approvers.approvers(HANDLING, target).contains(assigneeId)
                && accounts.hasRole(assigneeId, Role.NURSING_STAFF, Role.FLOOR_MANAGER, Role.DOCTOR);
        if (!eligible) {
            throw blocked("Người được giao phải là Điều dưỡng, Trưởng tầng hoặc Bác sĩ có quyền xử lý sự cố này");
        }

        var now = time.now();
        if (handling) {
            incidents.reassign(incidentId, assigneeId, incident.version());
        } else {
            incidents.assignFromNew(incidentId, assigneeId, user.accountId(), now, incident.version());
        }
        records.recordAssignment(
                incidentId, user.accountId(), assigneeId, note, incident.status(), now, incident.floorAreaId());
        audit.write(AuditLogEntry.builder()
                .action(ASSIGNED)
                .entity(ENTITY, incidentId)
                .resident(incident.residentId())
                .before(Map.of("status", incident.status(), "assignee", current == null ? "" : current))
                .after(Map.of("status", handling ? incident.status() : "IN_PROGRESS", "assignee", assigneeId))
                .build());
        events.publishEvent(new IncidentAssigned(
                incidentId,
                user.accountId(),
                assigneeId,
                incident.level().name(),
                ENTITY + ":" + incidentId + ":ASSIGNED:" + assigneeId + ":" + now.toEpochMilli(),
                now,
                notices(incident, assigneeId, user.accountId(), now)));
    }

    /** Báo người được giao theo mức hiện hành của sự cố (FR-045a). */
    private List<com.eldercare.backend.common.notification.NotificationRequest> notices(
            IncidentState incident, long assigneeId, long actorId, java.time.Instant at) {
        var staffId =
                accounts.byId(assigneeId).map(account -> account.staffId()).orElse(null);
        if (staffId == null) return List.of();
        return IncidentNotices.assigned(
                incident.id(), incident.residentId(), incident.floorAreaId(), incident.level(), staffId, actorId, at);
    }

    private static ConditionNotMetException blocked(String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition("FR-045a", message)));
    }
}
