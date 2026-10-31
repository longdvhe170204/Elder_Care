package com.eldercare.backend.notification.internal.directcontact.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Kết quả liên hệ trực tiếp ở {@code record_event} loại {@code M13.DIRECT_CONTACT_RESULT}, đối tượng là mục liên hệ
 * ({@link DirectContactContent#KIND}) (009 T043; FR-021, FR-031): một kết quả mỗi mục ({@link #uniqueKey}); người liên hệ
 * ở {@code actor_id}, thời điểm ghi ở {@code occurred_at}; Không liên hệ được bắt buộc lý do. Đích của đính chính
 * {@code M13.DIRECT_CONTACT_RESULT}.
 */
public record DirectContactResultContent(Instant occurredAt, String result, String reason, String note) {

    public static final SharedKind<DirectContactResultContent> KIND =
            SharedKind.of("M13.DIRECT_CONTACT_RESULT", DirectContactResultContent.class);

    public DirectContactResultContent {
        if ("UNREACHABLE".equals(result) && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Không liên hệ được cần lý do");
        }
    }

    public static String uniqueKey(long directContactId) {
        return "M13.DCR:" + directContactId;
    }
}
