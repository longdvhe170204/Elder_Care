package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.error.AlreadyProcessedException;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.AlertNoticeRequested;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.AssistDetails;
import com.eldercare.backend.incident.internal.alert.domain.AssistPressContent;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.SosStatusView;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nút hỗ trợ khẩn cấp của nhân viên (US13; FR-026 (e), FR-047i, BR-M05-29; research S-24). Người đang trong ca bấm thì
 * tạo hoặc gộp (theo người bấm) cảnh báo Khẩn cấp {@code STAFF_ASSIST_REQUEST} chưa gắn người cao tuổi qua đường tạo
 * sẵn có (S-08); không tự tạo sự cố, không báo người thân. Thiếu {@link AlertStaffPort} thì mọi lần bấm bị từ chối
 * {@code NOT_ON_SHIFT} (module-interfaces 2.3).
 */
@Service
public class EmergencyAssistService {
    public static final String ALERT_TYPE = "STAFF_ASSIST_REQUEST";
    static final String PRESS_SOURCE = "M05.ASSIST_PRESS";
    static final SharedKind<AssistPressContent> PRESS =
            SharedKind.of(PRESS_SOURCE, AssistPressContent.class).notDeletable();
    static final SharedKind<Map> DETAIL =
            SharedKind.of("M05.ASSIST_DETAIL", Map.class).notDeletable();
    private static final PermissionKey BUTTON = new PermissionKey(Function.EMERGENCY_ASSIST_BUTTON, Operation.CREATE);

    private final AlertService alerts;
    private final AlertStore store;
    private final RecordEventApi records;
    private final MonitoringLock monitoringLock;
    private final BusinessTime time;
    private final PermissionApi permissions;
    private final AlertResponsibility responsibility;
    private final PressStatusViews views;
    private final ApplicationEventPublisher events;
    private final Optional<AlertStaffPort> staff;
    private final Optional<ResidentApi> residents;

    public EmergencyAssistService(
            AlertService alerts,
            AlertStore store,
            RecordEventApi records,
            MonitoringLock monitoringLock,
            BusinessTime time,
            PermissionApi permissions,
            AlertResponsibility responsibility,
            PressStatusViews views,
            ApplicationEventPublisher events,
            @Autowired(required = false) AlertStaffPort staff,
            @Autowired(required = false) ResidentApi residents) {
        this.alerts = alerts;
        this.store = store;
        this.records = records;
        this.monitoringLock = monitoringLock;
        this.time = time;
        this.permissions = permissions;
        this.responsibility = responsibility;
        this.views = views;
        this.events = events;
        this.staff = Optional.ofNullable(staff);
        this.residents = Optional.ofNullable(residents);
    }

    /** Một lần bấm (FR-047i): trong ca mới bấm được, ngoài ca 409 {@code NOT_ON_SHIFT} kèm số liên hệ. */
    @Transactional
    public SosStatusView press(CurrentUser user, String idempotencyKey) {
        // Lớp vai trò (Phụ lục 27 dòng Nút hỗ trợ khẩn cấp); lần bấm chưa có đối tượng nên không xét phạm vi, điều kiện
        // "trong ca" (chú thích 49) kiểm bằng AlertStaffPort.onShift bên dưới (research S-24).
        if (!permissions.permissions(user).contains(BUTTON)) {
            throw new PermissionDeniedException("Không có quyền dùng nút hỗ trợ khẩn cấp");
        }
        Instant now = time.now();
        long staffId = user.staffId().orElseThrow(() -> notOnShift());
        if (staff.isEmpty() || !staff.get().onShift(staffId, now)) throw notOnShift();
        Long floor = staff.get().floorDuringShift(staffId, now).orElse(null);
        var press = records.add(
                PRESS,
                new EntityRef("M15.ACCOUNT", user.accountId()),
                new NewRecordEvent<>(
                        new AssistPressContent(user.accountId(), staffId, idempotencyKey, floor),
                        null,
                        user.accountId(),
                        false,
                        now,
                        now,
                        now,
                        floor,
                        null));
        boolean noOnDutyDoctor = staff.get().onDutyDoctors(now).isEmpty();
        List<Long> floorsWithShift = floor == null ? staff.get().floorsWithShift(now) : List.of();
        var alert = alerts.requestAlert(
                        new CreateAlertCommand(
                                null,
                                floor,
                                ALERT_TYPE,
                                TypeLock.forStaffAssist(user.accountId()),
                                IncidentLevel.CRITICAL,
                                now,
                                PRESS_SOURCE,
                                press.id(),
                                "Nhân viên yêu cầu hỗ trợ khẩn cấp",
                                user.accountId(),
                                user.accountId()),
                        created -> AssistNotices.created(created, noOnDutyDoctor, floorsWithShift, now))
                .orElseThrow();
        if (alert.occurrenceCount() == 1 && noOnDutyDoctor) store.markNoOnDutyDoctor(alert.id());
        return views.of(press.id(), alert);
    }

    /** Cảnh báo nút hỗ trợ đang mở gần nhất của người bấm (FR-047j); rỗng khi không có. */
    @Transactional(readOnly = true)
    public Optional<SosStatusView> current(CurrentUser user) {
        return store.findOpenAssist(user.accountId()).map(alert -> views.of(latestPress(alert.id()), alert));
    }

    /**
     * Người bấm chọn loại sự việc, tầng, nơi, người cao tuổi (FR-047i); mỗi trường đặt một lần (lần sau 409). Tầng mới
     * báo thêm nhóm của tầng và đặt người phụ trách nếu chưa có; người cao tuổi gắn vào cảnh báo và báo nhóm FR-047a.
     */
    @Transactional
    public void details(long alertId, AssistDetails details, CurrentUser user) {
        if (details == null || details.isEmpty()) throw new InvalidValueException("Chưa chọn chi tiết nào");
        var alert = store.lock(alertId).orElseThrow(NotFoundException::new);
        if (!ALERT_TYPE.equals(alert.type()) || !Long.valueOf(user.accountId()).equals(alert.requesterId())) {
            throw new NotFoundException();
        }
        if (!List.of(AlertStatus.NEW, AlertStatus.ESCALATED, AlertStatus.RECEIVED, AlertStatus.IN_PROGRESS)
                .contains(alert.status())) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Cảnh báo đã kết thúc");
        }
        var fields = new LinkedHashMap<String, Object>();
        if (details.eventType() != null)
            fields.put("eventType", details.eventType().name());
        if (details.floorAreaId() != null) fields.put("floorAreaId", details.floorAreaId());
        if (details.place() != null) fields.put("place", details.place().name());
        if (details.residentId() != null) fields.put("residentId", details.residentId());
        for (String field : fields.keySet()) {
            if (records.findByUniqueKey(DETAIL, key(alertId, field)).isPresent()) {
                throw new AlreadyProcessedException("Đã chọn " + field + " cho cảnh báo này");
            }
        }
        if (details.residentId() != null) {
            if (alert.residentId() != null) throw new AlreadyProcessedException("Cảnh báo đã gắn người cao tuổi");
            if (residents.map(api -> api.isFinalStatus(details.residentId())).orElse(false)) {
                throw new BusinessException(HttpStatus.CONFLICT, "FINAL_STATE", "Người cao tuổi đã ở trạng thái cuối");
            }
            monitoringLock.lock(details.residentId());
        }
        Instant now = time.now();
        for (var field : fields.entrySet()) {
            records.add(
                    DETAIL,
                    new EntityRef(AlertNotices.SOURCE_TYPE, alertId),
                    new NewRecordEvent<>(
                            Map.of(field.getKey(), field.getValue()),
                            null,
                            user.accountId(),
                            false,
                            now,
                            now,
                            now,
                            alert.floorAreaId(),
                            key(alertId, field.getKey())));
        }
        Long newFloor = null;
        if (details.floorAreaId() != null && alert.floorAreaId() == null) {
            newFloor = details.floorAreaId();
            Long assignee = alert.receiverId() != null
                    ? null
                    : responsibility.initialAssignee(null, newFloor, now).orElse(null);
            store.attachFloor(alertId, newFloor, assignee);
        }
        Long residentFloor = null;
        if (details.residentId() != null) {
            store.attachResident(alertId, details.residentId());
            residentFloor = responsibility.floorOf(details.residentId(), now).orElse(null);
        }
        var updated = store.findById(alertId).orElseThrow();
        var notices = AssistNotices.details(updated, newFloor, details.residentId(), residentFloor, now);
        if (!notices.isEmpty()) events.publishEvent(new AlertNoticeRequested(alertId, notices));
    }

    private BusinessException notOnShift() {
        return views.refused(HttpStatus.CONFLICT, "NOT_ON_SHIFT", "Bạn không trong ca, hãy gọi số liên hệ của viện");
    }

    private static String key(long alertId, String field) {
        return "ASSIST_DETAIL:" + alertId + ":" + field;
    }

    private long latestPress(long alertId) {
        return records.list(SharedKind.of("M05.ALERT_SOURCE", Map.class), new EntityRef("M05.ALERT", alertId)).stream()
                .filter(row -> PRESS_SOURCE.equals(row.content().get("sourceType")))
                .map(row -> ((Number) row.content().get("sourceId")).longValue())
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}
