package com.eldercare.backend.nutrition.internal.menu.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Giờ bữa dự kiến theo CFG-M08-04 ({@code mealTimes}: đối tượng mã bữa → {@code "HH:mm"}; {@code null} là "Theo cơ sở",
 * viện chưa đặt). Bữa chưa có giờ thì rỗng.
 */
@Component
class MealTimes {

    private final ParameterApi parameters;
    private final NutritionCatalogStore catalog;

    MealTimes(ParameterApi parameters, NutritionCatalogStore catalog) {
        this.parameters = parameters;
        this.catalog = catalog;
    }

    Optional<LocalTime> expected(long mealId) {
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
}
