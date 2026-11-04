package com.eldercare.backend.incident.internal.alert.service;

import static com.eldercare.backend.incident.internal.alert.domain.AlertActions.CREATED_MANUALLY;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tạo cảnh báo thủ công (FR-026 (c)): Trưởng tầng, Bác sĩ, Điều dưỡng trong phạm vi chọn loại sự cố, mức và bắt buộc mô
 * tả; khóa loại {@code THU_CONG:<loại>} nên cảnh báo đang mở cùng loại được gộp (FR-028, FR-030).
 */
@Service
public class ManualAlertService {
    private final AlertService alerts;
    private final IncidentCatalogService catalog;
    private final PermissionApi permissions;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final AlertResponsibility responsibility;

    public ManualAlertService(
            AlertService alerts,
            IncidentCatalogService catalog,
            PermissionApi permissions,
            AuditLogApi audit,
            BusinessTime time,
            AlertResponsibility responsibility) {
        this.alerts = alerts;
        this.catalog = catalog;
        this.permissions = permissions;
        this.audit = audit;
        this.time = time;
        this.responsibility = responsibility;
    }

    /** Kết quả tạo: cảnh báo sau lệnh và có phải gộp vào cảnh báo đang mở hay không. */
    public record Created(AlertRecord alert, boolean merged) {}

    @RequiresPermission(function = Function.ALERT_HANDLING, operation = Operation.CREATE)
    @Transactional
    public Created create(long residentId, long incidentTypeId, String level, String description, CurrentUser user) {
        if (description == null || description.isBlank()) throw new InvalidValueException("Thiếu mô tả cảnh báo");
        var type = catalog.incidentTypes().stream()
                .filter(item -> item.id() == incidentTypeId && item.active())
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("Loại sự cố không tồn tại hoặc đã ngừng dùng"));
        var alertLevel = level(level);
        var now = time.now();
        Long floorAreaId = responsibility.floorOf(residentId, now).orElse(null);
        var permission = permissions.hasPermission(
                user,
                new PermissionKey(Function.ALERT_HANDLING, Operation.CREATE),
                new Target(AlertNotices.SOURCE_TYPE, null, residentId, floorAreaId));
        if (!permission.allowed()) throw new PermissionDeniedException("Không có quyền tạo cảnh báo cho người này");

        var alert = alerts.requestAlert(new CreateAlertCommand(
                        residentId,
                        floorAreaId,
                        type.code(),
                        TypeLock.forManual(incidentTypeId),
                        alertLevel,
                        now,
                        null,
                        null,
                        description,
                        user.accountId()))
                .orElseThrow(() -> new ConditionNotMetException(
                        List.of(new UnmetCondition("FR-029", "Người cao tuổi đã ở trạng thái cuối"))));
        boolean merged = alert.occurrenceCount() > 1;
        audit.write(AuditLogEntry.builder()
                .action(CREATED_MANUALLY)
                .entity(AlertNotices.SOURCE_TYPE, alert.id())
                .resident(residentId)
                .after(Map.of("level", alertLevel.name(), "type", type.code(), "merged", merged))
                .build());
        return new Created(alert, merged);
    }

    private static IncidentLevel level(String value) {
        try {
            return IncidentLevel.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidValueException("Mức cảnh báo không hợp lệ");
        }
    }
}
