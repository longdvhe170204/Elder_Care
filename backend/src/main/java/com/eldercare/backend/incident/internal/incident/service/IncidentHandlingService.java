package com.eldercare.backend.incident.internal.incident.service;

import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.CLOSED;
import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.JOINED;
import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.MOVED_TO_MONITORING;
import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.REOPENED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.InfectionSuspicionChanged;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionGroup;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionTypeView;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Các lệnh xử lý sự cố theo bảng FR-045: chuyển theo dõi, mở lại, đóng (BR-M05-09, FR-066), tham gia xử lý sự cố
 * Khẩn cấp (FR-041). Quản lý viện ghi nhận được nhưng mọi lệnh xử lý trả 403 (research S-11, Q-71).
 */
@Service
public class IncidentHandlingService {
    private static final String ENTITY = "M05.INCIDENT";
    private static final String SUSPECTED_INFECTION = "SUSPECTED_INFECTION";
    private static final Set<String> OPEN = Set.of("NEW", "IN_PROGRESS", "MONITORING");
    private static final Set<String> INFECTION_OUTCOMES = Set.of("CONFIRMED_INFECTION", "RULED_OUT", "UNDETERMINED");
    private static final Set<Role> HANDLER_ROLES = Set.of(Role.NURSING_STAFF, Role.FLOOR_MANAGER, Role.DOCTOR);
    /** BR-M05-09: sự cố Trung bình, Khẩn cấp chỉ Điều dưỡng hoặc Bác sĩ đóng. */
    private static final Set<Role> SERIOUS_CLOSER_ROLES = Set.of(Role.NURSING_STAFF, Role.DOCTOR);

    private final IncidentStore incidents;
    private final IncidentProgressionStore progressions;
    private final IncidentCatalogService catalog;
    private final ContainmentStore containments;
    private final PermissionApi permissions;
    private final AuditLogApi audit;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final BusinessTime time;

    public IncidentHandlingService(
            IncidentStore incidents,
            IncidentProgressionStore progressions,
            IncidentCatalogService catalog,
            ContainmentStore containments,
            PermissionApi permissions,
            AuditLogApi audit,
            org.springframework.context.ApplicationEventPublisher events,
            BusinessTime time) {
        this.incidents = incidents;
        this.progressions = progressions;
        this.catalog = catalog;
        this.containments = containments;
        this.permissions = permissions;
        this.audit = audit;
        this.events = events;
        this.time = time;
    }

    /** Đang xử lý → Đang theo dõi; cần ít nhất một diễn biến đánh giá và một diễn biến xử lý. */
    @RequiresPermission(function = Function.INCIDENT_EMERGENCY, operation = Operation.UPDATE)
    @Transactional
    public void moveToMonitoring(long incidentId, CurrentUser user) {
        var incident = lockForHandling(incidentId, user, HANDLER_ROLES);
        requireStatus(incident, "IN_PROGRESS");
        Map<Long, ProgressionGroup> groups = catalog.progressionTypes().stream()
                .collect(Collectors.toMap(ProgressionTypeView::id, ProgressionTypeView::group));
        var recorded =
                progressions.typeIds(incidentId).stream().map(groups::get).collect(Collectors.toSet());
        if (!recorded.contains(ProgressionGroup.EVALUATION) || !recorded.contains(ProgressionGroup.ACTION)) {
            throw blocked("Cần ít nhất một diễn biến đánh giá và một diễn biến xử lý");
        }
        incidents.changeStatus(incidentId, "IN_PROGRESS", "MONITORING", incident.version());
        audit(MOVED_TO_MONITORING, incident, "MONITORING", null);
    }

    /** Đang theo dõi → Đang xử lý, bắt buộc lý do. */
    @BusinessCommand
    @RequiresPermission(function = Function.INCIDENT_EMERGENCY, operation = Operation.UPDATE)
    @Transactional
    public void reopen(long incidentId, Reason reason, CurrentUser user) {
        var incident = lockForHandling(incidentId, user, HANDLER_ROLES);
        requireStatus(incident, "MONITORING");
        incidents.changeStatus(incidentId, "MONITORING", "IN_PROGRESS", incident.version());
        audit(REOPENED, incident, "IN_PROGRESS", reason);
    }

    /** Đang xử lý, Đang theo dõi → Đã đóng (BR-M05-09; sự cố lây nhiễm theo FR-066). */
    @RequiresPermission(function = Function.INCIDENT_EMERGENCY, operation = Operation.UPDATE)
    @Transactional
    public void close(long incidentId, String resolution, String infectionOutcome, CurrentUser user) {
        var incident = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        var closers = incident.level() == IncidentLevel.LOW ? HANDLER_ROLES : SERIOUS_CLOSER_ROLES;
        requireHandler(incident, user, closers);
        if (resolution == null || resolution.isBlank()) throw new InvalidValueException("Thiếu kết quả xử lý");
        requireStatus(incident, "IN_PROGRESS", "MONITORING");
        boolean infection = SUSPECTED_INFECTION.equals(incidents.currentTypeCode(incidentId));
        String outcome = null;
        if (infection) {
            if (infectionOutcome == null || !INFECTION_OUTCOMES.contains(infectionOutcome)) {
                throw new InvalidValueException(
                        "Sự cố lây nhiễm phải chọn kết quả: xác nhận nhiễm, loại trừ, không xác định");
            }
            if (containments.activeOnlyFor(incidentId)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "ISOLATION_ONLY_FOR_THIS_INCIDENT",
                        "Còn vùng khoanh chỉ gắn với sự cố này: gỡ vùng hoặc gắn với sự cố lây nhiễm khác trước");
            }
            outcome = infectionOutcome;
        }
        var now = time.now();
        incidents.close(incidentId, resolution, outcome, user.accountId(), now, incident.version());
        audit(CLOSED, incident, "CLOSED", null);
        if (infection && incident.residentId() != null) {
            events.publishEvent(new InfectionSuspicionChanged(incident.residentId(), incidentId, false, now));
        }
    }

    /** Tham gia xử lý sự cố Khẩn cấp đang mở; không tạo sự cố mới (FR-041, FR-048b). */
    @RequiresPermission(function = Function.INCIDENT_EMERGENCY, operation = Operation.CREATE)
    @Transactional
    public void join(long incidentId, CurrentUser user) {
        if (user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Quản lý viện không tham gia xử lý sự cố");
        }
        var incident = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        var permission = permissions.hasPermission(
                user, new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE), target(incident));
        if (!permission.allowed()) throw new PermissionDeniedException("Không có quyền tham gia xử lý sự cố");
        if (incident.level() != IncidentLevel.CRITICAL || !OPEN.contains(incident.status())) {
            throw blocked("Chỉ tham gia được sự cố Khẩn cấp đang mở");
        }
        progressions.join(incidentId, user.accountId(), time.now(), incident.floorAreaId());
        audit(JOINED, incident, incident.status(), null);
    }

    private IncidentState lockForHandling(long incidentId, CurrentUser user, Set<Role> roles) {
        var incident = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        requireHandler(incident, user, roles);
        return incident;
    }

    private void requireHandler(IncidentState incident, CurrentUser user, Set<Role> roles) {
        if (roles.stream().noneMatch(user.roles()::contains)) {
            throw new PermissionDeniedException("Vai trò không được thực hiện lệnh này với sự cố");
        }
        var permission = permissions.hasPermission(
                user, new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE), target(incident));
        if (permission.allowed()) return;
        if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
        throw new PermissionDeniedException("Không có quyền xử lý sự cố");
    }

    private static Target target(IncidentState incident) {
        return new Target(ENTITY, Long.toString(incident.id()), incident.residentId(), incident.floorAreaId());
    }

    private static void requireStatus(IncidentState incident, String... allowed) {
        if (!List.of(allowed).contains(incident.status())) {
            throw blocked("Trạng thái sự cố không cho phép lệnh này");
        }
    }

    private void audit(AuditAction action, IncidentState incident, String status, Reason reason) {
        var after = new HashMap<String, Object>();
        after.put("status", status);
        var entry = AuditLogEntry.builder()
                .action(action)
                .entity(ENTITY, incident.id())
                .resident(incident.residentId())
                .before(Map.of("status", incident.status()))
                .after(after);
        if (reason != null) entry.reason(reason);
        audit.write(entry.build());
    }

    private static ConditionNotMetException blocked(String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition("FR-045", message)));
    }
}
