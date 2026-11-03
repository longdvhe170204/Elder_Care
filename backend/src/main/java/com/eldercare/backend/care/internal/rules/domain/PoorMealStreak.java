package com.eldercare.backend.care.internal.rules.domain;

import java.util.*;

/** FR040: đầu vào mới nhất trước; bữa thiếu/VOID được bỏ ở projection hiện hành. */
public final class PoorMealStreak {
    private PoorMealStreak() {}

    public static int count(List<String> newestFirst) {
        int count = 0;
        for (var value : newestFirst) {
            if (!Set.of("SOME", "NOT_EATEN", "SKIPPED").contains(value)) break;
            count++;
        }
        return count;
    }
}
