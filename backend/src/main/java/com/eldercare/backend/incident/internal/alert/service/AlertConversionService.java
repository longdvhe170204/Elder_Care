package com.eldercare.backend.incident.internal.alert.service;

import static com.eldercare.backend.incident.internal.alert.domain.AlertActions.CONVERTED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.emergency.service.EmergencyCallService;
import com.eldercare.backend.incident.internal.incident.service.IncidentRecordService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private static final Set<AlertStatus> ACTIVATABLE =
            Set.of(AlertStatus.NEW, AlertStatus.ESCALATED, AlertStatus.RECEIVED);

    private final AlertHandlingService handling;
    private final AlertStore alerts;
    private final IncidentRecordService incidents;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final AlertHistory history;
    private final EmergencyCallService calls;

    public AlertConversionService(
            AlertHandlingService handling,
            AlertStore alerts,
            IncidentRecordService incidents,
            AuditLogApi audit,
            BusinessTime time,
            EmergencyCallService calls,
            @Autowired(required = false) RecordEventApi records) {
        this.handling = handling;
        this.calls = calls;
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
        if (resident == null) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "RESIDENT_REQUIRED",
                    "Cảnh báo chưa gắn người cao tuổi, hãy chọn");
        }
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
        int version = alert.version();
        if (alert.residentId() == null) {
            alerts.attachResident(alertId, resident); // FR-047i: lệnh đặt luôn người cao tuổi của cảnh báo
            version++;
        }
        calls.copyToIncident(alertId, incidentId);
        alerts.convert(alertId, incidentId, version);
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

    /**
     * Lệnh "Kích hoạt khẩn cấp từ cảnh báo" (bảng FR-029; FR-047, FR-047a): từ Mới, Leo thang, Đã tiếp nhận; cùng một
     * lệnh ghi lần lượt Tiếp nhận, Bắt đầu xử lý, Chuyển sự cố với người thực hiện là người xử lý; sự cố tạo ở mức Khẩn
     * cấp nên quy trình khẩn cấp chạy trong lệnh ghi nhận. Cảnh báo chưa gắn người cao tuổi bắt buộc chọn (FR-047i).
     *
     * @return mã sự cố vừa tạo.
     */
    @Transactional
    public long activateEmergency(
            long alertId,
            Long incidentTypeId,
            String description,
            String initialResponse,
            Long residentId,
            CurrentUser user) {
        var alert = handling.lockForHandler(alertId, user);
        if (ROLES.stream().noneMatch(user.roles()::contains)) {
            throw new PermissionDeniedException("Vai trò không được kích hoạt khẩn cấp từ cảnh báo");
        }
        if (!ACTIVATABLE.contains(alert.status())) {
            throw blocked("FR-029", "Chỉ kích hoạt khẩn cấp từ cảnh báo Mới, Leo thang hoặc Đã tiếp nhận");
        }
        Long resident = alert.residentId() != null ? alert.residentId() : residentId;
        if (resident == null) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "RESIDENT_REQUIRED",
                    "Cảnh báo chưa gắn người cao tuổi, hãy chọn");
        }
        if (incidentTypeId == null) throw new InvalidValueException("Thiếu loại sự cố");
        var now = time.now();
        int version = alert.version();
        if (alert.residentId() == null) {
            alerts.attachResident(alertId, resident); // FR-047i
            version++;
        }
        if (alert.status() != AlertStatus.RECEIVED) {
            alerts.receive(alertId, user.accountId(), now, alert.level(), version++);
            history.add(alertId, "RECEIVED", Map.of("by", user.accountId()), now);
        }
        alerts.startHandling(alertId, user.accountId(), version++);
        history.add(alertId, "HANDLING_STARTED", Map.of("by", user.accountId()), now);
        String text = description != null && !description.isBlank() ? description : alert.description();
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
                                IncidentLevel.CRITICAL.name(),
                                null,
                                initialResponse,
                                false),
                        user,
                        alertId)
                .incidentId();
        calls.copyToIncident(alertId, incidentId);
        alerts.convert(alertId, incidentId, version);
        history.add(alertId, "CONVERTED_TO_INCIDENT", Map.of("by", user.accountId(), "incidentId", incidentId), now);
        audit.write(AuditLogEntry.builder()
                .action(CONVERTED)
                .entity(AlertNotices.SOURCE_TYPE, alertId)
                .resident(alert.residentId())
                .before(Map.of("status", alert.status().name()))
                .after(Map.of(
                        "status",
                        AlertStatus.CONVERTED_TO_INCIDENT.name(),
                        "incidentId",
                        incidentId,
                        "emergency",
                        true))
                .build());
        return incidentId;
    }

    private static ConditionNotMetException blocked(String code, String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, message)));
    }
}
