package com.eldercare.backend.notification.internal.urgent.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.notification.internal.urgent.domain.CallReminderTarget.Tier;
import java.time.Instant;

/**
 * Dòng con của lượt nhắc gọi ở {@code record_event}, đối tượng {@code M13.CALL_REMINDER} (009 T043, data-model mục 0.1;
 * FR-027 → FR-032): thông báo gộp vào lượt, chuyển nhắc, kết quả gọi.
 */
public final class CallReminderEvents {

    /** Loại đối tượng của dòng bảng chung gắn với lượt nhắc gọi. */
    public static final String CALL_REMINDER = "M13.CALL_REMINDER";

    private CallReminderEvents() {}

    /** Thông báo gộp vào lượt; mỗi thông báo thuộc tối đa một lượt ({@link #uniqueKey}, FR-032). */
    public record Link(long notificationId) {

        public static final SharedKind<Link> KIND = SharedKind.of("M13.CALL_REMINDER_NOTIFICATION", Link.class);

        public static String uniqueKey(long notificationId) {
            return "M13.CRN:" + notificationId;
        }
    }

    /** Chuyển nhắc (FR-030a); thời điểm ở {@code occurred_at}. */
    public record Handoff(Long previousStaffId, long remindedStaffId, Tier tier, String reason, Instant dueAt) {

        public static final SharedKind<Handoff> KIND = SharedKind.of("M13.CALL_REMINDER_HANDOFF", Handoff.class);
    }

    /**
     * Kết quả gọi (FR-030, FR-031); người gọi ở {@code actor_id}, thời điểm ghi ở {@code occurred_at}; Không liên lạc được
     * bắt buộc lý do, Đã liên lạc được bắt buộc ghi chú. Đích của đính chính {@code M13.CALL_RESULT}.
     */
    public record Result(
            Instant calledAt,
            String calledPhoneNumber,
            String result,
            String reason,
            String answeredBy,
            String note,
            boolean supplementary) {

        public static final SharedKind<Result> KIND = SharedKind.of("M13.CALL_RESULT", Result.class);

        public Result {
            if ("NOT_REACHED".equals(result) && reason == null) {
                throw new IllegalArgumentException("Không liên lạc được cần lý do");
            }
            if ("REACHED".equals(result) && note == null) {
                throw new IllegalArgumentException("Đã liên lạc được cần ghi chú");
            }
        }
    }
}
