package com.eldercare.backend.nutrition.internal.sample.domain;

import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson.DishChoice;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Danh sách món cần lưu mẫu của một bữa (hàm thuần; FR-061, Q-146; thay {@code MonCanLuuMau}): mọi món được nấu, gồm món
 * thực đơn, món thay thế được dùng, món an toàn được tự dùng (FR-037a) và món chỉ định riêng, mỗi món một lần dù xuất hiện ở
 * nhiều chế độ ăn. Không có thực đơn thì chỉ còn món an toàn.
 */
public final class DishesToRetain {

    private DishesToRetain() {}

    /**
     * @param served món phục vụ của từng người sau chốt, phát sinh và suất đặc biệt (món thực đơn, món thay thế)
     * @param assigned món Bác sĩ chỉ định riêng cho suất
     * @return mã món theo thứ tự xuất hiện, không trùng
     */
    public static List<Long> of(Collection<DishChoice> served, Collection<Long> assigned) {
        Set<Long> dishes = new LinkedHashSet<>();
        for (DishChoice choice : served) {
            dishes.add(choice.dishId());
            if (choice.substituteDishId() != null) {
                dishes.add(choice.substituteDishId());
            }
        }
        dishes.addAll(assigned);
        return new ArrayList<>(dishes);
    }

    /** Món cần lưu mẫu mà bản ghi chưa có (món mới phát sinh sau khi đã ghi lưu mẫu). */
    public static List<Long> notRecorded(List<Long> required, Collection<Long> recorded) {
        return required.stream().filter(dish -> !recorded.contains(dish)).toList();
    }
}
