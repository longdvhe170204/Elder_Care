package com.eldercare.backend.nutrition.internal.menu.domain;

import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.nutrition.internal.conflict.domain.SubstituteDishSelector;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hàm thuần cảnh báo dị ứng của thực đơn (BR-M08-07, FR-025 c, FR-020; research M-07): món chính xung đột với ít nhất một
 * người dùng chế độ ăn mà không có món thay thế nào phù hợp với người đó. Danh sách người chỉ dùng cho người có quyền xem
 * dị ứng; bếp chỉ nhận số người (19.3, SC-014).
 */
public final class AllergyWarningRule {

    /** Người dùng chế độ ăn: mục dị ứng Hiệu lực (001) và hạn chế riêng của bản gán. */
    public record DietUser(long residentId, List<AllergyItem> allergies, Set<Long> personalRestrictions) {}

    public record AllergyWarning(LocalDate date, long mealId, long dietId, long dishId, List<Long> residentIds) {

        /** Mã cảnh báo cần xác nhận (FR-025): theo ô (ngày, bữa, chế độ ăn) và món, không theo số người. */
        public String code() {
            return "ALLERGY:" + date + ":" + mealId + ":" + dietId + ":" + dishId;
        }
    }

    private AllergyWarningRule() {}

    public static List<AllergyWarning> find(
            List<MenuDish> dishes,
            Map<Long, List<DietUser>> usersByDiet,
            Map<Long, Set<Long>> dietRestrictions,
            Map<Long, Set<Long>> ingredients) {
        List<AllergyWarning> out = new ArrayList<>();
        for (MenuDish dish : dishes) {
            Set<Long> restrictions = dietRestrictions.getOrDefault(dish.dietId(), Set.of());
            List<Long> residents = new ArrayList<>();
            for (DietUser user : usersByDiet.getOrDefault(dish.dietId(), List.of())) {
                if (conflicts(user, dish.dishId(), restrictions, ingredients)
                        && SubstituteDishSelector.choose(
                                        dish.substituteDishIds(), id -> conflicts(user, id, restrictions, ingredients))
                                .isEmpty()) {
                    residents.add(user.residentId());
                }
            }
            if (!residents.isEmpty()) {
                out.add(new AllergyWarning(dish.date(), dish.mealId(), dish.dietId(), dish.dishId(), residents));
            }
        }
        return out;
    }

    private static boolean conflicts(
            DietUser user, long dishId, Set<Long> dietRestrictions, Map<Long, Set<Long>> ingredients) {
        return ConflictCheck.hasConflict(
                ingredients.getOrDefault(dishId, Set.of()),
                user.allergies(),
                user.personalRestrictions(),
                dietRestrictions);
    }
}
