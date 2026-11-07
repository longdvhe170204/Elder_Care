package com.eldercare.backend.incident.internal.alert.domain;

import java.util.OptionalInt;
import java.util.function.IntPredicate;

/**
 * Chuỗi leo thang ba cấp (FR-033, research S-08): cấp 0 người phụ trách ban đầu, cấp 1 Trưởng tầng được giao và
 * Bác sĩ trực, cấp 2 Quản lý viện. Mỗi lần chỉ lên một cấp có người nhận.
 */
public final class AlertEscalationChain {
    public static final int MAX_LEVEL = 2;

    private AlertEscalationChain() {}

    /** Cấp kế tiếp có người nhận, bỏ qua cấp không có ai; rỗng khi đã tối đa hoặc không còn cấp nào có người. */
    public static OptionalInt next(int current, IntPredicate hasRecipients) {
        for (int level = current + 1; level <= MAX_LEVEL; level++) {
            if (hasRecipients.test(level)) return OptionalInt.of(level);
        }
        return OptionalInt.empty();
    }

    public static boolean atMaximum(int level) {
        return level >= MAX_LEVEL;
    }
}
