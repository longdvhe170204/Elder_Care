package com.eldercare.backend.notification.internal.urgent.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.incident.api.AlertApi;
import com.eldercare.backend.incident.api.AlertCommandApi;
import com.eldercare.backend.incident.api.IncidentApi;
import com.eldercare.backend.incident.api.IncidentCommandApi;
import com.eldercare.backend.notification.internal.inbox.domain.InboxItem;
import com.eldercare.backend.notification.internal.inbox.persistence.InboxStore;
import com.eldercare.backend.notification.internal.inbox.service.InboxService;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * "Xác nhận và tiếp nhận" thông báo Khẩn cấp (009 T021, FR-026a, Q-102, research B-07): hai bản ghi riêng. Xác nhận ghi
 * trước trong transaction của {@link UrgentAcknowledgement} và luôn còn; tiếp nhận chạy ở transaction thứ hai qua
 * {@link AlertCommandApi} hoặc {@link IncidentCommandApi} của 007 (bản thật {@code MANDATORY}, tự kiểm quyền, trạng thái).
 * 007 từ chối thì trả {@code REJECTED} kèm mã lỗi, không hoàn tác xác nhận. Nguồn không phải cảnh báo, sự cố hoặc 007 chưa
 * có bean thì {@code SOURCE_UNAVAILABLE}.
 */
@Service
public class UrgentTakeCharge {

    static final String ALERT = "M05.ALERT";
    static final String INCIDENT = "M05.INCIDENT";
    static final String SOURCE_UNAVAILABLE = "SOURCE_UNAVAILABLE";
    private static final Set<String> RECEIVABLE_ALERT = Set.of("NEW", "ESCALATED");
    private static final Set<String> RECEIVABLE_INCIDENT = Set.of("NEW");

    public record Result(boolean succeeded, String errorCode, Map<String, Object> details) {
        static Result rejected(String errorCode, Map<String, Object> details) {
            return new Result(false, errorCode, details);
        }
    }

    private final UrgentAcknowledgement acknowledgement;
    private final InboxStore inbox;
    private final ObjectProvider<CurrentUser> currentUser;
    private final PermissionApi permissions;
    private final ObjectProvider<AlertCommandApi> alertCommands;
    private final ObjectProvider<IncidentCommandApi> incidentCommands;
    private final ObjectProvider<AlertApi> alerts;
    private final ObjectProvider<IncidentApi> incidents;
    private final TransactionTemplate transaction;

    UrgentTakeCharge(
            UrgentAcknowledgement acknowledgement,
            InboxStore inbox,
            ObjectProvider<CurrentUser> currentUser,
            PermissionApi permissions,
            ObjectProvider<AlertCommandApi> alertCommands,
            ObjectProvider<IncidentCommandApi> incidentCommands,
            ObjectProvider<AlertApi> alerts,
            ObjectProvider<IncidentApi> incidents,
            TransactionTemplate transaction) {
        this.acknowledgement = acknowledgement;
        this.inbox = inbox;
        this.currentUser = currentUser;
        this.permissions = permissions;
        this.alertCommands = alertCommands;
        this.incidentCommands = incidentCommands;
        this.alerts = alerts;
        this.incidents = incidents;
        this.transaction = transaction;
    }

    /** Không đặt transaction ngoài: xác nhận phải commit trước khi gọi tiếp nhận (FR-026a). */
    public Result acknowledgeAndTakeCharge(long notificationId) {
        acknowledgement.acknowledge(notificationId);
        CurrentUser user = currentUser.getObject();
        InboxItem row =
                inbox.find(notificationId, InboxService.recipientKey(user)).orElseThrow(NotFoundException::new);
        Optional<Long> sourceId = sourceId(row);
        if (sourceId.isEmpty()) return Result.rejected(SOURCE_UNAVAILABLE, Map.of());
        try {
            boolean called = Boolean.TRUE.equals(transaction.execute(status -> takeCharge(row, sourceId.get(), user)));
            return called ? new Result(true, null, Map.of()) : Result.rejected(SOURCE_UNAVAILABLE, Map.of());
        } catch (BusinessException e) {
            return Result.rejected(e.errorCode(), e.properties());
        }
    }

    private boolean takeCharge(InboxItem row, long sourceId, CurrentUser user) {
        if (ALERT.equals(row.sourceRecordType())) {
            AlertCommandApi api = alertCommands.getIfAvailable();
            if (api == null) return false;
            api.acknowledge(sourceId, user);
            return true;
        }
        IncidentCommandApi api = incidentCommands.getIfAvailable();
        if (api == null) return false;
        api.acknowledge(sourceId, user);
        return true;
    }

    /**
     * Nút "Xác nhận và tiếp nhận" (research B-07): Khẩn cấp chưa xác nhận, nguồn là cảnh báo Mới/Leo thang hoặc sự cố Mới,
     * người nhận có quyền xử lý nguồn (Phụ lục 27 "Xử lý cảnh báo", "Sự cố, khẩn cấp" thao tác Sửa). Điều kiện riêng của
     * 007 (vai trò tiếp nhận sự cố, Quản lý viện ở cấp leo thang 2) do 007 kiểm khi bấm; từ chối trả {@code REJECTED}.
     */
    public boolean available(InboxItem row) {
        if (!row.urgentUnacknowledged()) return false;
        Optional<Long> sourceId = sourceId(row);
        if (sourceId.isEmpty()) return false;
        CurrentUser user = currentUser.getObject();
        long id = sourceId.get();
        if (ALERT.equals(row.sourceRecordType())) {
            AlertApi api = alerts.getIfAvailable();
            return api != null
                    && api.view(id)
                            .filter(a -> RECEIVABLE_ALERT.contains(a.status()))
                            .map(a ->
                                    allowed(user, Function.ALERT_HANDLING, ALERT, id, a.residentId(), a.floorAreaId()))
                            .orElse(false);
        }
        IncidentApi api = incidents.getIfAvailable();
        return api != null
                && api.view(id)
                        .filter(i -> RECEIVABLE_INCIDENT.contains(i.status()))
                        .map(i -> allowed(
                                user, Function.INCIDENT_EMERGENCY, INCIDENT, id, i.residentId(), i.floorAreaId()))
                        .orElse(false);
    }

    private boolean allowed(
            CurrentUser user, Function function, String entity, long id, Long residentId, Long floorAreaId) {
        return permissions
                .hasPermission(
                        user,
                        new PermissionKey(function, Operation.UPDATE),
                        new Target(entity, Long.toString(id), residentId, floorAreaId))
                .allowed();
    }

    private static Optional<Long> sourceId(InboxItem row) {
        if (row.level() != NotificationLevel.URGENT
                || !(ALERT.equals(row.sourceRecordType()) || INCIDENT.equals(row.sourceRecordType()))) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(row.sourceRecordId()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
