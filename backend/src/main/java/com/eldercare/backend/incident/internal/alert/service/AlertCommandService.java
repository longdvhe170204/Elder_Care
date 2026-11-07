package com.eldercare.backend.incident.internal.alert.service;

import static com.eldercare.backend.incident.internal.alert.domain.AlertActions.RECEIVED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.AlertReceiveState;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh xử lý cảnh báo của actor (bảng FR-029). Tiếp nhận: người có quyền "Xử lý cảnh báo" trong phạm vi (Điều dưỡng,
 * Trưởng tầng, Bác sĩ, Người phụ trách ca theo nhiệm vụ); Quản lý viện chỉ khi cảnh báo ở Leo thang cấp 2 (FR-036,
 * Q-71). Chú thích quyền ở mức Xem vì ma trận chỉ cho Quản lý viện quyền Xem; quyền Sửa trong phạm vi kiểm trong lệnh.
 */
@Service
public class AlertCommandService {
    private static final String ENTITY = "M05.ALERT";
    private static final int MANAGER_ESCALATION_LEVEL = 2;

    private final AlertStore alerts;
    private final PermissionApi permissions;
    private final AuditLogApi audit;
    private final BusinessTime time;

    public AlertCommandService(AlertStore alerts, PermissionApi permissions, AuditLogApi audit, BusinessTime time) {
        this.alerts = alerts;
        this.permissions = permissions;
        this.audit = audit;
        this.time = time;
    }

    @Transactional
    public void receive(long alertId, CurrentUser user) {
        var alert = alerts.lockForReceive(alertId).orElseThrow(NotFoundException::new);
        boolean managerAtLevelTwo = user.roles().contains(Role.NURSING_HOME_MANAGER)
                && alert.status() == AlertStatus.ESCALATED
                && alert.escalationLevel() >= MANAGER_ESCALATION_LEVEL;
        if (!managerAtLevelTwo) requireHandling(alert, user);
        if (alert.status() == AlertStatus.RECEIVED || alert.status() == AlertStatus.IN_PROGRESS) {
            var receiver = new HashMap<String, Object>();
            if (alert.receiverId() != null)
                receiver.put("receivedBy", alert.receiverId().toString());
            if (alert.receivedAt() != null) receiver.put("receivedAt", alert.receivedAt());
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ALREADY_RECEIVED", "Cảnh báo đã có người tiếp nhận", receiver);
        }
        if (alert.status() != AlertStatus.NEW && alert.status() != AlertStatus.ESCALATED) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-029", "Cảnh báo đã kết thúc")));
        }
        var now = time.now();
        alerts.receive(alertId, user.accountId(), now, alert.level(), alert.version());
        audit.write(AuditLogEntry.builder()
                .action(RECEIVED)
                .entity(ENTITY, alertId)
                .resident(alert.residentId())
                .before(Map.of("status", alert.status().name(), "escalationLevel", alert.escalationLevel()))
                .after(Map.of("status", AlertStatus.RECEIVED.name(), "receivedBy", user.accountId()))
                .build());
    }

    private void requireHandling(AlertReceiveState alert, CurrentUser user) {
        var permission = permissions.hasPermission(
                user,
                new PermissionKey(Function.ALERT_HANDLING, Operation.UPDATE),
                new Target(ENTITY, Long.toString(alert.id()), alert.residentId(), alert.floorAreaId()));
        if (permission.allowed()) return;
        if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
        throw new PermissionDeniedException("Không có quyền tiếp nhận cảnh báo");
    }
}
