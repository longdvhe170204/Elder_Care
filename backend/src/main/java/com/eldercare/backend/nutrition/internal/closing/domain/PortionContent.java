package com.eldercare.backend.nutrition.internal.closing.domain;

import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson.DishChoice;
import com.eldercare.backend.nutrition.internal.conflict.domain.SubstituteDishSelector;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongPredicate;

/**
 * Hàm thuần nội dung suất của một người cao tuổi (FR-036, FR-036a, FR-037, FR-037a, FR-038; research M-06, M-08): món theo
 * thực đơn, món thay thế đầu tiên không xung đột, dấu thiếu món thay thế, chưa có món; món an toàn khi được phép áp.
 */
public final class PortionContent {

    /**
     * Đầu vào: các dòng thực đơn của chế độ ăn ở bữa đó, thực đơn đã công bố chưa, kết cấu, món Bác sĩ chỉ định riêng, phép
     * thử xung đột với người (FR-020), món an toàn của chế độ ăn, có áp món an toàn lúc này không, có dị ứng loại "khác".
     */
    public record Input(
            List<MenuDish> rows,
            boolean menuPublished,
            Texture texture,
            Long assignedDishId,
            LongPredicate conflicts,
            Long safeDishId,
            boolean applySafeDish,
            boolean crossCheck) {}

    /** Kết quả: món phục vụ, dấu, suất đặc biệt; {@code safeDishConflicts} khi cần món an toàn mà món đó xung đột. */
    public record Portion(
            List<DishChoice> dishes, Set<PortionFlag> flags, boolean special, boolean safeDishConflicts) {}

    private PortionContent() {}

    public static Portion of(Input input) {
        Set<PortionFlag> flags = EnumSet.noneOf(PortionFlag.class);
        if (input.crossCheck()) {
            flags.add(PortionFlag.NEEDS_CROSS_CHECK);
        }
        List<DishChoice> dishes = new ArrayList<>();
        boolean conflict = false;
        boolean missing = false;
        if (input.assignedDishId() != null) {
            input.rows().forEach(r -> dishes.add(new DishChoice(r.dishId(), input.assignedDishId())));
            if (dishes.isEmpty()) {
                dishes.add(new DishChoice(input.assignedDishId(), null));
            }
        } else {
            for (MenuDish row : input.rows()) {
                if (input.conflicts().test(row.dishId())) {
                    conflict = true;
                    Optional<Long> substitute =
                            SubstituteDishSelector.choose(row.substituteDishIds(), input.conflicts());
                    missing |= substitute.isEmpty();
                    dishes.add(new DishChoice(row.dishId(), substitute.orElse(null)));
                } else {
                    dishes.add(new DishChoice(row.dishId(), null));
                }
            }
        }
        boolean noDish = input.rows().isEmpty() && input.assignedDishId() == null;
        boolean needsSafe = missing || noDish;
        boolean safeConflicts = false;
        if (needsSafe && input.applySafeDish() && input.safeDishId() != null) {
            if (input.conflicts().test(input.safeDishId())) {
                safeConflicts = true;
            } else {
                long safe = input.safeDishId();
                List<DishChoice> withSafe = noDish
                        ? List.of(new DishChoice(safe, null))
                        : dishes.stream()
                                .map(d -> d.substituteDishId() == null
                                                && input.conflicts().test(d.dishId())
                                        ? new DishChoice(d.dishId(), safe)
                                        : d)
                                .toList();
                dishes.clear();
                dishes.addAll(withSafe);
                flags.add(PortionFlag.SAFE_DISH);
                needsSafe = false;
            }
        } else if (needsSafe && input.applySafeDish()) {
            safeConflicts = true;
        }
        if (needsSafe && missing) {
            flags.add(PortionFlag.MISSING_SUBSTITUTE_DISH);
        }
        if (needsSafe && noDish && input.menuPublished()) {
            flags.add(PortionFlag.NO_DISH_IN_MENU);
        }
        boolean special = conflict
                || input.texture() != Texture.REGULAR
                || input.assignedDishId() != null
                || flags.contains(PortionFlag.SAFE_DISH);
        return new Portion(dishes, flags, special, safeConflicts);
    }

    /** Suất người thân ở lại: món chính của chế độ mặc định, không là suất đặc biệt (FR-036). */
    public static List<DishChoice> family(List<MenuDish> rows) {
        return rows.stream().map(r -> new DishChoice(r.dishId(), null)).toList();
    }
}
