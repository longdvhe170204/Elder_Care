package com.eldercare.backend.nutrition.internal.closing.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing.FloorDietCount;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Nội dung {@code content} của lần chốt suất ở {@code record_event} loại {@code M08.PORTION_CLOSING} (R-15). */
public record PortionClosingContent(
        long mealId,
        LocalDate mealDate,
        Instant mealTime,
        int lateThresholdMinutes,
        boolean closedAfterMealTime,
        Long menuId,
        int totalPortions,
        List<CountedPerson> persons,
        List<FloorDietCount> portions,
        Set<String> flags,
        Set<String> missingData) {

    public static final SharedKind<PortionClosingContent> KIND =
            SharedKind.of("M08.PORTION_CLOSING", PortionClosingContent.class);
}
