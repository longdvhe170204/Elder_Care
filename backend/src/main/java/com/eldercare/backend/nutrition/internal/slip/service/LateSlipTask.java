package com.eldercare.backend.nutrition.internal.slip.service;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.api.events.MealSlipLateNotice;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.MealSlip;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing;
import com.eldercare.backend.nutrition.internal.closing.persistence.ClosingRecordStore;
import com.eldercare.backend.nutrition.internal.closing.persistence.PortionClosingStore;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobMarkers;
import com.eldercare.backend.nutrition.internal.schedule.service.M08MinuteTask;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quá giờ bữa + ngưỡng giao trễ CFG-M08-04 (đã chốt vào lần chốt) mà phiếu chưa ở Đã giao, Đã nhận hoặc Có sai lệch: báo
 * Trưởng tầng và bếp một lần mỗi phiếu (FR-048a, BR-M08-12; dấu {@code M08.JOB.MEAL_SLIP_LATE}). Không áp cho lần chốt sau
 * giờ bữa (đã báo mức Trung bình khi chốt). Xét các bữa có giờ trong 6 giờ trước nên lần chạy bù vẫn báo.
 */
@Component
class LateSlipTask implements M08MinuteTask {

    private static final String KEY = "MEAL_SLIP_LATE";
    private static final Set<String> DONE = Set.of(
            MealSlipStatus.DELIVERED.name(), MealSlipStatus.HAS_DISCREPANCY.name(), MealSlipStatus.RECEIVED.name());

    private final PortionClosingStore closings;
    private final ClosingRecordStore records;
    private final NutritionCatalogStore catalog;
    private final M08JobMarkers markers;
    private final ApplicationEventPublisher events;

    LateSlipTask(
            PortionClosingStore closings,
            ClosingRecordStore records,
            NutritionCatalogStore catalog,
            M08JobMarkers markers,
            ApplicationEventPublisher events) {
        this.closings = closings;
        this.records = records;
        this.catalog = catalog;
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
        for (PortionClosing c : closings.mealTimeBetween(now.minus(Duration.ofHours(6)), now.plusNanos(1_000))) {
            if (c.closedAfterMealTime()
                    || now.isBefore(c.mealTime().plus(Duration.ofMinutes(c.lateThresholdMinutes())))) {
                continue;
            }
            for (SimpleRecord<MealSlip> slip : records.slips(c.dailyMealId())) {
                if (DONE.contains(slip.status())) {
                    continue;
                }
                if (markers.firstTime(KEY, MealSlipService.ENTITY, slip.id(), "late")) {
                    events.publishEvent(new MealSlipLateNotice(
                            slip.id(),
                            c.dailyMealId(),
                            slip.content().floorAreaId(),
                            catalog.meal(c.mealId()).name(),
                            now));
                }
            }
        }
    }
}
