package com.eldercare.backend.belongings.internal.returns.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Một lần liên hệ người đại diện làm bằng chứng cho duyệt thay ({@code bang_chung_lien_he} → {@code record_event} loại
 * {@link #KIND}, cha là xác nhận người nhận khác; R-15, data-model mục 0, 5; FR-017a): thời điểm, kênh, kết quả.
 */
public record ContactAttempt(long familyMemberId, ContactChannel channel, Instant at, String result, long recordedBy) {

    public static final SharedKind<ContactAttempt> KIND = SharedKind.of("M12.CONTACT_EVIDENCE", ContactAttempt.class);
}
