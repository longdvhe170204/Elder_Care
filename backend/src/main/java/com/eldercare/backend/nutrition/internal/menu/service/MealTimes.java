package com.eldercare.backend.nutrition.internal.menu.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Giờ bữa dự kiến, ngưỡng giao trễ theo CFG-M08-04 ({@code mealTimes}: đối tượng mã bữa → {@code "HH:mm"}; {@code null} là
 * "Theo cơ sở", viện chưa đặt; {@code lateDeliveryThreshold} là thời lượng). Bữa chưa có giờ thì rỗng: Đổi món hôm nay
 * được phép, chốt suất bị chặn (chủ dự án chốt 2026-11-06).
 */
@Component
public class MealTimes {

    private final ParameterApi parameters;
    private final NutritionCatalogStore catalog;

    MealTimes(ParameterApi parameters, NutritionCatalogStore catalog) {
        this.parameters = parameters;
        this.catalog = catalog;
    }

    public Optional<LocalTime> expected(long mealId) {
        JsonNode raw = parameters.read(ParameterCode.CFG_M08_04).raw();
        JsonNode times = raw == null ? null : raw.get("mealTimes");
        if (times == null || !times.isObject()) {
            return Optional.empty();
        }
        JsonNode value = times.get(catalog.mealCode(mealId));
        if (value == null || !value.isString()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalTime.parse(value.asString()));
        } catch (DateTimeParseException invalid) {
            return Optional.empty();
        }
    }

    /** Giờ bữa dự kiến của bữa tại ngày (giờ địa phương Asia/Ho_Chi_Minh). */
    public Optional<Instant> expectedAt(long mealId, LocalDate date) {
        return expected(mealId)
                .map(t -> date.atTime(t).atZone(BusinessTime.ZONE).toInstant());
    }

    /** Ngưỡng giao trễ (mặc định 30 phút). */
    public Duration lateThreshold() {
        JsonNode raw = parameters.read(ParameterCode.CFG_M08_04).raw();
        JsonNode threshold = raw == null ? null : raw.get("lateDeliveryThreshold");
        if (threshold == null || !threshold.isObject()) {
            return Duration.ofMinutes(30);
        }
        long value = threshold.get("value").longValue();
        return switch (threshold.get("unit").asString()) {
            case "HOURS" -> Duration.ofHours(value);
            case "SECONDS" -> Duration.ofSeconds(value);
            default -> Duration.ofMinutes(value);
        };
    }
}
