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
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
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
    static final SharedKind<Map> SOURCE = SharedKind.of("M05.ALERT_SOURCE", Map.class);

    private final AlertStore alerts;
    private final PermissionApi permissions;
    private final ScopeApi scopes;
    private final AccountApi accounts;
    private final Optional<RecordEventApi> records;
    private final AlertLocationService locations;
    private final Optional<LocationStructureApi> structure;
    private final AlertHistory history;

    public AlertQueryService(
            AlertStore alerts,
            PermissionApi permissions,
            ScopeApi scopes,
            AccountApi accounts,
            @Autowired(required = false) RecordEventApi records,
            AlertLocationService locations,
            @Autowired(required = false) LocationStructureApi structure) {
        this.alerts = alerts;
        this.permissions = permissions;
        this.scopes = scopes;
        this.accounts = accounts;
        this.records = Optional.ofNullable(records);
        this.locations = locations;
        this.structure = Optional.ofNullable(structure);
        this.history = new AlertHistory(records);
    }

    /** Danh sách theo bộ lọc, đã giới hạn theo phạm vi; không kèm nguồn; có lệnh được phép để thẻ hiện nút ngay trên danh sách. */
    @Transactional(readOnly = true)
    public List<AlertDetails> list(
            Collection<AlertStatus> statuses,
            Long floorAreaId,
            Long residentId,
            Boolean maxEscalationReached,
            CurrentUser user) {
        var scope = scopes.filter(user, VIEW);
        var floors = new HashMap<Long, String>();
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
                .map(alert -> new AlertDetails(
                        alert,
                        assigneeName(alert),
                        List.of(),
                        allowedCommands(alert, user, null),
                        AlertDetails.Press.NONE,
                        alert.floorAreaId() == null
                                ? null
                                : floors.computeIfAbsent(alert.floorAreaId(), this::floorName),
                        List.of(),
                        mineOnly(alert, user),
                        null,
                        List.of()))
                .toList();
    }

    /** Chi tiết; ngoài phạm vi trả 404 như lệnh. */
    @Transactional(readOnly = true)
    public AlertDetails detail(long alertId, CurrentUser user) {
        var alert = alerts.findById(alertId).orElseThrow(NotFoundException::new);
        // Người gửi yêu cầu hỗ trợ luôn xem được yêu cầu của mình, kể cả khi tầng đã đổi ra ngoài phạm vi (đề xuất
        // 2026-11-23)
        boolean mine = Long.valueOf(user.accountId()).equals(alert.requesterId())
                && EmergencyAssistService.ALERT_TYPE.equals(alert.type());
        var view = permissions.hasPermission(user, VIEW, target(alert));
        if (!view.allowed() && !mine) {
            if (view.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xem cảnh báo");
        }
        var sources = sources(alertId);
        var rows = history.list(alertId);
        var helpers = helpers(alert);
        return new AlertDetails(
                alert,
                assigneeName(alert),
                sources,
                allowedCommands(alert, user, helpers),
                press(alert, sources).withLocation(locations.latest(alert).orElse(null)),
                alert.floorAreaId() == null ? null : floorName(alert.floorAreaId()),
                reports(alertId),
                assist(alert, mine, helpers, rows),
                alert.closedById() == null
                        ? null
                        : accounts.fullName(alert.closedById()).orElse(null),
                followUps(alertId));
    }

    /** Danh sách chỉ cần biết yêu cầu hỗ trợ là của người xem, không đọc người đến giúp, tin nhắn (đề xuất 2026-11-23). */
    private static AlertDetails.Assist mineOnly(AlertRecord alert, CurrentUser user) {
        boolean mine = EmergencyAssistService.ALERT_TYPE.equals(alert.type())
                && Long.valueOf(user.accountId()).equals(alert.requesterId());
        return mine ? new AlertDetails.Assist(true, false, List.of(), List.of()) : AlertDetails.Assist.NONE;
    }

    private List<Long> helpers(AlertRecord alert) {
        boolean pressed =
                SosService.ALERT_TYPE.equals(alert.type()) || EmergencyAssistService.ALERT_TYPE.equals(alert.type());
        if (!pressed) return List.of();
        return history.helpers(alert.id());
    }

    /** Phần yêu cầu hỗ trợ (đề xuất 2026-11-23): người gửi, dấu ngoài ca, người đến giúp, tin nhắn thêm. */
    @SuppressWarnings("unchecked")
    private AlertDetails.Assist assist(
            AlertRecord alert, boolean mine, List<Long> helpers, List<Map<String, Object>> rows) {
        boolean pressed =
                SosService.ALERT_TYPE.equals(alert.type()) || EmergencyAssistService.ALERT_TYPE.equals(alert.type());
        if (!pressed) return AlertDetails.Assist.NONE;
        var names = helpers.stream()
                .map(id -> accounts.fullName(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        var messages = rows.stream()
                .filter(row -> AlertHistory.SENDER_MESSAGE.equals(row.get("action")))
                .map(row -> {
                    var detail = (Map<String, Object>) row.get("detail");
                    Long by = detail.get("by") instanceof Number number ? number.longValue() : null;
                    return new AlertDetails.Message(
                            by == null ? null : accounts.fullName(by).orElse(null),
                            (String) detail.get("text"),
                            Instant.parse((String) detail.get("at")));
                })
                .toList();
        return new AlertDetails.Assist(mine, senderOffShift(alert), names, messages);
    }

    /** Lần bấm mới nhất của yêu cầu hỗ trợ mang dấu ngoài ca (dòng cũ không có trường này). */
    private boolean senderOffShift(AlertRecord alert) {
        if (!EmergencyAssistService.ALERT_TYPE.equals(alert.type()) || records.isEmpty()) return false;
        return records.get().list(EmergencyAssistService.PRESS, new EntityRef("M05.ALERT", alert.id())).stream()
                .max(java.util.Comparator.comparingLong(row -> row.id()))
                .map(row -> Boolean.TRUE.equals(row.content().offShift()))
                .orElse(false);
    }

    /** Kết quả theo dõi kèm họ tên người ghi (FR-087). */
    private List<AlertDetails.FollowUp> followUps(long alertId) {
        return new AlertFollowUps(records.orElse(null))
                .list(alertId).stream()
                        .map(row -> new AlertDetails.FollowUp(
                                row.content(),
                                row.at(),
                                accounts.fullName(row.recordedBy()).orElse(null)))
                        .toList();
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
            return new AlertDetails.Press(
                    pressedBy,
                    (String) details.get("eventType"),
                    (String) details.get("place"),
                    (String) details.get("placeNote"),
                    null);
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

    /**
     * Tên lệnh trong đường dẫn (glossary {@code allowedCommands}); chỉ các lệnh của US3 đã có route. {@code helpers} rỗng
     * (danh sách) thì không tính "Tôi đến giúp".
     */
    private Set<String> allowedCommands(AlertRecord alert, CurrentUser user, List<Long> helpers) {
        boolean manager = user.roles().contains(Role.NURSING_HOME_MANAGER);
        boolean handler = !manager
                && permissions.hasPermission(user, HANDLING, target(alert)).allowed();
        var status = alert.status();
        boolean open = Set.of(AlertStatus.NEW, AlertStatus.ESCALATED, AlertStatus.RECEIVED, AlertStatus.IN_PROGRESS)
                .contains(status);
        var commands = new LinkedHashSet<String>();
        long me = user.accountId();
        boolean assist = EmergencyAssistService.ALERT_TYPE.equals(alert.type());
        boolean sender = assist && Long.valueOf(me).equals(alert.requesterId());
        boolean responsible =
                Long.valueOf(me).equals(alert.assigneeId()) || Long.valueOf(me).equals(alert.receiverId());
        // Người gửi không thấy Tiếp nhận riêng: tiếp nhận làm dừng leo thang khi đang chờ người tới (đề xuất
        // 2026-11-23)
        if ((status == AlertStatus.NEW || status == AlertStatus.ESCALATED)
                && !sender
                && (handler || (manager && status == AlertStatus.ESCALATED && alert.escalationLevel() >= 2))) {
            commands.add("receive");
        }
        if (sender && open) {
            commands.add("assist-details");
            commands.add("send-message");
        }
        if (assist && open && (sender || responsible)) commands.add("change-floor");
        if (sender && alert.receiverId() == null && (status == AlertStatus.NEW || status == AlertStatus.ESCALATED)) {
            commands.add("withdraw");
        }
        boolean pressedAlert =
                SosService.ALERT_TYPE.equals(alert.type()) || EmergencyAssistService.ALERT_TYPE.equals(alert.type());
        if (pressedAlert && open && helpers != null && !sender && !responsible && !helpers.contains(me)) {
            commands.add("join-help");
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
        boolean shortcut = handler && AlertHandlingService.senderShortcut(alert, user);
        if (handler
                && (shortcut || (status == AlertStatus.IN_PROGRESS && AlertHandlingService.mayClose(alert, user)))) {
            commands.add("close");
        }
        if (open
                && !manager
                && user.roles().stream().anyMatch(AlertHandlingService.FOLLOWERS::contains)
                && permissions.hasPermission(user, VIEW, target(alert)).allowed()) {
            commands.add("follow-up");
        }
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
        boolean converter = handler
                && (user.roles().contains(Role.NURSING_STAFF)
                        || user.roles().contains(Role.FLOOR_MANAGER)
                        || user.roles().contains(Role.DOCTOR));
        if (converter && (status == AlertStatus.IN_PROGRESS || shortcut)) commands.add("convert-to-incident");
        if (converter
                && (status == AlertStatus.NEW || status == AlertStatus.ESCALATED || status == AlertStatus.RECEIVED)) {
            commands.add("activate-emergency");
        }
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

    /** Mô tả của các lần báo sau, ghi khi gộp vào cảnh báo thủ công đang mở ({@link AlertService}, FR-030). */
    @SuppressWarnings("unchecked")
    private List<AlertDetails.Report> reports(long alertId) {
        return history.list(alertId).stream()
                .filter(row -> AlertHistory.MERGED.equals(row.get("action")))
                .map(row -> {
                    var detail = (Map<String, Object>) row.get("detail");
                    Long by = detail.get("by") instanceof Number number ? number.longValue() : null;
                    return new AlertDetails.Report(
                            by == null ? null : accounts.fullName(by).orElse(null),
                            (String) detail.get("description"),
                            Instant.parse((String) detail.get("at")));
                })
                .toList();
    }

    private String floorName(long floorAreaId) {
        return structure
                .flatMap(api -> api.floor(floorAreaId))
                .map(floor -> floor.name())
                .orElse(null);
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
