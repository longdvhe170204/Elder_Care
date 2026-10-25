package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m01.api.AllergenApi;
import com.cambae.backend.m01.api.HealthItemApi;
import com.cambae.backend.m01.api.dto.AllergenView;
import com.cambae.backend.m01.api.dto.HealthItemCheckSource;
import com.cambae.backend.m01.api.dto.HealthItemType;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@link HealthAllergyPort} đọc dị ứng Hiệu lực của 001 qua {@link HealthItemApi} (FR-007, FR-008, FR-012). Dị ứng chọn
 * từ danh mục so khớp tự động theo dị nguyên; dị ứng khác nhập tay chỉ hiển thị (BR-M01-12). Mục sức khỏe của 001 không
 * có mức độ nên {@code severity} để trống.
 */
@Component
class M01HealthAllergyAdapter implements HealthAllergyPort {

    private final HealthItemApi healthItems;
    private final AllergenApi allergens;

    M01HealthAllergyAdapter(HealthItemApi healthItems, AllergenApi allergens) {
        this.healthItems = healthItems;
        this.allergens = allergens;
    }

    @Override
    public List<AllergyItemView> getActiveAllergies(long residentId) {
        return healthItems.active(residentId, HealthItemType.ALLERGY).stream()
                .map(item -> item.allergenId().isPresent() && !item.notAutoChecked()
                        ? AllergyItemView.fromCatalog(
                                item.id(),
                                item.allergenId().get(),
                                allergens
                                        .allergen(item.allergenId().get())
                                        .map(AllergenView::name)
                                        .orElse(null),
                                null)
                        : AllergyItemView.fromFreeText(
                                item.id(), item.freeText().orElse(null), null))
                .toList();
    }

    @Override
    public void markAllergyChecked(long allergyId, String module) {
        healthItems.markChecked(allergyId, HealthItemCheckSource.MEDICATION);
    }
}
