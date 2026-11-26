package com.eldercare.backend.nutrition.internal.closing.domain;

import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson.DishChoice;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import java.util.List;

/**
 * Nội dung suất của một người (FR-035, Q-445): các món của bữa trong thực đơn chung, như nhau cho mọi người; không có món
 * thay thế, món an toàn, suất đặc biệt.
 */
public final class PortionContent {

    /** Tầng/khu quy ước của phiếu nhân viên (FR-044, Q-445): suất Quản lý viện, nhân viên trong ca. */
    public static final long STAFF_GROUP = 0L;

    private PortionContent() {}

    public static List<DishChoice> of(List<MenuDish> rows) {
        return rows.stream().map(r -> new DishChoice(r.dishId(), null)).toList();
    }
}
