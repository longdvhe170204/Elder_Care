package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
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
    @Transactional(readOnly = true)
    public AlertDetails detail(long alertId, CurrentUser user) {
        var alert = alerts.findById(alertId).orElseThrow(NotFoundException::new);
        var view = permissions.hasPermission(user, VIEW, target(alert));
        if (!view.allowed()) {
            if (view.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xem cảnh báo");
        }
        var sources = sources(alertId);
        return new AlertDetails(
                alert, assigneeName(alert), sources, allowedCommands(alert, user), press(alert, sources));
    }

    /** Người bấm, loại sự việc, nơi đã chọn (nút hỗ trợ: dòng M05.ASSIST_DETAIL; SOS: nơi của lần bấm mới nhất). */
    @SuppressWarnings("unchecked")
    private AlertDetails.Press press(AlertRecord alert, List<AlertDetails.Source> sources) {
        if (records.isEmpty()) return AlertDetails.Press.NONE;
        var api = records.get();
        if (EmergencyAssistService.ALERT_TYPE.equals(alert.type())) {
            var details = new java.util.HashMap<String, Object>();
            api.list(EmergencyAssistService.DETAIL, new EntityRef(AlertNotices.SOURCE_TYPE, alert.id()))
                    .forEach(row -> details.putAll((Map<String, Object>) row.content()));
            String pressedBy = alert.requesterId() == null
                    ? null
                    : accounts.fullName(alert.requesterId()).orElse(null);
            return new AlertDetails.Press(pressedBy, (String) details.get("eventType"), (String) details.get("place"));
        }
        if (SosService.ALERT_TYPE.equals(alert.type())) {
            String place = sources.stream()
                    .filter(source -> SosService.PRESS_SOURCE.equals(source.sourceType()) && source.sourceId() != null)
                    .reduce((first, second) -> second)
                    .flatMap(source -> api.findByUniqueKey(SosService.PLACE, "SOS_PLACE:" + source.sourceId()))
                    .map(row -> row.content().place().name())
                    .orElse(null);
            return new AlertDetails.Press(null, null, place);
        }
        return AlertDetails.Press.NONE;
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
        boolean pressed =
                SosService.ALERT_TYPE.equals(alert.type()) || EmergencyAssistService.ALERT_TYPE.equals(alert.type());
        if (pressed
                && handler
                && alert.arrivedAt() == null
                && (status == AlertStatus.RECEIVED || status == AlertStatus.IN_PROGRESS)
                && (Objects.equals(alert.assigneeId(), user.accountId())
                        || Objects.equals(alert.receiverId(), user.accountId()))) {
            commands.add("mark-arrived");
        }
        if (open && handler && alert.level() == IncidentLevel.CRITICAL) commands.add("call-115");
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
