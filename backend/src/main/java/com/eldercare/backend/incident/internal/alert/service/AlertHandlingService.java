package com.eldercare.backend.incident.internal.alert.service;

import static com.eldercare.backend.incident.internal.alert.domain.AlertActions.ASSIGNED;
import static com.eldercare.backend.incident.internal.alert.domain.AlertActions.CLOSED;
import static com.eldercare.backend.incident.internal.alert.domain.AlertActions.HANDLING_STARTED;
import static com.eldercare.backend.incident.internal.alert.domain.AlertActions.LEVEL_CHANGED;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.ApproverApi;
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
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.AlertLevelRaised;
import com.eldercare.backend.incident.api.events.AssigneeChanged;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh xử lý cảnh báo sau Tiếp nhận (bảng FR-029): Bắt đầu xử lý, Nâng/Hạ mức, Đóng, Giao người phụ trách. Người xử lý
 * theo FR-036 (quyền Sửa "Xử lý cảnh báo" trong phạm vi, gồm Người phụ trách ca theo nhiệm vụ); Quản lý viện chỉ được giao
 * khi là người phụ trách (Q-71). Chú thích quyền ở mức Xem như lệnh Tiếp nhận; quyền Sửa trong phạm vi kiểm trong lệnh.
 */
@Service
public class AlertHandlingService {
    private static final String ENTITY = AlertNotices.SOURCE_TYPE;
    private static final Set<AlertStatus> OPEN =
            Set.of(AlertStatus.NEW, AlertStatus.ESCALATED, AlertStatus.RECEIVED, AlertStatus.IN_PROGRESS);
    private static final PermissionKey HANDLING = new PermissionKey(Function.ALERT_HANDLING, Operation.UPDATE);
    /** Khóa loại của cảnh báo công việc Bắt buộc quá hạn (research S-08): đóng phải có lý do (BR-M04-06). */
    private static final String OVERDUE_TASK_PREFIX = "QUA_HAN:";

    private final AlertStore alerts;
    private final PermissionApi permissions;
    private final ApproverApi approvers;
    private final AccountApi accounts;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final Optional<AlertStaffPort> staff;
    private final AlertHistory history;

    public AlertHandlingService(
            AlertStore alerts,
            PermissionApi permissions,
            ApproverApi approvers,
            AccountApi accounts,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            ParameterApi parameters,
            BusinessTime time,
            @Autowired(required = false) AlertStaffPort staff,
            @Autowired(required = false) RecordEventApi records) {
        this.alerts = alerts;
        this.permissions = permissions;
        this.approvers = approvers;
        this.accounts = accounts;
        this.audit = audit;
        this.events = events;
        this.parameters = parameters;
        this.time = time;
        this.staff = Optional.ofNullable(staff);
        this.history = new AlertHistory(records);
    }

    /** Đã tiếp nhận → Đang xử lý; người ra lệnh nhận cảnh báo (FR-031 "nhận từ người khác bằng Bắt đầu xử lý"). */
    @RequiresPermission(function = Function.ALERT_HANDLING, operation = Operation.VIEW)
    @Transactional
    public void startHandling(long alertId, CurrentUser user) {
        var alert = lockForHandler(alertId, user);
        requireStatus(alert, AlertStatus.RECEIVED);
        alerts.startHandling(alertId, user.accountId(), alert.version());
        var now = time.now();
        history.add(alertId, "HANDLING_STARTED", Map.of("by", user.accountId()), now);
        write(HANDLING_STARTED, alert, Map.of("status", AlertStatus.IN_PROGRESS.name()), null);
    }

    /** Nâng/Hạ mức khi đang mở; hạ từ Khẩn cấp chỉ Bác sĩ; Mới, Leo thang tính lại hạn từ lúc đổi (bảng FR-029). */
    @BusinessCommand
    @RequiresPermission(function = Function.ALERT_HANDLING, operation = Operation.VIEW)
    @Transactional
    public void changeLevel(long alertId, IncidentLevel newLevel, Reason reason, CurrentUser user) {
        if (reason == null) throw new ReasonRequiredException();
        if (newLevel == null) throw new InvalidValueException("Thiếu mức mới");
        var alert = lockForHandler(alertId, user);
        if (!OPEN.contains(alert.status())) throw blocked("FR-029", "Cảnh báo đã kết thúc");
        if (alert.level() == newLevel) throw new InvalidValueException("Mức mới phải khác mức hiện tại");
        if (alert.level() == IncidentLevel.CRITICAL && !user.roles().contains(Role.DOCTOR)) {
            throw new PermissionDeniedException("Chỉ Bác sĩ được hạ cảnh báo khỏi mức Khẩn cấp");
        }
        var now = time.now();
        boolean waiting = alert.status() == AlertStatus.NEW || alert.status() == AlertStatus.ESCALATED;
        Instant deadline =
                waiting ? AcknowledgementDeadlines.of(newLevel, now, alert.floorAreaId(), parameters, staff) : null;
        alerts.changeLevel(alertId, newLevel, deadline, alert.version());
        history.add(alertId, "LEVEL_CHANGED", Map.of("from", alert.level().name(), "to", newLevel.name()), now);
        write(LEVEL_CHANGED, alert, Map.of("level", newLevel.name()), reason);
        if (newLevel.ordinal() > alert.level().ordinal()) {
            // Người nhận theo mức mới: chờ task 007 T016a (dòng chờ chủ dự án ở team-plan mục 8), như lần gộp nâng mức.
            events.publishEvent(new AlertLevelRaised(
                    alertId,
                    newLevel.name(),
                    alert.noOnDutyDoctor(),
                    ENTITY + ":" + alertId + ":LEVEL_RAISED:" + newLevel.name() + ":" + now.toEpochMilli(),
                    now,
                    List.of()));
        }
    }

    /** Đang xử lý → Đã đóng: kết quả bắt buộc; cảnh báo công việc Bắt buộc quá hạn cần lý do (BR-M04-06). */
    @RequiresPermission(function = Function.ALERT_HANDLING, operation = Operation.VIEW)
    @Transactional
    public void close(long alertId, String resolution, Reason reason, CurrentUser user) {
        var alert = lockForHandler(alertId, user);
        requireStatus(alert, AlertStatus.IN_PROGRESS);
        if (resolution == null || resolution.isBlank()) throw new InvalidValueException("Thiếu kết quả xử lý");
        if (reason == null && alert.typeLock().startsWith(OVERDUE_TASK_PREFIX)) throw new ReasonRequiredException();
        var now = time.now();
        alerts.close(
                alertId, resolution, reason == null ? null : reason.value(), user.accountId(), now, alert.version());
        history.add(alertId, "CLOSED", Map.of("by", user.accountId()), now);
        write(CLOSED, alert, Map.of("status", AlertStatus.CLOSED.name()), reason);
    }

    /**
     * Giao người phụ trách: Quản lý viện khi là người phụ trách, cảnh báo Đã tiếp nhận; người được giao là Điều dưỡng,
     * Trưởng tầng hoặc Bác sĩ có quyền xử lý cảnh báo với người cao tuổi đó; ghi chú bắt buộc (bảng FR-029).
     * {@code staffId} là mã nhân viên theo api-007.
     */
    @RequiresPermission(function = Function.ALERT_HANDLING, operation = Operation.VIEW)
    @Transactional
    public void assign(long alertId, long staffId, String note, CurrentUser user) {
        if (note == null || note.isBlank()) throw new InvalidValueException("Giao người phụ trách phải có ghi chú");
        var alert = alerts.lock(alertId).orElseThrow(NotFoundException::new);
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER)
                || !Objects.equals(alert.assigneeId(), user.accountId())) {
            throw new PermissionDeniedException("Chỉ Quản lý viện đang là người phụ trách được giao cảnh báo");
        }
        requireStatus(alert, AlertStatus.RECEIVED);
        var assignee =
                accounts.byStaff(staffId).orElseThrow(() -> blocked("FR-029", "Nhân viên được giao chưa có tài khoản"));
        boolean eligible = approvers.approvers(HANDLING, target(alert)).contains(assignee.accountId())
                && accounts.hasRole(assignee.accountId(), Role.NURSING_STAFF, Role.FLOOR_MANAGER, Role.DOCTOR);
        if (!eligible) {
            throw blocked(
                    "FR-029",
                    "Người được giao phải là Điều dưỡng, Trưởng tầng hoặc Bác sĩ có quyền với người cao tuổi");
        }
        var now = time.now();
        alerts.reassign(alertId, assignee.accountId(), alert.version());
        history.add(alertId, "ASSIGNED", Map.of("by", user.accountId(), "to", assignee.accountId(), "note", note), now);
        write(ASSIGNED, alert, Map.of("assignee", assignee.accountId()), null);
        events.publishEvent(new AssigneeChanged(
                ENTITY,
                alertId,
                assignee.accountId(),
                ENTITY + ":" + alertId + ":ASSIGNED:" + assignee.accountId() + ":" + now.toEpochMilli(),
                now,
                AlertNotices.assigned(alert, staffId, now)));
    }

    AlertRecord lockForHandler(long alertId, CurrentUser user) {
        var alert = alerts.lock(alertId).orElseThrow(NotFoundException::new);
        if (user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Quản lý viện không xử lý cảnh báo (Q-71)");
        }
        var permission = permissions.hasPermission(user, HANDLING, target(alert));
        if (permission.allowed()) return alert;
        if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
        throw new PermissionDeniedException("Không có quyền xử lý cảnh báo");
    }

    private static Target target(AlertRecord alert) {
        return new Target(ENTITY, Long.toString(alert.id()), alert.residentId(), alert.floorAreaId());
    }

    private static void requireStatus(AlertRecord alert, AlertStatus expected) {
        if (alert.status() != expected) throw blocked("FR-029", "Trạng thái cảnh báo không cho phép lệnh này");
    }

    private void write(AuditAction action, AlertRecord alert, Map<String, Object> after, Reason reason) {
        var before = new HashMap<String, Object>();
        before.put("status", alert.status().name());
        before.put("level", alert.level().name());
        if (alert.assigneeId() != null) before.put("assignee", alert.assigneeId());
        var entry = AuditLogEntry.builder()
                .action(action)
                .entity(ENTITY, alert.id())
                .resident(alert.residentId())
                .before(before)
                .after(after);
        if (reason != null) entry.reason(reason);
        audit.write(entry.build());
    }

    private static ConditionNotMetException blocked(String code, String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, message)));
    }
}
