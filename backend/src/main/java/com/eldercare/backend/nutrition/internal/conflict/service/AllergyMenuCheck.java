package com.eldercare.backend.nutrition.internal.conflict.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.AlertRequested;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.persistence.DietAssignmentStore;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictType;
import com.eldercare.backend.nutrition.internal.menu.service.ResidentMenuConflicts;
import com.eldercare.backend.nutrition.internal.menu.service.ResidentMenuConflicts.Conflict;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dị ứng mới ghi nhận đối chiếu với thực đơn chưa hết tuần theo chế độ ăn hiện hành (FR-021; FR-065): món xung đột chưa có
 * món thay thế phù hợp thì yêu cầu 007 tạo cảnh báo mức Trung bình ({@code AlertRequested} loại
 * {@code DIET_ALLERGY_CONFLICT}, nguồn {@code MENU_DISH}, glossary mục 17.5); Bác sĩ được báo qua cảnh báo của 007. Suất đã
 * chốt được tính lại bởi listener chốt suất.
 */
@Component
public class AllergyMenuCheck {

    private final DietAssignmentStore assignments;
    private final NutritionCatalogStore catalog;
    private final ResidentMenuConflicts conflicts;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    AllergyMenuCheck(
            DietAssignmentStore assignments,
            NutritionCatalogStore catalog,
            ResidentMenuConflicts conflicts,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.assignments = assignments;
        this.catalog = catalog;
        this.conflicts = conflicts;
        this.time = time;
        this.events = events;
    }

    @Transactional
    public void onAllergyRecorded(long itemId, long residentId) {
        Optional<DietAssignment> active = assignments.active(residentId);
        Optional<Long> dietId = active.map(DietAssignment::dietId)
                .or(() -> catalog.diets().stream()
                        .filter(d -> d.active() && d.attributes().isDefault())
                        .map(d -> d.id())
                        .findFirst());
        if (dietId.isEmpty()) {
            return;
        }
        Set<Long> personal = active.map(a -> (Set<Long>) new HashSet<>(a.personalRestrictions()))
                .orElse(Set.of());
        Set<String> slots = new LinkedHashSet<>();
        for (Conflict c : conflicts.check(residentId, dietId.get(), personal).conflicts()) {
            String slot = c.date() + ":" + c.mealId() + ":" + c.dishId();
            if (c.type() != ConflictType.ALLERGY || c.hasSuitableSubstitute() || !slots.add(slot)) {
                continue;
            }
            events.publishEvent(new AlertRequested(
                    residentId,
                    "DIET_ALLERGY_CONFLICT",
                    "MEDIUM",
                    "MENU_DISH",
                    c.dishId(),
                    Optional.of(c.date() + ":" + c.mealId()),
                    time.now(),
                    "M08.DIET_ALLERGY_CONFLICT:" + itemId + ":" + slot));
        }
    }
}
