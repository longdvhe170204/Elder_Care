package com.cambae.backend.m13.internal.urgent.service;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.api.spi.FamilyContactView;
import com.cambae.backend.m13.api.spi.FamilyNotificationPort;
import com.cambae.backend.m13.internal.recipient.service.RecipientPhones;
import com.cambae.backend.m13.internal.urgent.persistence.CallReminderStore;
import com.cambae.backend.m13.internal.urgent.persistence.CallReminderStore.Reminder;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.spi.StaffProfileApi;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lượt nhắc gọi để xem ({@code GET /me/call-reminders}, {@code GET /call-reminders/{id}}; FR-027, FR-027a, FR-029,
 * FR-030a): lượt Chờ kết quả tôi đang được nhắc hoặc có quyền gọi; chi tiết cho người có quyền gọi hoặc người từng được
 * nhắc, số điện thoại người cần gọi lấy hiện hành lúc mở (FR-027). Quan hệ người thân chưa có nguồn nhãn (012) nên để trống.
 */
@Service
public class CallReminderQueries {

    /** Người cần gọi với số hiện hành. */
    public record PersonToCall(RecipientType type, String fullName, String relationship, String phoneNumber) {}

    public record ContentView(Instant occurredAt, String content) {}

    public record HandoffView(
            Long previousStaffId,
            long remindedStaffId,
            String tier,
            String reason,
            Instant dueAt,
            Instant occurredAt) {}

    public record ResultView(
            String result,
            Instant calledAt,
            String calledPhoneNumber,
            String reason,
            String answeredBy,
            String note,
            boolean supplementary) {}

    public record CallReminderView(
            long id,
            long callChainId,
            PersonToCall personToCall,
            Long residentId,
            List<ContentView> contentToReport,
            String currentlyReminded,
            Instant dueAt,
            String status,
            String stopReason,
            List<HandoffView> handoffs,
            List<ResultView> results,
            boolean canRecordResult) {}

    private final CallReminderStore store;
    private final CallReminderRouting routing;
    private final RecipientPhones phones;
    private final ObjectProvider<StaffProfileApi> profiles;
    private final ObjectProvider<FamilyNotificationPort> family;
    private final ObjectProvider<CurrentUser> currentUser;

    CallReminderQueries(
            CallReminderStore store,
            CallReminderRouting routing,
            RecipientPhones phones,
            ObjectProvider<StaffProfileApi> profiles,
            ObjectProvider<FamilyNotificationPort> family,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.routing = routing;
        this.phones = phones;
        this.profiles = profiles;
        this.family = family;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<CallReminderView> mine() {
        CurrentUser user = currentUser.getObject();
        return store.awaiting().stream()
                .filter(r -> user.staffId().equals(Optional.of(r.currentStaffId()))
                        || routing.canCall(user, r.floorAreaId()))
                .map(r -> view(r, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public CallReminderView detail(long id) {
        CurrentUser user = currentUser.getObject();
        Reminder r = store.find(id).orElseThrow(NotFoundException::new);
        boolean reminded = user.staffId()
                .map(s -> store.handoffs(id).stream().anyMatch(h -> h.remindedStaffId() == s))
                .orElse(false);
        if (!reminded && !routing.canCall(user, r.floorAreaId())) {
            throw new PermissionDeniedException(
                    "Chỉ người được nhắc hoặc người có quyền gọi xem lượt nhắc gọi (FR-029)");
        }
        return view(r, user);
    }

    private CallReminderView view(Reminder r, CurrentUser user) {
        boolean open = "AWAITING_RESULT".equals(r.status())
                || ("RESULT_RECORDED".equals(r.status()) && "NOT_REACHED".equals(r.finalResult()));
        return new CallReminderView(
                r.id(),
                r.chainId(),
                new PersonToCall(
                        r.calleeType(),
                        name(r.calleeType(), r.calleeId()),
                        null,
                        phones.of(r.calleeType(), r.calleeId(), r.residentId()).orElse(null)),
                r.residentId(),
                store.chainContent(r.chainId()).stream()
                        .map(c -> new ContentView(c.occurredAt(), c.content()))
                        .toList(),
                name(RecipientType.STAFF, r.currentStaffId()),
                r.dueAt(),
                r.status(),
                r.stopReason(),
                store.handoffs(r.id()).stream()
                        .map(h -> new HandoffView(
                                h.previousStaffId(),
                                h.remindedStaffId(),
                                h.tier().name(),
                                h.reason(),
                                h.dueAt(),
                                h.at()))
                        .toList(),
                store.results(r.id()).stream()
                        .map(c -> new ResultView(
                                c.result(),
                                c.calledAt(),
                                c.calledPhoneNumber(),
                                c.reason(),
                                c.answeredBy(),
                                c.note(),
                                c.supplementary()))
                        .toList(),
                open && routing.canCall(user, r.floorAreaId()));
    }

    private String name(RecipientType type, long id) {
        if (type == RecipientType.STAFF) {
            return Optional.ofNullable(profiles.getIfAvailable())
                    .flatMap(p -> p.fullName(id))
                    .orElse(null);
        }
        return Optional.ofNullable(family.getIfAvailable())
                .flatMap(p -> p.contact(id))
                .map(FamilyContactView::fullName)
                .orElse(null);
    }
}
