package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
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
import com.eldercare.backend.incident.internal.alert.domain.AlertActions;
import com.eldercare.backend.incident.internal.alert.domain.AlertNoticeRequested;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.AssistDetails;
import com.eldercare.backend.incident.internal.alert.domain.AssistFloorChoice;
import com.eldercare.backend.incident.internal.alert.domain.AssistPressContent;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.SosStatusView;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.governance.domain.ResidentItemClosed;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private static final int PLACE_NOTE_MAX = 200;
    private static final int MESSAGE_MAX = 200;
    private static final Set<String> PRESSED_TYPES = Set.of(SosService.ALERT_TYPE, ALERT_TYPE);
    private static final Set<AlertStatus> OPEN =
            Set.of(AlertStatus.NEW, AlertStatus.ESCALATED, AlertStatus.RECEIVED, AlertStatus.IN_PROGRESS);
    private static final PermissionKey VIEW = new PermissionKey(Function.ALERT_HANDLING, Operation.VIEW);
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
    private final AuditLogApi audit;
    private final AlertHistory history;

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
            @Autowired(required = false) ResidentApi residents,
            AuditLogApi audit) {
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
        this.audit = audit;
        this.history = new AlertHistory(records);
    }

    public SosStatusView press(CurrentUser user, String idempotencyKey) {
        return press(user, idempotencyKey, AssistFloorChoice.NONE);
    }

    /**
     * Một lần bấm (FR-047i). Tầng xảy ra người bấm chọn (tầng đang có ca) hoặc khu vực chung; không chọn thì tầng theo ca
     * (đề xuất 2026-11-23). Người không trong ca vẫn gửi được, lần bấm mang dấu ngoài ca, không chọn tầng thì báo như khu
     * vực chung (đề xuất 2026-11-23, thay cho 409 cũ). Không có hồ sơ nhân viên hoặc nguồn ca thì 409
     * {@code NOT_ON_SHIFT} kèm số liên hệ.
     */
    @Transactional
    public SosStatusView press(CurrentUser user, String idempotencyKey, AssistFloorChoice choice) {
        // Lớp vai trò (Phụ lục 27 dòng Nút hỗ trợ khẩn cấp); lần bấm chưa có đối tượng nên không xét phạm vi. "Trong
        // ca"
        // (chú thích 49) chỉ còn quyết định tầng chọn sẵn và dấu ngoài ca (đề xuất 2026-11-23).
        if (!permissions.permissions(user).contains(BUTTON)) {
            throw new PermissionDeniedException("Không có quyền dùng nút hỗ trợ khẩn cấp");
        }
        Instant now = time.now();
        long staffId = user.staffId().orElseThrow(() -> notOnShift());
        if (staff.isEmpty()) throw notOnShift();
        var port = staff.get();
        boolean onShift = port.onShift(staffId, now);
        Long own = onShift ? port.floorDuringShift(staffId, now).orElse(null) : null;
        List<Long> withShift = port.floorsWithShift(now);
        Long floor = floorOf(choice == null ? AssistFloorChoice.NONE : choice, own, withShift);
        boolean noOnDutyDoctor = port.onDutyDoctors(now).isEmpty();
        List<Long> floorsWithShift = floor == null ? withShift : List.of();
        var alert = alerts.requestAlert(
                        new CreateAlertCommand(
                                null,
                                floor,
                                ALERT_TYPE,
                                TypeLock.forStaffAssist(user.accountId()),
                                IncidentLevel.CRITICAL,
                                now,
                                PRESS_SOURCE,
                                null,
                                "Nhân viên yêu cầu hỗ trợ khẩn cấp",
                                user.accountId(),
                                user.accountId()),
                        created -> AssistNotices.created(created, noOnDutyDoctor, floorsWithShift, now))
                .orElseThrow();
        // 007 T106 (chủ dự án chốt 2026-11-10): lần bấm gắn vào cảnh báo nó tạo hoặc gộp vào, xóa cùng cảnh báo theo
        // hạn
        // lưu giữ hồ sơ sức khỏe (R-15: dòng bảng chung mang entity_type của bản ghi cha).
        var press = records.add(
                PRESS,
                new EntityRef("M05.ALERT", alert.id()),
                new NewRecordEvent<>(
                        new AssistPressContent(user.accountId(), staffId, idempotencyKey, floor, !onShift),
                        null,
                        user.accountId(),
                        false,
                        now,
                        now,
                        now,
                        floor,
                        null));
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
        requireOpen(alert);
        var fields = new LinkedHashMap<String, Object>();
        if (details.eventType() != null)
            fields.put("eventType", details.eventType().name());
        if (details.floorAreaId() != null) fields.put("floorAreaId", details.floorAreaId());
        if (details.place() != null) fields.put("place", details.place().name());
        if (details.residentId() != null) fields.put("residentId", details.residentId());
        if (details.placeNote() != null && !details.placeNote().isBlank()) {
            String note = details.placeNote().trim();
            if (note.length() > PLACE_NOTE_MAX) throw new InvalidValueException("Nơi cần hỗ trợ dài quá 200 ký tự");
            fields.put("placeNote", note);
        }
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

    /**
     * Đổi tầng sau khi gửi (đề xuất 2026-11-23): người bấm hoặc người phụ trách, khi cảnh báo còn mở; tầng mới là tầng
     * đang có ca hoặc khu vực chung, khác tầng hiện tại. Báo thêm nhóm của tầng mới; chưa ai tiếp nhận thì người phụ trách
     * theo tầng mới.
     */
    @Transactional
    public void changeFloor(long alertId, AssistFloorChoice choice, CurrentUser user) {
        if (choice == null || choice.isEmpty()) throw new InvalidValueException("Chọn tầng hoặc khu vực chung");
        if (choice.commonArea() && choice.floorAreaId() != null) throw bothChosen();
        var alert = store.lock(alertId).orElseThrow(NotFoundException::new);
        if (!ALERT_TYPE.equals(alert.type())) throw new NotFoundException();
        long me = user.accountId();
        boolean allowed = Long.valueOf(me).equals(alert.requesterId())
                || Long.valueOf(me).equals(alert.assigneeId())
                || Long.valueOf(me).equals(alert.receiverId());
        if (!allowed) throw new PermissionDeniedException("Chỉ người gửi hoặc người phụ trách được đổi tầng");
        requireOpen(alert);
        Instant now = time.now();
        List<Long> withShift = staff.map(port -> port.floorsWithShift(now)).orElse(List.of());
        Long target = choice.commonArea() ? null : choice.floorAreaId();
        if (target != null && !withShift.contains(target)) throw floorWithoutShift();
        if (Objects.equals(target, alert.floorAreaId())) {
            throw new InvalidValueException("Tầng mới phải khác tầng hiện tại");
        }
        Long assignee = target == null
                ? null
                : responsibility.initialAssignee(null, target, now).orElse(null);
        store.changeFloor(alertId, target, assignee, alert.version());
        var detail = new HashMap<String, Object>();
        detail.put("by", me);
        detail.put("from", alert.floorAreaId());
        detail.put("to", target);
        history.add(alertId, AlertHistory.FLOOR_CHANGED, detail, now);
        write(AlertActions.FLOOR_CHANGED, alert, detail);
        var updated = store.findById(alertId).orElseThrow();
        events.publishEvent(new AlertNoticeRequested(
                alertId, AssistNotices.floorChanged(updated, target == null ? withShift : List.of(), now)));
    }

    /** Người bấm nhắn thêm tình hình khi yêu cầu còn mở (đề xuất 2026-11-23), tối đa 200 ký tự; báo người nhận. */
    @Transactional
    public void message(long alertId, String text, CurrentUser user) {
        String body = text == null ? "" : text.trim();
        if (body.isEmpty()) throw new InvalidValueException("Chưa nhập nội dung nhắn");
        if (body.length() > MESSAGE_MAX) throw new InvalidValueException("Nội dung nhắn dài quá 200 ký tự");
        var alert = ownAlert(alertId, user);
        requireOpen(alert);
        Instant now = time.now();
        history.add(
                alertId,
                AlertHistory.SENDER_MESSAGE,
                Map.of("by", user.accountId(), "text", body, "at", now.toString()),
                now,
                now.toEpochMilli() + ":" + user.accountId());
        events.publishEvent(
                new AlertNoticeRequested(alertId, AssistNotices.message(alert, body, leads(alert, now), now)));
    }

    /**
     * Người bấm rút yêu cầu khi chưa ai tiếp nhận (đề xuất 2026-11-23): đóng với kết quả "Người gửi rút yêu cầu", báo người
     * nhận thôi chạy tới. Đã có người tiếp nhận thì 409, người bấm nhắn thêm để người đó đóng.
     */
    @Transactional
    public void withdraw(long alertId, String reason, CurrentUser user) {
        String why = reason == null ? "" : reason.trim();
        if (why.length() > MESSAGE_MAX) throw new InvalidValueException("Lý do dài quá 200 ký tự");
        var alert = ownAlert(alertId, user);
        if (alert.receiverId() != null
                || (alert.status() != AlertStatus.NEW && alert.status() != AlertStatus.ESCALATED)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "ALREADY_RECEIVED",
                    "Đã có người tiếp nhận, hãy nhắn thêm để người đó đóng yêu cầu");
        }
        Instant now = time.now();
        String resolution = why.isEmpty() ? "Người gửi rút yêu cầu" : "Người gửi rút yêu cầu: " + why;
        store.withdraw(alertId, resolution, user.accountId(), now, alert.version());
        history.add(alertId, AlertHistory.WITHDRAWN, Map.of("by", user.accountId()), now);
        write(AlertActions.WITHDRAWN, alert, Map.of("status", AlertStatus.CLOSED.name()));
        events.publishEvent(new AlertNoticeRequested(alertId, AssistNotices.withdrawn(alert, leads(alert, now), now)));
        if (alert.residentId() != null) events.publishEvent(new ResidentItemClosed(alert.residentId()));
    }

    /**
     * "Tôi đến giúp" (đề xuất 2026-11-23): người xem được cảnh báo từ nút SOS, nút hỗ trợ đang mở báo mình đang tới phụ;
     * người lo chính vẫn là người tiếp nhận. Không đổi trạng thái; một lần mỗi người; không áp cho người gửi, người phụ
     * trách.
     */
    @Transactional
    public void joinHelp(long alertId, CurrentUser user) {
        var alert = store.lock(alertId).orElseThrow(NotFoundException::new);
        if (!PRESSED_TYPES.contains(alert.type())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "NOT_PRESSED_ALERT",
                    "Chỉ cảnh báo từ nút SOS hoặc nút hỗ trợ có Tôi đến giúp");
        }
        long me = user.accountId();
        if (Long.valueOf(me).equals(alert.requesterId())) {
            throw new InvalidValueException("Bạn là người gửi yêu cầu này");
        }
        var view = permissions.hasPermission(user, VIEW, target(alert));
        if (!view.allowed()) {
            if (view.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xem cảnh báo");
        }
        requireOpen(alert);
        if (Long.valueOf(me).equals(alert.receiverId()) || Long.valueOf(me).equals(alert.assigneeId())) {
            throw new InvalidValueException("Bạn đang phụ trách yêu cầu này");
        }
        if (history.helpers(alertId).contains(me)) throw new AlreadyProcessedException("Bạn đã báo đến giúp");
        history.add(alertId, AlertHistory.HELPER_JOINED, Map.of("by", me), time.now(), Long.toString(me));
    }

    /** Tầng của lần bấm: khu vực chung → rỗng; không chọn → tầng theo ca; chọn → phải là tầng đang có ca. */
    private static Long floorOf(AssistFloorChoice choice, Long own, List<Long> withShift) {
        if (choice.commonArea() && choice.floorAreaId() != null) throw bothChosen();
        if (choice.commonArea()) return null;
        Long chosen = choice.floorAreaId();
        if (chosen == null) return own;
        if (!chosen.equals(own) && !withShift.contains(chosen)) throw floorWithoutShift();
        return chosen;
    }

    private static InvalidValueException bothChosen() {
        return new InvalidValueException("Chọn một tầng hoặc khu vực chung, không chọn cả hai");
    }

    private static InvalidValueException floorWithoutShift() {
        return new InvalidValueException("Tầng này không có ca đang diễn ra, hãy chọn khu vực chung");
    }

    private AlertRecord ownAlert(long alertId, CurrentUser user) {
        var alert = store.lock(alertId).orElseThrow(NotFoundException::new);
        if (!ALERT_TYPE.equals(alert.type()) || !Long.valueOf(user.accountId()).equals(alert.requesterId())) {
            throw new NotFoundException();
        }
        return alert;
    }

    private static void requireOpen(AlertRecord alert) {
        if (!OPEN.contains(alert.status())) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Cảnh báo đã kết thúc");
        }
    }

    /** Tầng đang có ca khi cảnh báo ở khu vực chung (người nhận là Người phụ trách ca các tầng đó). */
    private List<Long> leads(AlertRecord alert, Instant now) {
        if (alert.floorAreaId() != null) return List.of();
        return staff.map(port -> port.floorsWithShift(now)).orElse(List.of());
    }

    private static Target target(AlertRecord alert) {
        return new Target(AlertNotices.SOURCE_TYPE, Long.toString(alert.id()), alert.residentId(), alert.floorAreaId());
    }

    private void write(AuditAction action, AlertRecord alert, Map<String, Object> after) {
        var before = new HashMap<String, Object>();
        before.put("status", alert.status().name());
        before.put("floorAreaId", alert.floorAreaId());
        audit.write(AuditLogEntry.builder()
                .action(action)
                .entity(AlertNotices.SOURCE_TYPE, alert.id())
                .resident(alert.residentId())
                .before(before)
                .after(after)
                .build());
    }

    private BusinessException notOnShift() {
        return views.refused(
                HttpStatus.CONFLICT,
                "NOT_ON_SHIFT",
                "Tài khoản chưa có hồ sơ nhân viên để gửi, hãy gọi số liên hệ của viện");
    }

    private static String key(long alertId, String field) {
        return "ASSIST_DETAIL:" + alertId + ":" + field;
    }

    private long latestPress(long alertId) {
        return latestPress(records, alertId);
    }

    /** Lần bấm mới nhất gắn vào cảnh báo (cha là cảnh báo từ 2026-11-10). */
    static long latestPress(RecordEventApi records, long alertId) {
        return records.list(PRESS, new EntityRef("M05.ALERT", alertId)).stream()
                .mapToLong(row -> row.id())
                .max()
                .orElseThrow();
    }
}
