package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.incident.internal.alert.domain.AlertDetails;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc cảnh báo cho màn hình (FR-027, FR-034, FR-036, FR-080): danh sách theo phạm vi "Xử lý cảnh báo" (Nhân viên chăm
 * sóc chỉ người được phân công, Quản lý viện toàn viện), chi tiết kèm nguồn và lệnh được phép theo bảng FR-029.
 */
@Service
public class AlertQueryService {
    private static final PermissionKey VIEW = new PermissionKey(Function.ALERT_HANDLING, Operation.VIEW);
    private static final PermissionKey HANDLING = new PermissionKey(Function.ALERT_HANDLING, Operation.UPDATE);
    private static final SharedKind<Map> SOURCE = SharedKind.of("M05.ALERT_SOURCE", Map.class);

    private final AlertStore alerts;
    private final PermissionApi permissions;
    private final ScopeApi scopes;
    private final AccountApi accounts;
    private final Optional<RecordEventApi> records;

    public AlertQueryService(
            AlertStore alerts,
            PermissionApi permissions,
            ScopeApi scopes,
            AccountApi accounts,
            @Autowired(required = false) RecordEventApi records) {
        this.alerts = alerts;
        this.permissions = permissions;
        this.scopes = scopes;
        this.accounts = accounts;
        this.records = Optional.ofNullable(records);
    }

    /** Danh sách theo bộ lọc, đã giới hạn theo phạm vi; không kèm nguồn và lệnh (xem chi tiết). */
    @RequiresPermission(function = Function.ALERT_HANDLING, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<AlertDetails> list(
            Collection<AlertStatus> statuses,
            Long floorAreaId,
            Long residentId,
            Boolean maxEscalationReached,
            CurrentUser user) {
        var scope = scopes.filter(user, VIEW);
        return alerts
                .search(
                        statuses,
                        floorAreaId,
                        residentId,
                        maxEscalationReached,
                        scope.facilityWide(),
                        scope.floorAreaIds(),
                        scope.residentIds())
                .stream()
                .map(alert -> new AlertDetails(alert, assigneeName(alert), List.of(), Set.of()))
                .toList();
    }

    /** Chi tiết; ngoài phạm vi trả 404 như lệnh. */
    @RequiresPermission(function = Function.ALERT_HANDLING, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public AlertDetails detail(long alertId, CurrentUser user) {
        var alert = alerts.findById(alertId).orElseThrow(NotFoundException::new);
        var view = permissions.hasPermission(user, VIEW, target(alert));
        if (!view.allowed()) {
            if (view.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xem cảnh báo");
        }
        return new AlertDetails(alert, assigneeName(alert), sources(alertId), allowedCommands(alert, user));
    }

    /** Tên lệnh trong đường dẫn (glossary {@code allowedCommands}); chỉ các lệnh của US3 đã có route. */
    private Set<String> allowedCommands(AlertRecord alert, CurrentUser user) {
        boolean manager = user.roles().contains(Role.NURSING_HOME_MANAGER);
        boolean handler = !manager
                && permissions.hasPermission(user, HANDLING, target(alert)).allowed();
        var status = alert.status();
        boolean open = Set.of(AlertStatus.NEW, AlertStatus.ESCALATED, AlertStatus.RECEIVED, AlertStatus.IN_PROGRESS)
                .contains(status);
        var commands = new LinkedHashSet<String>();
        if ((status == AlertStatus.NEW || status == AlertStatus.ESCALATED)
                && (handler || (manager && status == AlertStatus.ESCALATED && alert.escalationLevel() >= 2))) {
            commands.add("receive");
        }
        if (status == AlertStatus.RECEIVED && handler) commands.add("start-handling");
        if (status == AlertStatus.RECEIVED && manager && Objects.equals(alert.assigneeId(), user.accountId())) {
            commands.add("assign");
        }
        if (open
                && handler
                && (alert.level() != IncidentLevel.CRITICAL || user.roles().contains(Role.DOCTOR))) {
            commands.add("change-level");
        }
        if (status == AlertStatus.IN_PROGRESS && handler) commands.add("close");
        return commands;
    }

    @SuppressWarnings("unchecked")
    private List<AlertDetails.Source> sources(long alertId) {
        return records.map(api -> api.list(SOURCE, new EntityRef(AlertNotices.SOURCE_TYPE, alertId)).stream()
                        .map(event -> {
                            Map<String, Object> content = (Map<String, Object>) event.content();
                            return new AlertDetails.Source(
                                    (String) content.get("sourceType"),
                                    content.get("sourceId") instanceof Number number ? number.longValue() : null,
                                    (String) content.get("level"),
                                    event.occurredAt(),
                                    resolvedAt(content.get("resolvedAt")));
                        })
                        .toList())
                .orElse(List.of());
    }

    private static Instant resolvedAt(Object value) {
        return value == null ? null : Instant.parse(value.toString());
    }

    private String assigneeName(AlertRecord alert) {
        return alert.assigneeId() == null
                ? null
                : accounts.fullName(alert.assigneeId()).orElse(null);
    }

    private static Target target(AlertRecord alert) {
        return new Target(AlertNotices.SOURCE_TYPE, Long.toString(alert.id()), alert.residentId(), alert.floorAreaId());
    }
}
