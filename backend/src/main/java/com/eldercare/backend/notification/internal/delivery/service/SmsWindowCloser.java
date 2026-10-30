package com.eldercare.backend.notification.internal.delivery.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.delivery.domain.BatchedAttempt;
import com.eldercare.backend.notification.internal.delivery.domain.ChannelSelection;
import com.eldercare.backend.notification.internal.delivery.domain.SendResult;
import com.eldercare.backend.notification.internal.delivery.domain.SmsBatching;
import com.eldercare.backend.notification.internal.delivery.domain.SmsWindow;
import com.eldercare.backend.notification.internal.delivery.persistence.DeliveryStore;
import com.eldercare.backend.notification.internal.delivery.persistence.SmsWindowStore;
import com.eldercare.backend.notification.internal.domain.DeliveryOutcome;
import com.eldercare.backend.notification.internal.job.service.M13FastTask;
import com.eldercare.backend.notification.internal.persistence.NotificationStore;
import com.eldercare.backend.notification.internal.quiethours.service.QuietHoursPolicy;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.staffing.api.ShiftApi;
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
        Optional<SmsWindow> found = windows.lockOpen(windowId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        SmsWindow w = found.get();
        List<BatchedAttempt> batched = windows.batched(w.id());
        if (batched.isEmpty()) {
            windows.close(w.id(), now, null);
            return Optional.empty();
        }
        BatchedAttempt carrier = batched.getLast();
        long digest = notifications.planAttempt(
                carrier.notificationId(),
                ChannelSelection.SMS,
                carrier.address(),
                now,
                w.recipientKey().startsWith(RecipientType.STAFF.name() + ":")
                        ? SmsBatching.digest(batched.size(), places(batched))
                        : SmsBatching.digest(batched.size()));
        for (BatchedAttempt b : batched) {
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
    private List<String> places(List<BatchedAttempt> batched) {
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
