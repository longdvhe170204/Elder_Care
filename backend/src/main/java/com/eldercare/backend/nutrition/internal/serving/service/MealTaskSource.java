package com.eldercare.backend.nutrition.internal.serving.service;

import com.eldercare.backend.care.api.spi.TaskSourcePort;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.MealScheduleApi;
import com.eldercare.backend.nutrition.api.dto.DailyMealView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Nguồn công việc loại {@code MEAL} cho 005 (T026; FR-017, FR-042, FR-050): kiểm bữa, người trong lịch bữa, liệt kê bữa
 * trong ca và xác nhận phục vụ trước khi ghi kết quả ăn uống. Bữa bị bỏ vì vắng ({@code sourceCancelled}) chưa cài.
 */
@Component
public class MealTaskSource implements TaskSourcePort {

    /** Lười: lịch bữa đi qua đếm suất, đọc chấm công 005, mà 005 lại cần bean này (vòng phụ thuộc lúc khởi tạo). */
    private final ObjectProvider<MealScheduleApi> scheduleProvider;

    private final ServingService serving;

    MealTaskSource(ObjectProvider<MealScheduleApi> scheduleProvider, ServingService serving) {
        this.scheduleProvider = scheduleProvider;
        this.serving = serving;
    }

    @Override
    public Set<SourceType> managedTypes() {
        return Set.of(SourceType.MEAL);
    }

    @Override
    public SourceValidation validate(SourceType type, long sourceId, long residentId, Instant plannedAt) {
        LocalDate date = LocalDate.ofInstant(plannedAt, BusinessTime.ZONE);
        boolean exists =
                scheduleProvider.getObject().mealsOnDate(date).stream().anyMatch(m -> m.dailyMealId() == sourceId);
        return new SourceValidation(
                exists, exists && scheduleProvider.getObject().isInMealSchedule(sourceId, residentId), exists, false);
    }

    @Override
    public List<SourceView> sources(long residentId, Instant from, Instant to) {
        List<SourceView> out = new ArrayList<>();
        LocalDate last = LocalDate.ofInstant(to, BusinessTime.ZONE);
        for (LocalDate d = LocalDate.ofInstant(from, BusinessTime.ZONE); !d.isAfter(last); d = d.plusDays(1)) {
            for (DailyMealView meal : scheduleProvider.getObject().mealsOnDate(d)) {
                Instant at = meal.expectedTime();
                if (!at.isBefore(from)
                        && at.isBefore(to)
                        && scheduleProvider.getObject().isInMealSchedule(meal.dailyMealId(), residentId)) {
                    out.add(new SourceView(SourceType.MEAL, meal.dailyMealId(), at, "Bữa ăn"));
                }
            }
        }
        return out;
    }

    @Override
    public boolean confirmServing(long mealId, long residentId) {
        return serving.servingConfirmed(mealId, residentId);
    }
}
