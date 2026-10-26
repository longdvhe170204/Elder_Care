package com.cambae.backend.m09.handover.internal.domain;

import java.util.Set;

/** FR-041/Q-83: chỉ xét trạng thái nguồn tại thời điểm hoàn tất. */
public final class CriticalHandoverItem {
    private CriticalHandoverItem() {}

    public static boolean requiresNote(String group, String status, String level) {
        return switch (group) {
            case "ALERT", "INCIDENT" ->
                level != null && Set.of("EMERGENCY", "MEDIUM").contains(level);
            case "DOSE" -> status != null && Set.of("MISSED", "REFUSED").contains(status);
            case "TASK" -> "OVERDUE".equals(status) && "MANDATORY".equals(level);
            default -> false;
        };
    }
}
