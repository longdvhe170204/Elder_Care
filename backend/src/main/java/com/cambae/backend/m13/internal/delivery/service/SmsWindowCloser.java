package com.cambae.backend.m13.internal.delivery.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.LocationStructureApi;
import com.cambae.backend.m09.api.ShiftApi;
import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.internal.delivery.domain.ChannelSelection;
import com.cambae.backend.m13.internal.delivery.domain.SendResult;
import com.cambae.backend.m13.internal.delivery.domain.SmsBatching;
import com.cambae.backend.m13.internal.delivery.persistence.DeliveryStore;
import com.cambae.backend.m13.internal.delivery.persistence.SmsWindowStore;
import com.cambae.backend.m13.internal.delivery.persistence.SmsWindowStore.Batched;
import com.cambae.backend.m13.internal.delivery.persistence.SmsWindowStore.Window;
import com.cambae.backend.m13.internal.domain.DeliveryOutcome;
import com.cambae.backend.m13.internal.job.service.M13FastTask;
import com.cambae.backend.m13.internal.persistence.NotificationStore;
import com.cambae.backend.m13.internal.quiethours.service.QuietHoursPolicy;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đóng khung gộp hết hạn (việc 20 giây {@code CLOSE_SMS_WINDOWS}; FR-016b, research B-05): có tin dồn thì gửi một tin
 * tổng hợp (lần gửi mới trên thông báo dồn sau cùng), các tin dồn ghi Đã gộp kèm tin tổng hợp, mở khung mới bắt đầu bằng
 * tin tổng hợp; không có thì đóng. Khung của người thân hết hạn trong giờ yên tĩnh thì chờ hết giờ (FR-016b, FR-022). Tin
 * tổng hợp gửi nhân viên nêu tầng/khu vực (tên theo 003) và ca đang diễn ra lúc xảy ra sự kiện (008) của các thông báo dồn.
 */
@Service
public class SmsWindowCloser implements M13FastTask {

    private static final DateTimeFormatter HOUR =
            DateTimeFormatter.ofPattern("HH:mm").withZone(BusinessTime.ZONE);

    private final SmsWindowStore windows;
    private final NotificationStore notifications;
    private final DeliveryStore deliveries;
    private final DeliveryDispatcher dispatcher;
    private final QuietHoursPolicy quietHours;
    private final ParameterApi parameters;
    private final TransactionTemplate tx;
    private final BusinessTime time;
    private final ObjectProvider<ShiftApi> shifts;
    private final ObjectProvider<LocationStructureApi> structure;

    SmsWindowCloser(
            SmsWindowStore windows,
            NotificationStore notifications,
            DeliveryStore deliveries,
            DeliveryDispatcher dispatcher,
            QuietHoursPolicy quietHours,
            ParameterApi parameters,
            TransactionTemplate tx,
            BusinessTime time,
            ObjectProvider<ShiftApi> shifts,
            ObjectProvider<LocationStructureApi> structure) {
        this.windows = windows;
        this.notifications = notifications;
        this.deliveries = deliveries;
        this.dispatcher = dispatcher;
        this.quietHours = quietHours;
        this.parameters = parameters;
        this.tx = tx;
        this.time = time;
        this.shifts = shifts;
        this.structure = structure;
    }

    @Override
    public String name() {
        return "CLOSE_SMS_WINDOWS";
    }

    @Override
    public int run() {
        Instant now = time.now();
        boolean quiet = quietHours.current().endIfWithin(now).isPresent();
        int closed = 0;
        for (long id : windows.due(now)) {
            Boolean done = tx.execute(s -> windows.lockOpen(id)
                    .filter(w -> !(quiet && w.recipientKey().startsWith(RecipientType.FAMILY_MEMBER.name() + ":")))
                    .map(w -> {
                        close(w.id(), now);
                        return true;
                    })
                    .orElse(false));
            if (Boolean.TRUE.equals(done)) {
                closed++;
            }
        }
        return closed;
    }

    /** Đóng một khung trong transaction của người gọi; trả lần gửi tin tổng hợp nếu có tin dồn. */
    public Optional<Long> close(long windowId, Instant now) {
        Optional<Window> found = windows.lockOpen(windowId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Window w = found.get();
        List<Batched> batched = windows.batched(w.id());
        if (batched.isEmpty()) {
            windows.close(w.id(), now, null);
            return Optional.empty();
        }
        Batched carrier = batched.getLast();
        long digest = notifications.planAttempt(
                carrier.notificationId(),
                ChannelSelection.SMS,
                carrier.address(),
                now,
                w.recipientKey().startsWith(RecipientType.STAFF.name() + ":")
                        ? SmsBatching.digest(batched.size(), places(batched))
                        : SmsBatching.digest(batched.size()));
        for (Batched b : batched) {
            deliveries.insertResult(b.attemptId(), SendResult.of(DeliveryOutcome.BATCHED), digest, now);
            notifications.recompute(b.notificationId());
        }
        windows.close(w.id(), now, digest);
        windows.open(
                w.recipientKey(),
                now,
                now.plus(parameters.read(ParameterCode.CFG_M13_03).asDuration()),
                digest);
        dispatcher.dispatch(List.of(digest));
        return Optional.of(digest);
    }

    /** Tầng/khu vực và ca của các thông báo dồn, theo thứ tự, không lặp (008 FR-059). */
    private List<String> places(List<Batched> batched) {
        LocationStructureApi locations = structure.getIfAvailable();
        if (locations == null) {
            return List.of();
        }
        ShiftApi api = shifts.getIfAvailable();
        return batched.stream()
                .filter(b -> b.floorAreaId() != null)
                .flatMap(b -> locations.floor(b.floorAreaId()).stream()
                        .map(f -> Optional.ofNullable(api)
                                .flatMap(x -> x.currentShift(b.floorAreaId(), b.occurredAt()))
                                .map(s ->
                                        f.name() + ", ca " + HOUR.format(s.startsAt()) + "–" + HOUR.format(s.endsAt()))
                                .orElse(f.name())))
                .distinct()
                .toList();
    }
}
