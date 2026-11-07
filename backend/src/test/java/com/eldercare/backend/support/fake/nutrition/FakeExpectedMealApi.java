package com.eldercare.backend.support.fake.nutrition;

import com.eldercare.backend.nutrition.api.ExpectedMealApi;
import com.eldercare.backend.nutrition.api.dto.ExpectedPortionView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bản giả {@link ExpectedMealApi} cho kênh người cao tuổi của 012 (T067). */
public class FakeExpectedMealApi implements ExpectedMealApi {

    private final Map<Long, List<ExpectedPortionView>> portions = new HashMap<>();

    public FakeExpectedMealApi portion(long residentId, ExpectedPortionView portion) {
        portions.computeIfAbsent(residentId, k -> new ArrayList<>()).add(portion);
        return this;
    }

    @Override
    public List<ExpectedPortionView> ofResident(long residentId, LocalDate from, LocalDate to) {
        return portions.getOrDefault(residentId, List.of()).stream()
                .filter(p -> !p.date().isBefore(from) && !p.date().isAfter(to))
                .toList();
    }
}
