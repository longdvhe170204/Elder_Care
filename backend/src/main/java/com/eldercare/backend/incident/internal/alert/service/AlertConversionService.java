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
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.ConversionRequest;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.emergency.service.EmergencyCallService;
import com.eldercare.backend.incident.internal.incident.service.IncidentHandlingService;
import com.eldercare.backend.incident.internal.incident.service.IncidentRecordService;
import java.util.HashMap;
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
 * chọn người cao tuổi (FR-047i). Cảnh báo cùng vụ với sự cố đang mở thì gắn vào sự cố đó (FR-029, FR-041).
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
    private final IncidentHandlingService incidentHandling;

    public AlertConversionService(
            AlertHandlingService handling,
            AlertStore alerts,
            IncidentRecordService incidents,
            AuditLogApi audit,
            BusinessTime time,
            EmergencyCallService calls,
            IncidentHandlingService incidentHandling,
            @Autowired(required = false) RecordEventApi records) {
        this.handling = handling;
        this.calls = calls;
        this.incidentHandling = incidentHandling;
        this.alerts = alerts;
        this.incidents = incidents;
        this.audit = audit;
        this.time = time;
        this.history = new AlertHistory(records);
    }

    /** @return mã sự cố vừa tạo hoặc sự cố đang mở mà cảnh báo được gắn vào. */
    @Transactional
    public long convert(long alertId, ConversionRequest request, CurrentUser user) {
        var alert = handling.lockForHandler(alertId, user);
        if (ROLES.stream().noneMatch(user.roles()::contains)) {
            throw new PermissionDeniedException("Vai trò không được chuyển cảnh báo thành sự cố");
        }
        if (alert.status() != AlertStatus.IN_PROGRESS) {
            throw blocked("FR-029", "Chỉ chuyển sự cố từ cảnh báo Đang xử lý");
        }
        long incidentId = target(alert, request, alert.level(), "Mức theo cảnh báo gốc " + alertId, user);
        finish(alert, incidentId, attach(alert, request, incidentId, alert.version()), request, false, user);
        return incidentId;
    }

    @Transactional
    public long convert(
            long alertId,
            Long incidentTypeId,
            String description,
            String initialResponse,
            Long residentId,
            CurrentUser user) {
        return convert(alertId, new ConversionRequest(incidentTypeId, description, initialResponse, residentId), user);
    }

    /**
     * Lệnh "Kích hoạt khẩn cấp từ cảnh báo" (bảng FR-029; FR-047, FR-047a): từ Mới, Leo thang, Đã tiếp nhận; cùng một
     * lệnh ghi lần lượt Tiếp nhận, Bắt đầu xử lý, Chuyển sự cố với người thực hiện là người xử lý; sự cố tạo ở mức Khẩn
     * cấp nên quy trình khẩn cấp chạy trong lệnh ghi nhận. Cảnh báo chưa gắn người cao tuổi bắt buộc chọn (FR-047i).
     *
     * @return mã sự cố vừa tạo hoặc sự cố đang mở mà cảnh báo được gắn vào.
     */
    @Transactional
    public long activateEmergency(long alertId, ConversionRequest request, CurrentUser user) {
        var alert = handling.lockForHandler(alertId, user);
        if (ROLES.stream().noneMatch(user.roles()::contains)) {
            throw new PermissionDeniedException("Vai trò không được kích hoạt khẩn cấp từ cảnh báo");
        }
        if (!ACTIVATABLE.contains(alert.status())) {
            throw blocked("FR-029", "Chỉ kích hoạt khẩn cấp từ cảnh báo Mới, Leo thang hoặc Đã tiếp nhận");
        }
        long incidentId = target(alert, request, IncidentLevel.CRITICAL, null, user);
        var now = time.now();
        int version = attach(alert, request, incidentId, alert.version());
        if (alert.status() != AlertStatus.RECEIVED) {
            alerts.receive(alertId, user.accountId(), now, alert.level(), version++);
            history.add(alertId, "RECEIVED", Map.of("by", user.accountId()), now);
        }
        alerts.startHandling(alertId, user.accountId(), version++);
        history.add(alertId, "HANDLING_STARTED", Map.of("by", user.accountId()), now);
        finish(alert, incidentId, version, request, true, user);
        return incidentId;
    }

    @Transactional
    public long activateEmergency(
            long alertId,
            Long incidentTypeId,
            String description,
            String initialResponse,
            Long residentId,
            CurrentUser user) {
        return activateEmergency(
                alertId, new ConversionRequest(incidentTypeId, description, initialResponse, residentId), user);
    }

    /** Sự cố đích: sự cố đang mở cùng vụ (FR-029 "Gắn vào sự cố") hoặc sự cố mới ghi trong cùng lệnh (FR-040). */
    private long target(
            AlertRecord alert, ConversionRequest request, IncidentLevel level, String levelReason, CurrentUser user) {
        if (request.existingIncidentId() != null) {
            incidentHandling.linkAlert(request.existingIncidentId(), alert.residentId(), level, user);
            return request.existingIncidentId();
        }
        Long resident = alert.residentId() != null ? alert.residentId() : request.residentId();
        if (resident == null) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "RESIDENT_REQUIRED",
                    "Cảnh báo chưa gắn người cao tuổi, hãy chọn");
        }
        if (request.incidentTypeId() == null) throw new InvalidValueException("Thiếu loại sự cố");
        String text = request.description() != null && !request.description().isBlank()
                ? request.description()
                : alert.description();
        return incidents
                .record(
                        new RecordIncidentCommand(
                                resident,
                                request.incidentTypeId(),
                                null,
                                alert.firstAt(),
                                alert.firstAt(),
                                alert.floorAreaId(),
                                null,
                                null,
                                text,
                                level.name(),
                                levelReason,
                                request.initialResponse(),
                                request.createDespiteOpen()),
                        user,
                        alert.id())
                .incidentId();
    }

    /** FR-047i: lệnh đặt luôn người cao tuổi của cảnh báo chưa gắn; trả phiên bản kế tiếp của cảnh báo. */
    private int attach(AlertRecord alert, ConversionRequest request, long incidentId, int version) {
        if (alert.residentId() != null) return version;
        Long resident =
                request.existingIncidentId() != null ? incidentHandling.residentOf(incidentId) : request.residentId();
        alerts.attachResident(alert.id(), resident);
        return version + 1;
    }

    private void finish(
            AlertRecord alert,
            long incidentId,
            int version,
            ConversionRequest request,
            boolean emergency,
            CurrentUser user) {
        boolean linked = request.existingIncidentId() != null;
        calls.copyToIncident(alert.id(), incidentId);
        alerts.convert(alert.id(), incidentId, version);
        history.add(
                alert.id(),
                linked ? "LINKED_TO_INCIDENT" : "CONVERTED_TO_INCIDENT",
                Map.of("by", user.accountId(), "incidentId", incidentId),
                time.now());
        var after = new HashMap<String, Object>();
        after.put("status", AlertStatus.CONVERTED_TO_INCIDENT.name());
        after.put("incidentId", incidentId);
        if (emergency) after.put("emergency", true);
        if (linked) after.put("linked", true);
        audit.write(AuditLogEntry.builder()
                .action(CONVERTED)
                .entity(AlertNotices.SOURCE_TYPE, alert.id())
                .resident(alert.residentId())
                .before(Map.of("status", alert.status().name()))
                .after(after)
                .build());
    }

    private static ConditionNotMetException blocked(String code, String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, message)));
    }
}
