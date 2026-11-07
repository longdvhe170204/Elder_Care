package com.eldercare.backend.nutrition.internal.familyfood.domain;

import com.eldercare.backend.nutrition.api.dto.FamilyFoodStatus;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictType;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Hàm thuần đối chiếu đồ ăn gia đình với người nhận (FR-053, FR-054, BR-M08-04; research M-11). Không sử dụng khi loại đồ ăn
 * thuộc danh mục bị cấm, hạn dùng đã qua, hoặc thành phần xung đột (dị ứng, hạn chế riêng, hạn chế của chế độ ăn); ngược lại
 * Cần xác nhận khi có điểm nghi vấn; ngược lại Được sử dụng. Mục dị ứng loại "khác" của người nhận không kiểm tự động nên luôn
 * là điểm nghi vấn. Lý do là mã ổn định để lưu vào {@code systemReasons}.
 */
public final class FamilyFoodCheck {

    public static final String FORBIDDEN_TYPE = "FORBIDDEN_TYPE";
    public static final String EXPIRED = "EXPIRED";
    public static final String ALLERGY = ConflictType.ALLERGY.name();
    public static final String PERSONAL_RESTRICTION = ConflictType.PERSONAL_RESTRICTION.name();
    public static final String DIET_RESTRICTION = ConflictType.DIET_RESTRICTION.name();

    public static final String INGREDIENTS_UNKNOWN = "INGREDIENTS_UNKNOWN";
    public static final String OTHER_INGREDIENT = "OTHER_INGREDIENT";
    public static final String RESIDENT_OTHER_ALLERGY = "RESIDENT_OTHER_ALLERGY";
    public static final String TEXTURE_NOT_REGULAR = "TEXTURE_NOT_REGULAR";
    public static final String SENDER_NOT_RELATED = "SENDER_NOT_RELATED";
    public static final String TREATMENT_DIET = "TREATMENT_DIET";

    /** Đồ ăn: {@code hasOtherGroupIngredient} khi có thành phần thuộc nhóm dị nguyên "khác"; {@code senderRelated} khi người gửi có quan hệ Hiệu lực. */
    public record Food(
            String type,
            Set<Long> ingredients,
            boolean ingredientsUnknown,
            boolean hasOtherGroupIngredient,
            Instant expiresAt,
            boolean senderRelated) {}

    /** Người nhận: dị ứng Hiệu lực, hạn chế riêng, hạn chế của chế độ ăn hiện hành, kết cấu, chế độ ăn liên quan điều trị. */
    public record Person(
            List<AllergyItem> allergies,
            Set<Long> personalRestrictions,
            Set<Long> dietRestrictions,
            Texture texture,
            boolean treatmentDiet) {}

    public record Result(FamilyFoodStatus status, List<String> reasons) {}

    private FamilyFoodCheck() {}

    public static Result check(Food food, Person person, Set<String> forbiddenTypes, Instant now) {
        List<String> notAllowed = new ArrayList<>();
        String type = normalize(food.type());
        if (forbiddenTypes.stream().map(FamilyFoodCheck::normalize).anyMatch(type::equals)) {
            notAllowed.add(FORBIDDEN_TYPE);
        }
        if (!food.expiresAt().isAfter(now)) {
            notAllowed.add(EXPIRED);
        }
        ConflictCheck.conflicts(
                        food.ingredients(),
                        person.allergies(),
                        person.personalRestrictions(),
                        person.dietRestrictions())
                .forEach(c -> notAllowed.add(c.name()));
        if (!notAllowed.isEmpty()) {
            return new Result(FamilyFoodStatus.NOT_ALLOWED, List.copyOf(notAllowed));
        }
        List<String> suspicions = new ArrayList<>();
        if (food.ingredientsUnknown()) {
            suspicions.add(INGREDIENTS_UNKNOWN);
        }
        if (food.hasOtherGroupIngredient()) {
            suspicions.add(OTHER_INGREDIENT);
        }
        if (person.allergies().stream().anyMatch(a -> a.notAutoChecked() || a.allergenId() == null)) {
            suspicions.add(RESIDENT_OTHER_ALLERGY);
        }
        if (person.texture() != Texture.REGULAR) {
            suspicions.add(TEXTURE_NOT_REGULAR);
        }
        if (!food.senderRelated()) {
            suspicions.add(SENDER_NOT_RELATED);
        }
        if (person.treatmentDiet()) {
            suspicions.add(TREATMENT_DIET);
        }
        return suspicions.isEmpty()
                ? new Result(FamilyFoodStatus.ALLOWED, List.of())
                : new Result(FamilyFoodStatus.NEEDS_CONFIRMATION, List.copyOf(suspicions));
    }

    /** Không phân biệt hoa thường, khoảng trắng thừa, dấu (danh mục nhập tay, đồ ăn nhập tay). */
    static String normalize(String text) {
        String stripped = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return stripped.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
