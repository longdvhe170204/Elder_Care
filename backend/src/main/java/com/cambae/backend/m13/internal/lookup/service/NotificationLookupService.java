package com.cambae.backend.m13.internal.lookup.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.api.NotificationLookupApi;
import com.cambae.backend.m13.api.dto.NotifiedPersonView;
import com.cambae.backend.m13.internal.lookup.domain.DeliveryError;
import com.cambae.backend.m13.internal.lookup.domain.HistoryFilter;
import com.cambae.backend.m13.internal.lookup.domain.SentNotification;
import com.cambae.backend.m13.internal.lookup.persistence.LookupStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.spi.AssignmentApi;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
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
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<AssignmentApi> assignments;

    NotificationLookupService(
            LookupStore store,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<AssignmentApi> assignments) {
        this.store = store;
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
                        List.of(),
                        r.flags().contains("MISSING_LEVEL")))
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
