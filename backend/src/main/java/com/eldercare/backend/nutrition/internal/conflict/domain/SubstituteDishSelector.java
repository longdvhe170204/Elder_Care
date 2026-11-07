package com.eldercare.backend.nutrition.internal.conflict.domain;

import java.util.List;
import java.util.Optional;
import java.util.function.LongPredicate;

/**
 * Chọn món thay thế (FR-036, research M-06): món đầu tiên theo thứ tự ưu tiên không xung đột với người; không có thì
 * rỗng, người gọi gắn dấu {@code MISSING_SUBSTITUTE_DISH}.
 */
public final class SubstituteDishSelector {

    private SubstituteDishSelector() {}

    public static Optional<Long> choose(List<Long> substituteDishIds, LongPredicate hasConflict) {
        return substituteDishIds.stream().filter(id -> !hasConflict.test(id)).findFirst();
    }
}
