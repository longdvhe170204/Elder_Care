package com.eldercare.backend.care.internal.wellbeing.domain;

import java.util.Map;
import java.util.OptionalDouble;

/** FR-070, Q-173: không có mẫu số thì chưa có tỷ lệ. */
public final class ParticipationRate {
    private ParticipationRate() {}

    public static OptionalDouble calculate(int present, Map<String, Integer> absentByReason) {
        int total = present
                + absentByReason.getOrDefault("REFUSED", 0)
                + absentByReason.getOrDefault("HEALTH", 0)
                + absentByReason.getOrDefault("OTHER", 0);
        return total == 0 ? OptionalDouble.empty() : OptionalDouble.of((double) present / total);
    }
}
