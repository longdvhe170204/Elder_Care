package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.assignment.domain.ProfessionalReasonRule;
import com.eldercare.backend.nutrition.internal.assignment.domain.ProfessionalReasonRule.Side;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 011 T012 (US1, FR-013 (a)–(d), BR-M08-03, Q-145, Q-283): bản gán mới so với bản Hiệu lực (hoặc chế độ ăn mặc định)
 * cần lý do chuyên môn khi đổi vào, ra khỏi chế độ liên quan điều trị, chuyển kết cấu cứng hơn hoặc bỏ hạn chế riêng
 * trong lúc dùng chế độ đó. Thứ tự kết cấu từ mềm tới cứng: xay nhuyễn → mềm → thường.
 */
class ProfessionalReasonRuleTest {

    private static final long REGULAR_DIET = 1;
    private static final long DIABETES = 2;
    private static final long KIDNEY = 3;
    private static final long VEGETARIAN = 4;

    private static Side side(long diet, boolean treatment, Texture texture, Long... restrictions) {
        return new Side(diet, treatment, texture, Set.of(restrictions));
    }

    @Test
    void FR_013a_toTreatmentDietFromOther_requiresReason() {
        assertThat(ProfessionalReasonRule.required(
                        side(REGULAR_DIET, false, Texture.REGULAR), side(DIABETES, true, Texture.SOFT)))
                .isTrue();
    }

    @Test
    void FR_013b_fromTreatmentDietToOther_requiresReason() {
        assertThat(ProfessionalReasonRule.required(
                        side(DIABETES, true, Texture.SOFT), side(REGULAR_DIET, false, Texture.SOFT)))
                .isTrue();
        assertThat(ProfessionalReasonRule.required(
                        side(DIABETES, true, Texture.SOFT), side(KIDNEY, true, Texture.SOFT)))
                .isTrue();
    }

    @Test
    void FR_013_nonTreatmentToNonTreatment_noReason() {
        assertThat(ProfessionalReasonRule.required(
                        side(REGULAR_DIET, false, Texture.REGULAR), side(VEGETARIAN, false, Texture.PUREED)))
                .isFalse();
    }

    @Test
    void FR_013c_sameTreatmentDietHarderTexture_requiresReason() {
        assertThat(ProfessionalReasonRule.required(
                        side(DIABETES, true, Texture.PUREED), side(DIABETES, true, Texture.SOFT)))
                .isTrue();
        assertThat(ProfessionalReasonRule.required(
                        side(DIABETES, true, Texture.SOFT), side(DIABETES, true, Texture.REGULAR)))
                .isTrue();
    }

    @Test
    void FR_013c_sameTreatmentDietSofterTextureOrMoreRestrictions_noReason() {
        assertThat(ProfessionalReasonRule.required(
                        side(DIABETES, true, Texture.SOFT, 10L), side(DIABETES, true, Texture.PUREED, 10L, 11L)))
                .isFalse();
        assertThat(ProfessionalReasonRule.required(
                        side(DIABETES, true, Texture.SOFT), side(DIABETES, true, Texture.SOFT)))
                .isFalse();
    }

    @Test
    void FR_013d_sameTreatmentDietDropsRestriction_requiresReason() {
        assertThat(ProfessionalReasonRule.required(
                        side(DIABETES, true, Texture.SOFT, 10L), side(DIABETES, true, Texture.SOFT)))
                .isTrue();
        // Thay một hạn chế bằng hạn chế khác cũng tính là bỏ
        assertThat(ProfessionalReasonRule.required(
                        side(DIABETES, true, Texture.SOFT, 10L), side(DIABETES, true, Texture.SOFT, 11L)))
                .isTrue();
    }

    @Test
    void FR_013cd_sameNonTreatmentDiet_harderTextureOrDropRestriction_noReason() {
        assertThat(ProfessionalReasonRule.required(
                        side(VEGETARIAN, false, Texture.PUREED, 10L), side(VEGETARIAN, false, Texture.REGULAR)))
                .isFalse();
    }

    @Test
    void FR_013_noCurrentAndNoDefault_onlyTreatmentDietRequiresReason() {
        assertThat(ProfessionalReasonRule.required(null, side(DIABETES, true, Texture.REGULAR)))
                .isTrue();
        assertThat(ProfessionalReasonRule.required(null, side(VEGETARIAN, false, Texture.REGULAR)))
                .isFalse();
    }
}
