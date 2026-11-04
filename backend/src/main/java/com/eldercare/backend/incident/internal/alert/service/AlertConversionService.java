package com.eldercare.backend.incident.internal.alert.service;

import static com.eldercare.backend.incident.internal.alert.domain.AlertActions.CONVERTED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentRecordService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh "Chuyển sự cố" của cảnh báo Đang xử lý (bảng FR-029): tạo sự cố liên kết với mức bằng mức cảnh báo, cảnh báo sang
 * Chuyển sự cố. Chỉ Điều dưỡng, Trưởng tầng, Bác sĩ có quyền xử lý cảnh báo; cảnh báo không gắn người cao tuổi bắt buộc
 * chọn người cao tuổi (FR-047i).
 */
@Service
public class AlertConversionService {
    private static final Set<Role> ROLES = Set.of(Role.NURSING_STAFF, Role.FLOOR_MANAGER, Role.DOCTOR);

    private final AlertHandlingService handling;
    private final AlertStore alerts;
    private final IncidentRecordService incidents;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final AlertHistory history;

    public AlertConversionService(
            AlertHandlingService handling,
            AlertStore alerts,
            IncidentRecordService incidents,
            AuditLogApi audit,
            BusinessTime time,
            @Autowired(required = false) RecordEventApi records) {
        this.handling = handling;
        this.alerts = alerts;
        this.incidents = incidents;
        this.audit = audit;
        this.time = time;
        this.history = new AlertHistory(records);
    }

    /** @return mã sự cố vừa tạo. */
    @Transactional
    public long convert(
            long alertId,
            Long incidentTypeId,
            String description,
            String initialResponse,
            Long residentId,
            CurrentUser user) {
        var alert = handling.lockForHandler(alertId, user);
        if (ROLES.stream().noneMatch(user.roles()::contains)) {
            throw new PermissionDeniedException("Vai trò không được chuyển cảnh báo thành sự cố");
        }
        if (alert.status() != AlertStatus.IN_PROGRESS) {
            throw blocked("FR-029", "Chỉ chuyển sự cố từ cảnh báo Đang xử lý");
        }
        Long resident = alert.residentId() != null ? alert.residentId() : residentId;
        if (resident == null) throw new InvalidValueException("RESIDENT_REQUIRED: cảnh báo chưa gắn người cao tuổi");
        if (incidentTypeId == null) throw new InvalidValueException("Thiếu loại sự cố");
        String text = description != null && !description.isBlank() ? description : alert.description();
        var now = time.now();
        long incidentId = incidents
                .record(
                        new RecordIncidentCommand(
                                resident,
                                incidentTypeId,
                                null,
                                alert.firstAt(),
                                alert.firstAt(),
                                alert.floorAreaId(),
                                null,
                                null,
                                text,
                                alert.level().name(),
                                "Mức theo cảnh báo gốc " + alertId,
                                initialResponse,
                                false),
                        user,
                        alertId)
                .incidentId();
        alerts.convert(alertId, incidentId, alert.version());
        history.add(alertId, "CONVERTED_TO_INCIDENT", Map.of("by", user.accountId(), "incidentId", incidentId), now);
        audit.write(AuditLogEntry.builder()
                .action(CONVERTED)
                .entity(AlertNotices.SOURCE_TYPE, alertId)
                .resident(alert.residentId())
                .before(Map.of("status", alert.status().name()))
                .after(Map.of("status", AlertStatus.CONVERTED_TO_INCIDENT.name(), "incidentId", incidentId))
                .build());
        return incidentId;
    }

    private static ConditionNotMetException blocked(String code, String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, message)));
    }
}
