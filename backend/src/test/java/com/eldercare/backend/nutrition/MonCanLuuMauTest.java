package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson.DishChoice;
import com.eldercare.backend.nutrition.internal.sample.domain.DishesToRetain;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T038 (US9, FR-061, SC-013; quickstart Q47; tên {@code MonCanLuuMauTest} trong tasks): danh sách món cần lưu mẫu tính
 * từ món phục vụ của lần chốt (món thực đơn, món thay thế, món an toàn) và món chỉ định riêng, mỗi món một lần.
 */
class MonCanLuuMauTest {

    @Test
    void FR_061_menuSubstituteAndSafeDish_eachOnceInOrder() {
        List<Long> dishes = DishesToRetain.of(
                List.of(
                        new DishChoice(10, null),
                        new DishChoice(10, 20L),
                        new DishChoice(30, null),
                        new DishChoice(10, null)),
                List.of());

        assertThat(dishes).containsExactly(10L, 20L, 30L);
    }

    @Test
    void FR_061_assignedDishAddedOnce_evenWhenAlreadyServed() {
        List<Long> dishes = DishesToRetain.of(List.of(new DishChoice(10, 20L)), List.of(40L, 20L, 40L));

        assertThat(dishes).containsExactly(10L, 20L, 40L);
    }
}
