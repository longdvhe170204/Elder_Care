package com.eldercare.backend.notification.internal.quiethours.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.api.spi.FamilyContactView;
import com.eldercare.backend.notification.api.spi.FamilyNotificationPort;
import com.eldercare.backend.notification.api.spi.NotifiableRelationshipView;
import com.eldercare.backend.notification.internal.delivery.service.DeliveryDispatcher;
import com.eldercare.backend.notification.internal.delivery.service.OutOfAppPlanner;
import com.eldercare.backend.notification.internal.domain.NotificationEventType;
import com.eldercare.backend.notification.internal.job.service.M13FastTask;
import com.eldercare.backend.notification.internal.persistence.NotificationStore;
import com.eldercare.backend.notification.internal.quiethours.persistence.HeldNotificationStore;
import com.eldercare.backend.notification.internal.quiethours.persistence.HeldNotificationStore.Held;
import com.eldercare.backend.notification.internal.recipient.domain.ConsentScope;
import com.eldercare.backend.notification.internal.recipient.domain.ContentFilter;
import com.eldercare.backend.notification.internal.recipient.service.RecipientPhones;
import com.eldercare.backend.notification.internal.recipient.service.ResidentFacts;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Giải phóng thông báo hết giờ yên tĩnh (việc 20 giây {@code RELEASE_QUIET_HOURS}; FR-025, research B-06): theo thứ tự
 * tiếp nhận, mỗi thông báo một transaction, xét lại người nhận lúc gửi rồi ghi Đã gửi kèm các lần gửi theo kênh
 * ({@link OutOfAppPlanner}; tin nhắn Trung bình vào khung gộp, FR-016b), hoặc Không gửi kèm lý do: người cao tuổi đã Qua
 * đời ({@code RESIDENT_DECEASED}, FR-015, trừ việc sau qua đời của 004), người nhận không còn đủ điều kiện theo quan hệ,
 * quyền và bản đồng ý hiện hành.
 */
@Service
public class QuietHoursRelease implements M13FastTask {

    static final String NO_LONGER_ELIGIBLE = "RECIPIENT_NO_LONGER_ELIGIBLE";
    static final String RESIDENT_DECEASED = "RESIDENT_DECEASED";
    private static final int BATCH = 200;

    private final HeldNotificationStore held;
    private final NotificationStore notifications;
    private final DeliveryDispatcher dispatcher;
    private final OutOfAppPlanner planner;
    private final RecipientPhones phones;
    private final ObjectProvider<AccountApi> accounts;
    private final ObjectProvider<FamilyNotificationPort> family;
    private final ResidentFacts facts;
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
            ResidentFacts facts,
            TransactionTemplate tx,
            BusinessTime time) {
        this.held = held;
        this.notifications = notifications;
        this.dispatcher = dispatcher;
        this.planner = planner;
        this.phones = phones;
        this.accounts = accounts;
        this.family = family;
        this.facts = facts;
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

    /**
     * Người cao tuổi vừa Qua đời: thông báo người thân đang giữ về người đó chuyển Không gửi ngay (US4 #6), trừ việc sau qua
     * đời của 004.
     */
    public void cancelForDeath(long residentId) {
        Instant now = time.now();
        for (long id : held.heldForFamilyOf(residentId)) {
            tx.executeWithoutResult(s ->
                    held.lock(id).filter(h -> !h.afterDeath()).ifPresent(h -> notSent(id, RESIDENT_DECEASED, now)));
        }
    }

    private boolean release(long id, Instant now) {
        Optional<Held> found = held.lock(id).filter(h -> !h.scheduledSendAt().isAfter(now));
        if (found.isEmpty()) {
            return false;
        }
        Held h = found.get();
        if (h.recipientType() == RecipientType.FAMILY_MEMBER
                && h.residentId() != null
                && !h.afterDeath()
                && facts.deathAt(h.residentId()).isPresent()) {
            notSent(id, RESIDENT_DECEASED, now);
            return true;
        }
        Optional<Optional<Long>> account = switch (h.recipientType()) {
            case RESIDENT -> residentAccount(h);
            case FAMILY_MEMBER -> familyAccount(h);
            case STAFF -> Optional.of(Optional.empty());
        };
        if (account.isEmpty()) {
            notSent(id, NO_LONGER_ELIGIBLE, now);
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

    private void notSent(long id, String reason, Instant now) {
        notifications.appendEvent(
                id, NotificationEventType.NOT_SENT, Optional.empty(), Optional.empty(), reason, null, now);
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
        ConsentScope.Decision decision = ConsentScope.decide(
                h.informationSubject().orElse(null),
                rel.isPresent() && facts.notificationScope(h.residentId()),
                h.hasQ354Case(),
                h.level());
        if (decision == ConsentScope.Decision.SKIP || decision == ConsentScope.Decision.MISSING_CASE) {
            return Optional.empty();
        }
        Set<InformationType> visible = ContentFilter.visibleParts(
                h.requestParts(),
                rel.map(NotifiableRelationshipView::rights).orElse(Set.of()),
                rel.map(NotifiableRelationshipView::representative).orElse(false),
                rel.isPresent(),
                rel.isPresent() && facts.healthConsent(h.residentId(), h.recipientId()));
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
