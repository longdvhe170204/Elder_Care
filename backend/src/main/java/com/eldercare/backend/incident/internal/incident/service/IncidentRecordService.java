package com.eldercare.backend.incident.internal.incident.service;

import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.RECORDED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import com.eldercare.backend.incident.api.events.IncidentNotificationCreated;
import com.eldercare.backend.incident.api.events.IncidentRecorded;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;
import com.eldercare.backend.incident.internal.alert.service.AcknowledgementDeadlines;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.incident.internal.incident.domain.NewIncident;
import com.eldercare.backend.incident.internal.incident.domain.RecordedIncident;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh "Ghi nhận sự cố" (T035 lát 1; FR-040 → FR-043 a, b; FR-047 a; FR-047b). Các tác động riêng của loại Ngã
 * (FR-051), lây nhiễm nghi ngờ (mục I) và thẻ khẩn cấp (mục G) làm ở lát sau.
 */
@Service
public class IncidentRecordService {
    private static final String ENTITY = "M05.INCIDENT";
    private static final String FACILITY = "FACILITY";
    private static final String FALL = "FALL";
    /** FR-043 (b) sửa theo spec 013 FR-022: không báo đánh giá lại ở mọi mức. */
    private static final Set<String> NO_REASSESSMENT = Set.of("BELONGINGS_LOST", "BELONGINGS_DAMAGED");

    private final IncidentStore incidents;
    private final IncidentCatalogService catalog;
    private final PermissionApi permissions;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final MonitoringLock monitoringLock;
    private final Optional<AlertStaffPort> staff;
    private final Optional<ResidentApi> residents;
    private final Optional<ResidentLocationApi> locations;

    public IncidentRecordService(
            IncidentStore incidents,
            IncidentCatalogService catalog,
            PermissionApi permissions,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time,
            ParameterApi parameters,
            MonitoringLock monitoringLock,
            @Autowired(required = false) AlertStaffPort staff,
            @Autowired(required = false) ResidentApi residents,
            @Autowired(required = false) ResidentLocationApi locations) {
        this.incidents = incidents;
        this.catalog = catalog;
        this.permissions = permissions;
        this.audit = audit;
        this.events = events;
        this.time = time;
        this.parameters = parameters;
        this.monitoringLock = monitoringLock;
        this.staff = Optional.ofNullable(staff);
        this.residents = Optional.ofNullable(residents);
        this.locations = Optional.ofNullable(locations);
    }

    @RequiresPermission(function = Function.INCIDENT_EMERGENCY, operation = Operation.CREATE)
    @Transactional
    public RecordedIncident record(RecordIncidentCommand command, CurrentUser user) {
        var type = activeType(command.incidentTypeId());
        boolean facility = FACILITY.equals(type.code());
        Long residentId = command.residentId();
        if (residentId == null && !facility) {
            throw blocked("FR-040", "Sự cố phải gắn người cao tuổi, trừ loại cơ sở vật chất");
        }
        if (facility && command.floorAreaId() == null) {
            throw blocked("FR-040", "Sự cố cơ sở vật chất phải có tầng hoặc khu vực");
        }
        if (command.description() == null || command.description().isBlank()) {
            throw new InvalidValueException("Thiếu mô tả sự cố");
        }
        IncidentLevel level = level(command.level());
        if (level.ordinal() < type.defaultLevel().ordinal()
                && (command.lowerLevelReason() == null
                        || command.lowerLevelReason().isBlank())) {
            throw new ReasonRequiredException();
        }

        Long floorAreaId = residentId == null ? command.floorAreaId() : residentFloor(residentId, command);
        var permission = permissions.hasPermission(
                user,
                new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE),
                new Target(ENTITY, null, residentId, floorAreaId));
        if (!permission.allowed()) throw new PermissionDeniedException("Không có quyền ghi nhận sự cố");
        boolean outOfScope =
                permission.basis().map(PermissionBasis.OUT_OF_SCOPE::equals).orElse(false);

        if (residentId != null) {
            if (residents.isPresent() && residents.get().isFinalStatus(residentId)) {
                throw blocked("FR-041", "Người cao tuổi đã ở trạng thái cuối");
            }
            monitoringLock.lock(residentId);
            var open = incidents.openEmergency(residentId);
            if (open.isPresent() && !command.createDespiteExisting()) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "OPEN_EMERGENCY_INCIDENT_EXISTS",
                        "Người cao tuổi đang có sự cố Khẩn cấp chưa đóng",
                        Map.of("incidentId", open.get()));
            }
        }

        var now = time.now();
        boolean noOnDutyDoctor = level == IncidentLevel.CRITICAL
                && staff.map(port -> port.onDutyDoctors(now).isEmpty()).orElse(true);
        var deadline = AcknowledgementDeadlines.of(level, now, floorAreaId, parameters, staff);
        long id = incidents.insert(new NewIncident(
                residentId,
                type.id(),
                type.code(),
                command.source(),
                command.occurredAt(),
                command.detectedAt(),
                floorAreaId,
                command.location(),
                command.ongoingActivity(),
                command.description(),
                command.initialResponse(),
                level,
                level.ordinal() < type.defaultLevel().ordinal() ? command.lowerLevelReason() : null,
                outOfScope,
                user.accountId(),
                now,
                deadline,
                noOnDutyDoctor));

        var after = new HashMap<String, Object>();
        after.put("type", type.code());
        after.put("level", level.name());
        after.put("outOfScope", outOfScope);
        audit.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(ENTITY, id)
                .resident(residentId)
                .after(after)
                .build());

        if (residentId != null) {
            boolean needsReassessment =
                    !NO_REASSESSMENT.contains(type.code()) && (level != IncidentLevel.LOW || FALL.equals(type.code()));
            events.publishEvent(new IncidentRecorded(id, residentId, type.code(), level.name(), needsReassessment));
        }
        events.publishEvent(new IncidentNotificationCreated(
                id,
                level.name(),
                ENTITY + ":" + id + ":RECORDED",
                now,
                IncidentNotices.recorded(id, residentId, floorAreaId, level, noOnDutyDoctor, user.accountId(), now)));
        return new RecordedIncident(id, outOfScope, noOnDutyDoctor);
    }

    private IncidentTypeView activeType(long typeId) {
        return catalog.incidentTypes().stream()
                .filter(type -> type.id() == typeId && type.active())
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("Loại sự cố không tồn tại hoặc đã ngừng dùng"));
    }

    private static IncidentLevel level(String value) {
        try {
            return IncidentLevel.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidValueException("Mức sự cố không hợp lệ");
        }
    }

    /** Tầng của người cao tuổi theo giường đang giữ (research S-08); chưa có nguồn thì dùng tầng người ghi chọn. */
    private Long residentFloor(long residentId, RecordIncidentCommand command) {
        return locations
                .flatMap(api -> api.current(residentId))
                .map(location -> location.floorId())
                .orElse(command.floorAreaId());
    }

    private static ConditionNotMetException blocked(String code, String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, message)));
    }
}
