package com.eldercare.backend.notification.internal.inbox.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.events.NotificationViewed;
import com.eldercare.backend.notification.api.spi.FamilyNotificationPort;
import com.eldercare.backend.notification.api.spi.NotifiableRelationshipView;
import com.eldercare.backend.notification.internal.delivery.persistence.DeliveryStore;
import com.eldercare.backend.notification.internal.delivery.service.NotificationStreams;
import com.eldercare.backend.notification.internal.domain.NotificationEventType;
import com.eldercare.backend.notification.internal.inbox.domain.InboxCursor;
import com.eldercare.backend.notification.internal.inbox.domain.InboxFilter;
import com.eldercare.backend.notification.internal.inbox.domain.InboxItem;
import com.eldercare.backend.notification.internal.inbox.persistence.InboxStore;
import com.eldercare.backend.notification.internal.persistence.NotificationStore;
import com.eldercare.backend.notification.internal.recipient.domain.ContentFilter;
import com.eldercare.backend.notification.internal.recipient.service.ResidentFacts;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Hộp thông báo của người đang đăng nhập (FR-033 → FR-036; research B-11): chỉ thông báo của chính mình (bảng FR-039);
 * nội dung theo quyền hiện hành, không theo lúc gửi (FR-034, Q-99): nhân viên còn phạm vi với người cao tuổi và còn quyền
 * xem nhóm thông tin tương ứng ({@code NotificationFieldClassification}: sức khỏe, chi phí); người thân theo quyền hiện
 * hành và quan hệ còn hiệu lực ({@link ContentFilter}). Không còn quyền thì chỉ tiêu đề, loại sự kiện, thời điểm.
 */
@Service
public class InboxService {

    /** Số thông báo mỗi trang (thông số kỹ thuật). */
    static final int PAGE_SIZE = 50;

    /** Một trang hộp thông báo. */
    public record Page(
            int unreadCount, int unacknowledgedUrgentCount, List<Opened> items, Optional<String> nextCursor) {}

    /** Thông báo kèm phần được hiện và phần bị ẩn theo quyền hiện hành. */
    public record Opened(InboxItem row, List<ContentPart> content, List<InformationType> hiddenParts) {}

    private final InboxStore inbox;
    private final NotificationStore notifications;
    private final DeliveryStore deliveries;
    private final NotificationStreams streams;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<ScopeApi> scope;
    private final ObjectProvider<FieldVisibilityApi> fields;
    private final ObjectProvider<FamilyNotificationPort> family;
    private final ResidentFacts facts;

    InboxService(
            InboxStore inbox,
            NotificationStore notifications,
            DeliveryStore deliveries,
            NotificationStreams streams,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<ScopeApi> scope,
            ObjectProvider<FieldVisibilityApi> fields,
            ObjectProvider<FamilyNotificationPort> family,
            ResidentFacts facts) {
        this.inbox = inbox;
        this.notifications = notifications;
        this.deliveries = deliveries;
        this.streams = streams;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
        this.scope = scope;
        this.fields = fields;
        this.family = family;
        this.facts = facts;
    }

    @Transactional(readOnly = true)
    public Page list(InboxFilter filter, String cursor) {
        CurrentUser user = currentUser.getObject();
        String key = recipientKey(user);
        List<InboxItem> rows = inbox.page(key, filter, decode(cursor), PAGE_SIZE + 1);
        boolean more = rows.size() > PAGE_SIZE;
        List<InboxItem> page = more ? rows.subList(0, PAGE_SIZE) : rows;
        int[] counts = inbox.counts(key);
        return new Page(
                counts[0],
                counts[1],
                page.stream().map(row -> shown(user, row)).toList(),
                more ? Optional.of(encode(page.getLast())) : Optional.empty());
    }

    /** Mở thông báo: lần đầu ghi thời điểm xem cho cả thông báo, các lần sau không ghi đè (FR-034, FR-044). */
    @Transactional
    public Opened open(long notificationId) {
        CurrentUser user = currentUser.getObject();
        InboxItem row = find(user, notificationId);
        if (row.viewedAt() == null) {
            Instant now = time.now();
            try {
                notifications.appendEvent(
                        notificationId,
                        NotificationEventType.VIEW,
                        Optional.empty(),
                        Optional.empty(),
                        null,
                        user.accountId(),
                        now);
                events.publishEvent(new NotificationViewed(
                        notificationId,
                        new SourceRecord(row.sourceRecordType(), row.sourceRecordId()),
                        row.recipientType(),
                        row.recipientId(),
                        now));
            } catch (DuplicateKeyException e) {
                // Hai lần mở đồng thời: lần kia đã ghi xem lần đầu
            }
            row = find(user, notificationId);
        }
        return shown(user, row);
    }

    /** Ẩn thông báo đã xem khỏi hộp của mình; bản ghi vẫn còn (FR-036). */
    @Transactional
    public void hide(long notificationId) {
        CurrentUser user = currentUser.getObject();
        InboxItem row = find(user, notificationId);
        if (row.viewedAt() == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "NOTIFICATION_NOT_VIEWED", "Chỉ ẩn được thông báo đã xem (FR-036)");
        }
        if (!row.hidden()) {
            notifications.appendEvent(
                    notificationId,
                    NotificationEventType.HIDE,
                    Optional.empty(),
                    Optional.empty(),
                    null,
                    user.accountId(),
                    time.now());
        }
    }

    /** Luồng SSE của tài khoản đang đăng nhập. */
    public SseEmitter stream() {
        return streams.open(currentUser.getObject().accountId());
    }

    /** Đăng ký hoặc cập nhật token FCM của thiết bị đang đăng nhập (research B-05). */
    @Transactional
    public void registerPushDevice(String app, String fcmToken) {
        CurrentUser user = currentUser.getObject();
        deliveries.registerToken(user.accountId(), user.deviceId(), app, fcmToken, time.now());
    }

    private InboxItem find(CurrentUser user, long notificationId) {
        return inbox.find(notificationId, recipientKey(user)).orElseThrow(NotFoundException::new);
    }

    private Opened shown(CurrentUser user, InboxItem row) {
        List<ContentPart> content = inbox.content(row.requestId());
        Set<InformationType> allowed = allowed(user, row, content);
        List<ContentPart> shown =
                content.stream().filter(p -> allowed.contains(p.type())).toList();
        Set<InformationType> hidden = new LinkedHashSet<>();
        content.stream()
                .map(ContentPart::type)
                .filter(t -> !allowed.contains(t))
                .forEach(hidden::add);
        return new Opened(row, shown, new ArrayList<>(hidden));
    }

    /** Loại thông tin được hiện lúc này: trong các phần được gửi, theo quyền hiện hành. */
    private Set<InformationType> allowed(CurrentUser user, InboxItem row, List<ContentPart> content) {
        Set<InformationType> allowed = EnumSet.noneOf(InformationType.class);
        if (row.identityOnly()) {
            return allowed;
        }
        allowed.addAll(row.visibleParts());
        return switch (row.recipientType()) {
            case STAFF -> staffAllowed(user, row, allowed);
            case FAMILY_MEMBER -> familyAllowed(row, allowed, content);
            case RESIDENT -> allowed;
        };
    }

    private Set<InformationType> staffAllowed(CurrentUser user, InboxItem row, Set<InformationType> allowed) {
        if (row.residentId() == null) {
            return allowed;
        }
        Target target = new Target("M13.NOTIFICATION", Long.toString(row.id()), row.residentId(), row.floorAreaId());
        if (!scope.getObject().inScope(user, target)) {
            return EnumSet.noneOf(InformationType.class);
        }
        FieldVisibilityApi visibility = fields.getObject();
        if (visibility.canView(user, InfoGroup.HEALTH, target) != Visibility.FULL) {
            allowed.remove(InformationType.HEALTH);
        }
        if (visibility.canView(user, InfoGroup.COSTS, target) != Visibility.FULL) {
            allowed.remove(InformationType.COSTS);
        }
        return allowed;
    }

    /** Người thân: quan hệ còn hiệu lực, quyền hiện hành và bản đồng ý Hiệu lực hiện hành (FR-013, DBR-03). */
    private Set<InformationType> familyAllowed(InboxItem row, Set<InformationType> allowed, List<ContentPart> content) {
        if (row.residentId() == null) {
            return allowed;
        }
        FamilyNotificationPort port = family.getIfAvailable();
        Optional<NotifiableRelationshipView> relation = port == null
                ? Optional.empty()
                : port.activeRelationships(row.residentId()).stream()
                        .filter(r -> r.familyMemberId() == row.recipientId())
                        .findFirst();
        if (relation.isEmpty()) {
            return EnumSet.noneOf(InformationType.class);
        }
        Set<InformationType> parts = EnumSet.noneOf(InformationType.class);
        content.forEach(p -> parts.add(p.type()));
        allowed.retainAll(ContentFilter.visibleParts(
                parts,
                relation.get().rights(),
                relation.get().representative(),
                true,
                facts.healthConsent(row.residentId(), row.recipientId())));
        return allowed;
    }

    /** Khóa người nhận của tài khoản đang đăng nhập ({@code notification_state.recipient_key}); không có hồ sơ thì 404. */
    public static String recipientKey(CurrentUser user) {
        long id = switch (user.type()) {
            case STAFF -> user.staffId().orElseThrow(NotFoundException::new);
            case FAMILY_MEMBER -> user.familyMemberId().orElseThrow(NotFoundException::new);
            case RESIDENT -> user.residentId().orElseThrow(NotFoundException::new);
        };
        return user.type().name() + ":" + id;
    }

    private static String encode(InboxItem row) {
        long micros = ChronoUnit.MICROS.between(Instant.EPOCH, row.sentAt());
        return (row.urgentUnacknowledged() ? 1 : 0) + "_" + micros + "_" + row.id();
    }

    private static InboxCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String[] parts = cursor.split("_");
        try {
            long micros = Long.parseLong(parts[1]);
            return new InboxCursor(
                    Integer.parseInt(parts[0]),
                    Instant.ofEpochSecond(micros / 1_000_000, micros % 1_000_000 * 1000),
                    Long.parseLong(parts[2]));
        } catch (RuntimeException e) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", "Khóa phân trang không hợp lệ");
        }
    }
}
