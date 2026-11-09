package com.eldercare.backend.nutrition.internal.menu.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.nutrition.internal.menu.domain.AllergyWarningRule.AllergyWarning;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck.Gap;
import com.eldercare.backend.nutrition.internal.menu.domain.DishRepetitionCheck.Repetition;
import java.util.List;

/**
 * Kết quả một lần kiểm tra thực đơn ở {@code record_event} loại {@code M08.MENU_CHECK}, cha {@code M08.MENU} (thay
 * {@code thuc_don_kiem_tra}, nhóm 3; data-model mục 5, 0.1): lần Gửi, Công bố, Đổi món; cảnh báo dị ứng giữ id người cho
 * bản của Bác sĩ, bếp chỉ được trả số người.
 */
public record MenuCheckRecord(
        Occasion occasion,
        List<Gap> coverageGaps,
        List<Repetition> repetitions,
        List<AllergyWarning> allergies,
        List<String> confirmedWarnings) {

    public static final SharedKind<MenuCheckRecord> KIND = SharedKind.of("M08.MENU_CHECK", MenuCheckRecord.class);

    public enum Occasion {
        SUBMIT,
        PUBLISH,
        DISH_SWAP
    }

    public MenuCheckRecord {
        coverageGaps = coverageGaps == null ? List.of() : List.copyOf(coverageGaps);
        repetitions = repetitions == null ? List.of() : List.copyOf(repetitions);
        allergies = allergies == null ? List.of() : List.copyOf(allergies);
        confirmedWarnings = confirmedWarnings == null ? List.of() : List.copyOf(confirmedWarnings);
    }
}
