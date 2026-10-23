package com.cambae.backend.m13.internal.quiethours.service;

import com.cambae.backend.common.notification.FamilyRight;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.api.spi.FamilyContactView;
import com.cambae.backend.m13.api.spi.FamilyNotificationPort;
import com.cambae.backend.m13.api.spi.NotifiableRelationshipView;
import com.cambae.backend.m13.internal.delivery.service.DeliveryDispatcher;
import com.cambae.backend.m13.internal.delivery.service.OutOfAppPlanner;
import com.cambae.backend.m13.internal.domain.NotificationEventType;
import com.cambae.backend.m13.internal.job.service.M13FastTask;
import com.cambae.backend.m13.internal.persistence.NotificationStore;
import com.cambae.backend.m13.internal.quiethours.persistence.HeldNotificationStore;
import com.cambae.backend.m13.internal.quiethours.persistence.HeldNotificationStore.Held;
import com.cambae.backend.m13.internal.recipient.domain.ConsentScope;
import com.cambae.backend.m13.internal.recipient.domain.ContentFilter;
import com.cambae.backend.m13.internal.recipient.service.RecipientPhones;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Giải phóng thông báo hết giờ yên tĩnh (việc 20 giây {@code RELEASE_QUIET_HOURS}; FR-025, research B-06): theo thứ tự
 * tiếp nhận, mỗi thông báo một transaction, xét lại người nhận lúc gửi rồi ghi Đã gửi kèm các lần gửi theo kênh
 * ({@link OutOfAppPlanner}; tin nhắn Trung bình vào khung gộp, FR-016b), hoặc Không gửi kèm lý do.
 *
 * <p>Chưa có, chờ 001 (docs/team-plan.md mục 8): người cao tuổi đã Qua đời (FR-015, lý do {@code RESIDENT_DECEASED}) và
 * bản đồng ý hiện hành; tới đó phạm vi đồng ý xét như lúc tiếp nhận.
 */
@Service
public class QuietHoursRelease implements M13FastTask {

    static final String NO_LONGER_ELIGIBLE = "RECIPIENT_NO_LONGER_ELIGIBLE";
    private static final int BATCH = 200;

    private final HeldNotificationStore held;
    private final NotificationStore notifications;
    private final DeliveryDispatcher dispatcher;
    private final OutOfAppPlanner planner;
    private final RecipientPhones phones;
    private final ObjectProvider<AccountApi> accounts;
    private final ObjectProvider<FamilyNotificationPort> family;
    private final TransactionTemplate tx;
    private final BusinessTime time;

    QuietHoursRelease(
            HeldNotificationStore held,
            NotificationStore notifications,
            DeliveryDispatcher dispatcher,
            OutOfAppPlanner planner,
            RecipientPhones phones,
            ObjectProvider<AccountApi> accounts,
            ObjectProvider<FamilyNotificationPort> family,
            TransactionTemplate tx,
            BusinessTime time) {
        this.held = held;
        this.notifications = notifications;
        this.dispatcher = dispatcher;
        this.planner = planner;
        this.phones = phones;
        this.accounts = accounts;
        this.family = family;
        this.tx = tx;
        this.time = time;
    }

    @Override
    public String name() {
        return "RELEASE_QUIET_HOURS";
    }

    @Override
    public int run() {
        Instant now = time.now();
        int released = 0;
        for (long id : held.due(now, BATCH)) {
            Boolean done = tx.execute(s -> release(id, now));
            if (Boolean.TRUE.equals(done)) {
                released++;
            }
        }
        return released;
    }

    private boolean release(long id, Instant now) {
        Optional<Held> found = held.lock(id).filter(h -> !h.scheduledSendAt().isAfter(now));
        if (found.isEmpty()) {
            return false;
        }
        Held h = found.get();
        Optional<Optional<Long>> account = switch (h.recipientType()) {
            case RESIDENT -> residentAccount(h);
            case FAMILY_MEMBER -> familyAccount(h);
            case STAFF -> Optional.of(Optional.empty());
        };
        if (account.isEmpty()) {
            notifications.appendEvent(
                    id,
                    NotificationEventType.NOT_SENT,
                    Optional.empty(),
                    Optional.empty(),
                    NO_LONGER_ELIGIBLE,
                    null,
                    now);
            return true;
        }
        notifications.markSent(id, now);
        dispatcher.dispatch(planner.plan(
                new OutOfAppPlanner.Target(
                        id,
                        h.recipientType(),
                        h.recipientId(),
                        account.get().isPresent(),
                        phones.of(h.recipientType(), h.recipientId(), h.residentId())),
                h.level(),
                h.invalidOutOfAppContent(),
                now));
        return true;
    }

    /** Người cao tuổi còn tài khoản Hoạt động hoặc Khóa tạm; rỗng khi không còn đủ điều kiện. */
    private Optional<Optional<Long>> residentAccount(Held h) {
        return accounts.getObject()
                .byResident(h.recipientId())
                .filter(a -> a.status() == AccountStatus.ACTIVE || a.status() == AccountStatus.TEMPORARILY_LOCKED)
                .map(a -> Optional.of(a.accountId()));
    }

    /**
     * Người thân còn thuộc ít nhất một nhóm đã gửi tới họ (quan hệ, người liên hệ chính, người đại diện, quyền), qua phạm
     * vi đồng ý và còn ít nhất một phần nội dung được xem (FR-013, FR-014, FR-025); trả tài khoản cổng hiện hành nếu có.
     */
    private Optional<Optional<Long>> familyAccount(Held h) {
        FamilyNotificationPort port = family.getIfAvailable();
        if (port == null) {
            return Optional.empty();
        }
        Optional<NotifiableRelationshipView> rel = h.residentId() == null
                ? Optional.empty()
                : port.activeRelationships(h.residentId()).stream()
                        .filter(r -> r.familyMemberId() == h.recipientId())
                        .findFirst();
        if (h.basis().stream().noneMatch(b -> stillHolds(b, rel))) {
            return Optional.empty();
        }
        ConsentScope.Decision decision =
                ConsentScope.decide(h.informationSubject().orElse(null), false, h.hasQ354Case(), h.level());
        if (decision == ConsentScope.Decision.SKIP || decision == ConsentScope.Decision.MISSING_CASE) {
            return Optional.empty();
        }
        Set<InformationType> visible = ContentFilter.visibleParts(
                h.requestParts(),
                rel.map(NotifiableRelationshipView::rights).orElse(Set.of()),
                rel.map(NotifiableRelationshipView::representative).orElse(false),
                rel.isPresent(),
                false);
        if (visible.isEmpty()) {
            return Optional.empty();
        }
        Optional<Long> portal = rel.isPresent()
                ? rel.filter(NotifiableRelationshipView::portalAccountActive)
                        .flatMap(NotifiableRelationshipView::portalAccountId)
                : port.contact(h.recipientId())
                        .filter(FamilyContactView::portalAccountActive)
                        .flatMap(FamilyContactView::portalAccountId);
        return Optional.of(portal);
    }

    /** Căn cứ người nhận lúc tiếp nhận (như {@code RecipientResolver}) còn đúng với quan hệ hiện hành không. */
    private static boolean stillHolds(String basis, Optional<NotifiableRelationshipView> rel) {
        if (basis.startsWith("FamilyMember:")) {
            return true;
        }
        if (basis.startsWith("FamilyWithRight:")) {
            FamilyRight right = FamilyRight.valueOf(basis.substring("FamilyWithRight:".length()));
            return rel.filter(r -> r.rights().contains(right)).isPresent();
        }
        return switch (basis) {
            case "PrimaryContact" ->
                rel.filter(NotifiableRelationshipView::primaryContact).isPresent();
            case "Representative" ->
                rel.filter(NotifiableRelationshipView::representative).isPresent();
            default -> false;
        };
    }
}
