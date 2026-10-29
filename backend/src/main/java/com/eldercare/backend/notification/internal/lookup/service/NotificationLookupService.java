package com.eldercare.backend.notification.internal.lookup.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.spi.AssignmentApi;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.NotificationLookupApi;
import com.eldercare.backend.notification.api.dto.NotifiedPersonView;
import com.eldercare.backend.notification.internal.lookup.domain.DeliveryError;
import com.eldercare.backend.notification.internal.lookup.domain.HistoryFilter;
import com.eldercare.backend.notification.internal.lookup.domain.SentNotification;
import com.eldercare.backend.notification.internal.lookup.persistence.LookupStore;
import com.eldercare.backend.notification.internal.urgent.persistence.CallReminderStore;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tra cứu thông báo đã gửi ({@code ThongBaoTraCuuApi}, {@code TraCuuService}; FR-035, FR-037, FR-038; research B-11):
 * dữ liệu cho module nguồn, lịch sử gửi (Quản lý viện toàn viện, Trưởng tầng tầng được giao; bảng FR-039), lỗi gửi (Quản lý
 * viện). Lượt nhắc gọi làm ở US1 (Phase 4) nên hiện rỗng.
 */
@Service
public class NotificationLookupService implements NotificationLookupApi {

    /** Một dòng lịch sử gửi kèm kết quả từng kênh. */
    public record HistoryEntry(SentNotification row, List<NotifiedPersonView.ChannelResult> channels) {}

    private final LookupStore store;
    private final CallReminderStore reminders;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<AssignmentApi> assignments;

    NotificationLookupService(
            LookupStore store,
            CallReminderStore reminders,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<AssignmentApi> assignments) {
        this.store = store;
        this.reminders = reminders;
        this.time = time;
        this.currentUser = currentUser;
        this.assignments = assignments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotifiedPersonView> bySource(String sourceRecordType, String sourceRecordId) {
        return store.bySource(sourceRecordType, sourceRecordId).stream()
                .map(r -> new NotifiedPersonView(
                        r.notificationId(),
                        r.recipientType(),
                        r.recipientId(),
                        r.basis(),
                        r.status(),
                        store.channels(r.notificationId()),
                        Optional.ofNullable(r.sentAt()),
                        Optional.ofNullable(r.viewedAt()),
                        Optional.ofNullable(r.acknowledgedAt()),
                        Optional.ofNullable(r.acknowledgementMethod()),
                        callReminders(r.notificationId()),
                        r.flags().contains("MISSING_LEVEL")))
                .toList();
    }

    /** Mọi lượt nhắc gọi của chuỗi chứa thông báo (cả lượt gọi người thân kế tiếp) và kết quả gọi (FR-031, 007 FR-048b). */
    private List<NotifiedPersonView.CallReminderSummary> callReminders(long notificationId) {
        return reminders.chainRemindersOf(notificationId).stream()
                .flatMap(id -> reminders.find(id).stream())
                .map(r -> new NotifiedPersonView.CallReminderSummary(
                        r.id(),
                        r.calleeType(),
                        r.calleeId(),
                        r.status(),
                        reminders.results(r.id()).stream()
                                .map(c -> new NotifiedPersonView.CallOutcome(c.result(), c.calledAt()))
                                .toList()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> viewedAt(long notificationId) {
        return store.viewedAt(notificationId);
    }

    /** Lịch sử gửi theo bộ lọc; Trưởng tầng chỉ thấy yêu cầu gắn tầng được giao lúc tra (FR-037). */
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(
            String sourceRecordType,
            String sourceRecordId,
            Long recipientId,
            Long floorAreaId,
            Instant from,
            Instant to) {
        CurrentUser user = currentUser.getObject();
        Optional<Collection<Long>> floors;
        if (user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            floors = Optional.empty();
        } else if (user.roles().contains(Role.FLOOR_MANAGER)) {
            floors = Optional.of(assignedFloors(user));
        } else {
            throw new PermissionDeniedException(
                    "Chỉ Quản lý viện, Trưởng tầng tra cứu lịch sử thông báo (FR-037, FR-039)");
        }
        return store
                .history(
                        new HistoryFilter(sourceRecordType, sourceRecordId, recipientId, floorAreaId, from, to, floors))
                .stream()
                .map(r -> new HistoryEntry(r, store.channels(r.notificationId())))
                .toList();
    }

    /** Danh sách lỗi gửi (FR-038): chỉ Quản lý viện. */
    @Transactional(readOnly = true)
    public List<DeliveryError> deliveryErrors(String type) {
        if (!currentUser.getObject().roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Chỉ Quản lý viện xem danh sách lỗi gửi (FR-038, FR-039)");
        }
        return store.errors(type);
    }

    private Collection<Long> assignedFloors(CurrentUser user) {
        AssignmentApi api = assignments.getIfAvailable();
        if (api == null || user.staffId().isEmpty()) {
            return List.of();
        }
        return api.activeAt(user.staffId().get(), time.now()).stream()
                .filter(a -> a.kind() == AssignmentKind.FLOOR_MANAGER)
                .map(AssignmentView::floorAreaId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
