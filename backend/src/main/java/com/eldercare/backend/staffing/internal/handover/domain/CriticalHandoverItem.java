package com.eldercare.backend.staffing.internal.handover.domain;

import java.util.Set;

/**
 * FR-041/Q-83: chỉ xét trạng thái nguồn tại thời điểm hoàn tất. Mức cảnh báo, sự cố theo {@code IncidentLevel} của 007
 * ({@code LOW}, {@code MEDIUM}, {@code CRITICAL}): Khẩn cấp là {@code CRITICAL}.
 */
public final class CriticalHandoverItem {
    private CriticalHandoverItem() {}

    public static boolean requiresNote(String group, String status, String level) {
        return switch (group) {
            case "ALERT", "INCIDENT" ->
                level != null && Set.of("CRITICAL", "MEDIUM").contains(level);
            case "DOSE" -> status != null && Set.of("MISSED", "REFUSED").contains(status);
            case "TASK" -> "OVERDUE".equals(status) && "MANDATORY".equals(level);
            default -> false;
        };
    }
}
