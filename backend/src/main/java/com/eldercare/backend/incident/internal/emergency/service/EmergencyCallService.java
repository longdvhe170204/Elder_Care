package com.eldercare.backend.incident.internal.emergency.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.error.AlreadyProcessedException;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.emergency.domain.Call115Attempt;
import com.eldercare.backend.incident.internal.emergency.domain.Call115Content;
import com.eldercare.backend.incident.internal.emergency.domain.Call115Info;
import com.eldercare.backend.incident.internal.emergency.domain.Call115Result;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentProgressionService;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nút "Gọi 115" trên cảnh báo, sự cố mức Khẩn cấp (US13; FR-047k, FR-048b, BR-M05-31; research S-24). Thông tin đọc cho
 * tổng đài lấy từ CFG-M11-11, vị trí, nhận dạng; dị ứng, bệnh nền chỉ khi người xem được xem đủ nhóm sức khỏe. Lượt gọi
 * ghi trước khi app mở trình quay số; kết quả đặt một lần, chỉ người bấm; Đã gọi được thì thêm diễn biến
 * {@code CALL_EMERGENCY} vào sự cố liên kết; lượt trên cảnh báo chưa có sự cố được chép khi chuyển sự cố.
 */
@Service
public class EmergencyCallService {
    static final SharedKind<Call115Content> CALL =
            SharedKind.of("M05.CALL_115", Call115Content.class).notDeletable();
    static final SharedKind<Call115Result> RESULT =
            SharedKind.of("M05.CALL_115_RESULT", Call115Result.class).notDeletable();
    static final String PROGRESSION_TYPE = "CALL_EMERGENCY";
    private static final String ALERT = "M05.ALERT";
    private static final String INCIDENT = "M05.INCIDENT";
    private static final PermissionKey ALERT_HANDLING = new PermissionKey(Function.ALERT_HANDLING, Operation.UPDATE);
    private static final PermissionKey INCIDENT_HANDLING =
            new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE);

    private final AlertStore alerts;
    private final IncidentStore incidents;
    private final IncidentProgressionStore progressions;
    private final IncidentProgressionService progressionTypes;
    private final RecordEventApi records;
    private final PermissionApi permissions;
    private final FieldVisibilityApi visibility;
    private final ParameterApi parameters;
    private final EmergencyCardService cards;
    private final BusinessTime time;
    private final Optional<ResidentApi> residents;
    private final Optional<ResidentLocationApi> locations;
    private final Optional<AccountApi> accounts;

    public EmergencyCallService(
            AlertStore alerts,
            IncidentStore incidents,
            IncidentProgressionStore progressions,
            IncidentProgressionService progressionTypes,
            RecordEventApi records,
            PermissionApi permissions,
            FieldVisibilityApi visibility,
            ParameterApi parameters,
            EmergencyCardService cards,
            BusinessTime time,
            @Autowired(required = false) ResidentApi residents,
            @Autowired(required = false) ResidentLocationApi locations,
            @Autowired(required = false) AccountApi accounts) {
        this.alerts = alerts;
        this.incidents = incidents;
        this.progressions = progressions;
        this.progressionTypes = progressionTypes;
        this.records = records;
        this.permissions = permissions;
        this.visibility = visibility;
        this.parameters = parameters;
        this.cards = cards;
        this.time = time;
        this.residents = Optional.ofNullable(residents);
        this.locations = Optional.ofNullable(locations);
        this.accounts = Optional.ofNullable(accounts);
    }

    /** Đối tượng gọi 115: cảnh báo hoặc sự cố Khẩn cấp, sau khi kiểm quyền xử lý (FR-036, FR-045). */
    private record Subject(String type, long id, Long residentId, Long floorAreaId, Long alertId, Long incidentId) {
        Target target() {
            return new Target(type, Long.toString(id), residentId, floorAreaId);
        }
    }

    @Transactional(readOnly = true)
    public Call115Info alertInfo(long alertId, CurrentUser user) {
        return info(alertSubject(alertId, user), user);
    }

    @Transactional(readOnly = true)
    public Call115Info incidentInfo(long incidentId, CurrentUser user) {
        return info(incidentSubject(incidentId, user), user);
    }

    @Transactional
    public Call115Attempt pressOnAlert(long alertId, CurrentUser user) {
        return press(alertSubject(alertId, user), user);
    }

    @Transactional
    public Call115Attempt pressOnIncident(long incidentId, CurrentUser user) {
        return press(incidentSubject(incidentId, user), user);
    }

    /** Kết quả gọi (FR-047k): một lần, chỉ người bấm; Đã gọi được thì thêm diễn biến vào sự cố liên kết (FR-048b). */
    @Transactional
    public void result(long attemptId, String result, CurrentUser user) {
        if (!"CONNECTED".equals(result) && !"NOT_CONNECTED".equals(result)) {
            throw new InvalidValueException("Kết quả gọi không hợp lệ");
        }
        RecordEvent<Call115Content> attempt = records.require(CALL, attemptId);
        if (attempt.content().pressedBy() != user.accountId()) {
            throw new PermissionDeniedException("Chỉ người bấm gọi 115 chọn kết quả");
        }
        String key = resultKey(attemptId);
        if (records.findByUniqueKey(RESULT, key).isPresent()) {
            throw new AlreadyProcessedException("Đã ghi kết quả lượt gọi này");
        }
        Long incidentId = attempt.content().incidentId() != null
                ? attempt.content().incidentId()
                : alerts.findById(attempt.content().alertId())
                        .map(AlertRecord::incidentId)
                        .orElse(null);
        Long progressionId = "CONNECTED".equals(result) && incidentId != null
                ? addProgression(incidentId, attempt.content(), attempt.floorAreaId())
                : null;
        Instant now = time.now();
        records.add(
                RESULT,
                new EntityRef(CALL.code(), attemptId),
                new NewRecordEvent<>(
                        new Call115Result(result, progressionId),
                        null,
                        user.accountId(),
                        false,
                        now,
                        now,
                        now,
                        attempt.floorAreaId(),
                        key));
    }

    /**
     * Chuyển cảnh báo thành sự cố (plan điểm báo lại 12): lượt gọi Đã gọi được trên cảnh báo được chép thành diễn biến
     * {@code CALL_EMERGENCY} của sự cố mới với thời điểm bấm. Cùng transaction với lệnh chuyển.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void copyToIncident(long alertId, long incidentId) {
        for (var attempt : records.list(CALL, new EntityRef(ALERT, alertId))) {
            var result = records.findByUniqueKey(RESULT, resultKey(attempt.id()));
            if (result.isPresent() && "CONNECTED".equals(result.get().content().result())) {
                addProgression(incidentId, attempt.content(), attempt.floorAreaId());
            }
        }
    }

    private Call115Attempt press(Subject subject, CurrentUser user) {
        Instant now = time.now();
        var attempt = records.add(
                CALL,
                new EntityRef(subject.type(), subject.id()),
                new NewRecordEvent<>(
                        new Call115Content(subject.alertId(), subject.incidentId(), user.accountId(), now),
                        null,
                        user.accountId(),
                        false,
                        now,
                        now,
                        now,
                        subject.floorAreaId(),
                        null));
        return new Call115Attempt(attempt.id(), now, name(user.accountId()), null);
    }

    private Call115Info info(Subject subject, CurrentUser user) {
        var facility = facility();
        String floor = null;
        String room = null;
        Call115Info.Resident resident = null;
        List<String> allergies = null;
        List<String> conditions = null;
        var hidden = new ArrayList<String>();
        if (subject.residentId() != null) {
            long id = subject.residentId();
            var location = locations.flatMap(api -> api.current(id));
            floor = location.map(l -> l.floorName()).orElse(null);
            room = location.map(l -> l.roomName()).orElse(null);
            resident = new Call115Info.Resident(
                    residents.map(api -> api.identity(id).fullName()).orElse(null),
                    null,
                    residents.flatMap(api -> api.gender(id)).map(Enum::name).orElse(null));
            if (visibility.canView(user, InfoGroup.HEALTH, subject.target()) == Visibility.FULL) {
                var card = cards.build(id, time.now());
                allergies = names(card.allergies());
                conditions = names(card.chronicConditions());
            } else {
                hidden.add("allergies");
                hidden.add("chronicConditions");
            }
        }
        return new Call115Info(
                facility == null
                        ? new Call115Info.Facility(null, null, null)
                        : new Call115Info.Facility(facility.name(), facility.address(), facility.phone()),
                floor,
                room,
                resident,
                allergies,
                conditions,
                List.copyOf(hidden));
    }

    private Subject alertSubject(long alertId, CurrentUser user) {
        var alert = alerts.findById(alertId).orElseThrow(NotFoundException::new);
        var subject = new Subject(ALERT, alertId, alert.residentId(), alert.floorAreaId(), alertId, alert.incidentId());
        check(user, ALERT_HANDLING, subject);
        if (alert.level() != IncidentLevel.CRITICAL) throw notCritical();
        return subject;
    }

    private Subject incidentSubject(long incidentId, CurrentUser user) {
        IncidentView incident = incidents.view(incidentId).orElseThrow(NotFoundException::new);
        var subject = new Subject(
                INCIDENT,
                incidentId,
                incident.residentId(),
                incident.floorAreaId(),
                incident.sourceAlertId(),
                incidentId);
        check(user, INCIDENT_HANDLING, subject);
        if (incident.level() != IncidentLevel.CRITICAL) throw notCritical();
        return subject;
    }

    private void check(CurrentUser user, PermissionKey key, Subject subject) {
        var permission = permissions.hasPermission(user, key, subject.target());
        if (permission.allowed()) return;
        if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
        throw new PermissionDeniedException("Không có quyền xử lý");
    }

    private long addProgression(long incidentId, Call115Content call, Long floorAreaId) {
        return progressions.add(
                incidentId,
                progressionTypes.typeIdByCode(PROGRESSION_TYPE),
                PROGRESSION_TYPE,
                "Đã gọi được cấp cứu 115",
                call.pressedAt(),
                call.pressedBy(),
                false,
                floorAreaId,
                time.now());
    }

    private FacilityInfo facility() {
        var value = parameters.read(ParameterCode.CFG_M11_11);
        return value.isSet() ? value.asTable(FacilityInfo.class) : null;
    }

    private String name(long accountId) {
        return accounts.flatMap(api -> api.fullName(accountId)).orElse(null);
    }

    private static List<String> names(List<Map<String, Object>> items) {
        if (items == null) return null;
        return items.stream().map(item -> (String) item.get("name")).toList();
    }

    private static String resultKey(long attemptId) {
        return "CALL_115_RESULT:" + attemptId;
    }

    private static BusinessException notCritical() {
        return new BusinessException(
                HttpStatus.CONFLICT, "CONDITION_NOT_MET", "Gọi 115 chỉ dùng cho cảnh báo, sự cố mức Khẩn cấp");
    }
}
