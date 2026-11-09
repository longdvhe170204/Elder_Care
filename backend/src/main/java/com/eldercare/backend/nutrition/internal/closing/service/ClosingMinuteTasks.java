package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.events.AdjustmentsPending;
import com.eldercare.backend.nutrition.api.events.PortionClosingReminder;
import com.eldercare.backend.nutrition.internal.closing.domain.DailyMeal;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing;
import com.eldercare.backend.nutrition.internal.closing.persistence.ClosingRecordStore;
import com.eldercare.backend.nutrition.internal.closing.persistence.PortionClosingStore;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobMarkers;
import com.eldercare.backend.nutrition.internal.schedule.service.M08MinuteTask;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Việc theo phút của chốt suất (research M-08, M-15; Q-306, Q-333): nhắc chốt tại mốc (FR-033), món an toàn tại CFG-M08-05
 * trước giờ bữa (FR-037a ii), suất giữ tại giờ bữa (FR-040a), nhắc phát sinh chưa xác nhận (FR-041). Mỗi việc mỗi bữa ngày
 * đúng một lần theo dấu {@code M08.JOB.<khóa>}; xét mọi bữa đã tới hạn nên lần chạy bù vẫn làm. Không chốt suất (Q-306).
 */
final class ClosingMinuteTasks {

    private ClosingMinuteTasks() {}

    /** Tới mốc chốt (giờ bữa − CFG-M08-01) mà bữa chưa chốt: nhắc bếp một lần. */
    @Component
    static class ClosingReminderTask implements M08MinuteTask {

        private static final String KEY = "PORTION_CLOSING_DUE";

        private final DailyMeals dailyMeals;
        private final M08JobMarkers markers;
        private final ApplicationEventPublisher events;

        ClosingReminderTask(DailyMeals dailyMeals, M08JobMarkers markers, ApplicationEventPublisher events) {
            this.dailyMeals = dailyMeals;
            this.markers = markers;
            this.events = events;
        }

        @Override
        public String key() {
            return KEY;
        }

        @Override
        @Transactional
        public void run(Instant now) {
            LocalDate today = now.atZone(BusinessTime.ZONE).toLocalDate();
            Duration lead = dailyMeals.closingLead();
            for (LocalDate date : List.of(today, today.plusDays(1))) {
                for (DailyMeal meal : dailyMeals.on(date)) {
                    Optional<Instant> opens = meal.closingOpensAt(lead);
                    if (meal.closed()
                            || opens.isEmpty()
                            || now.isBefore(opens.get())
                            || now.isAfter(meal.lateDeadline())) {
                        continue;
                    }
                    if (markers.firstTime(KEY, ClosingRecordStore.DAILY_MEAL, meal.id(), "due")) {
                        events.publishEvent(new PortionClosingReminder(meal.id(), meal.mealName(), date, now));
                    }
                }
            }
        }
    }

    /** CFG-M08-05 trước giờ bữa: suất thiếu món thay thế, chưa có món được Bác sĩ chưa xử lý thì dùng món an toàn. */
    @Component
    static class SafeDishTask implements M08MinuteTask {

        private static final String KEY = "SAFE_DISH";

        private final PortionClosingStore closings;
        private final PortionClosingService closing;
        private final DailyMeals dailyMeals;
        private final M08JobMarkers markers;

        SafeDishTask(
                PortionClosingStore closings,
                PortionClosingService closing,
                DailyMeals dailyMeals,
                M08JobMarkers markers) {
            this.closings = closings;
            this.closing = closing;
            this.dailyMeals = dailyMeals;
            this.markers = markers;
        }

        @Override
        public String key() {
            return KEY;
        }

        @Override
        @Transactional
        public void run(Instant now) {
            Duration lead = dailyMeals.reminderLead();
            for (PortionClosing c : closings.mealTimeBetween(
                    now.plusNanos(1_000), now.plus(lead).plusNanos(1_000))) {
                if (markers.firstTime(KEY, ClosingRecordStore.DAILY_MEAL, c.dailyMealId(), "safe-dish")) {
                    closing.recompute(c, Optional.empty(), "M08.SAFE_DISH:" + c.dailyMealId(), "SAFE_DISH", true, now);
                }
            }
        }
    }

    /** Giờ bữa: suất "dự kiến trở về" của người chưa về thành suất giữ (FR-040a). */
    @Component
    static class HeldPortionTask implements M08MinuteTask {

        private static final String KEY = "HELD_PORTION";

        private final PortionClosingStore closings;
        private final AdjustmentService adjustments;
        private final M08JobMarkers markers;

        HeldPortionTask(PortionClosingStore closings, AdjustmentService adjustments, M08JobMarkers markers) {
            this.closings = closings;
            this.adjustments = adjustments;
            this.markers = markers;
        }

        @Override
        public String key() {
            return KEY;
        }

        @Override
        @Transactional
        public void run(Instant now) {
            for (PortionClosing c : closings.mealTimeBetween(now.minus(Duration.ofHours(6)), now.plusNanos(1_000))) {
                if (now.isAfter(c.mealTime().plus(Duration.ofMinutes(c.lateThresholdMinutes())))) {
                    continue;
                }
                if (markers.firstTime(KEY, ClosingRecordStore.DAILY_MEAL, c.dailyMealId(), "held")) {
                    adjustments.holdNotReturned(c, now);
                }
            }
        }
    }

    /** Phát sinh cần giao chưa được bếp xác nhận tại CFG-M08-05 trước giờ bữa: nhắc bếp và Bác sĩ một lần (FR-041). */
    @Component
    static class AdjustmentReminderTask implements M08MinuteTask {

        private static final String KEY = "ADJUSTMENT_REMINDER";

        private final PortionClosingStore closings;
        private final AdjustmentService adjustments;
        private final DailyMeals dailyMeals;
        private final M08JobMarkers markers;
        private final ApplicationEventPublisher events;

        AdjustmentReminderTask(
                PortionClosingStore closings,
                AdjustmentService adjustments,
                DailyMeals dailyMeals,
                M08JobMarkers markers,
                ApplicationEventPublisher events) {
            this.closings = closings;
            this.adjustments = adjustments;
            this.dailyMeals = dailyMeals;
            this.markers = markers;
            this.events = events;
        }

        @Override
        public String key() {
            return KEY;
        }

        @Override
        @Transactional
        public void run(Instant now) {
            Duration lead = dailyMeals.reminderLead();
            for (PortionClosing c : closings.mealTimeBetween(
                    now.plusNanos(1_000), now.plus(lead).plusNanos(1_000))) {
                int pending = adjustments.unconfirmed(c.dailyMealId());
                if (pending > 0 && markers.firstTime(KEY, ClosingRecordStore.DAILY_MEAL, c.dailyMealId(), "reminder")) {
                    events.publishEvent(new AdjustmentsPending(
                            c.dailyMealId(),
                            c.mealDate(),
                            pending,
                            true,
                            "M08.ADJUSTMENT_CONFIRMATION_DUE:" + c.dailyMealId(),
                            now));
                }
            }
        }
    }
}
