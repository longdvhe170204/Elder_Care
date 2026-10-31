package com.eldercare.backend.administration.approval.api;

import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import java.time.Instant;

/**
 * Tính việc nhắc người duyệt và báo Quản lý viện cho một mục chờ quyết định (FR-031a, BR-M15-09; contracts mục 1.5
 * {@code NhacQuaHan}). Dùng lại cho các vòng đời riêng (010, 012, 013, 017, 019). Hai mốc tính theo giờ liên tục từ
 * {@code since}, đọc tham số lúc gọi; không bao giờ tự hủy.
 */
public interface OverdueReminder {

    Decision check(
            Instant since,
            Instant remindedAt,
            Instant escalatedAt,
            ParameterCode remind,
            ParameterCode escalate,
            boolean hasApprover);

    enum EscalationReason {
        /** Quá mốc báo Quản lý viện (CFG-M15-06 hoặc tham số riêng). */
        OVERDUE,
        /** Tới mốc nhắc mà không còn ai có quyền duyệt (BR-M15-09). */
        NO_APPROVER_LEFT
    }

    /** {@code remind}: gửi nhắc người duyệt; {@code escalation}: báo Quản lý viện, {@code null} là không. */
    record Decision(boolean remind, EscalationReason escalation) {

        public static final Decision NONE = new Decision(false, null);
    }
}
