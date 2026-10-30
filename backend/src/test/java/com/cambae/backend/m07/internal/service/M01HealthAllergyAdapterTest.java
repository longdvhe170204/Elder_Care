package com.cambae.backend.m07.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cambae.backend.m01.api.AllergenApi;
import com.cambae.backend.m01.api.HealthItemApi;
import com.cambae.backend.m01.api.dto.AllergenView;
import com.cambae.backend.m01.api.dto.HealthItemCheckSource;
import com.cambae.backend.m01.api.dto.HealthItemType;
import com.cambae.backend.m01.api.dto.HealthItemView;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** FR-007, FR-008, FR-012: dị ứng Hiệu lực của 001 thành mục so khớp của 006. */
class M01HealthAllergyAdapterTest {

    private final HealthItemApi healthItems = mock(HealthItemApi.class);
    private final AllergenApi allergens = mock(AllergenApi.class);
    private final M01HealthAllergyAdapter adapter = new M01HealthAllergyAdapter(healthItems, allergens);

    @Test
    void FR_007_catalogAllergyIsAutoChecked_otherAllergyIsReviewOnly() {
        when(healthItems.active(42L, HealthItemType.ALLERGY))
                .thenReturn(List.of(
                        new HealthItemView(
                                1L, 42L, HealthItemType.ALLERGY, Optional.of(101L), Optional.empty(), false, true),
                        new HealthItemView(
                                2L,
                                42L,
                                HealthItemType.ALLERGY,
                                Optional.empty(),
                                Optional.of("Hải sản"),
                                true,
                                false)));
        when(allergens.allergen(101L)).thenReturn(Optional.of(new AllergenView(101L, "Penicillin", null)));

        List<AllergyItemView> result = adapter.getActiveAllergies(42L);

        assertThat(result)
                .containsExactly(
                        AllergyItemView.fromCatalog(1L, 101L, "Penicillin", null),
                        AllergyItemView.fromFreeText(2L, "Hải sản", null));
    }

    @Test
    void FR_012_markChecked_clearsTheMedicationFlagOfM01() {
        adapter.markAllergyChecked(7L, "THUOC");

        verify(healthItems).markChecked(7L, HealthItemCheckSource.MEDICATION);
    }
}
