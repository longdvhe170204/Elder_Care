package com.eldercare.backend.notification.internal.directcontact.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Mục cần liên hệ trực tiếp ở {@code simple_record} loại {@code M13.DIRECT_CONTACT}, đối tượng {@code M13.NOTIFICATION}
 * (009 T043; FR-021): trạng thái {@code PENDING}, {@code CONTACTED}, {@code UNREACHABLE}; một mục mỗi thông báo
 * ({@link #uniqueKey}); khóa dòng khi rời Chờ liên hệ (thay {@code trg_direct_contact_final}).
 */
public record DirectContactContent(
        Long familyMemberId,
        String fullName,
        String relationship,
        String phoneNumber,
        String shortContent,
        Instant remindedAt) {

    public static final SharedKind<DirectContactContent> KIND =
            SharedKind.of("M13.DIRECT_CONTACT", DirectContactContent.class);

    public static final String PENDING = "PENDING";

    public static String uniqueKey(long notificationId) {
        return "M13.DC:" + notificationId;
    }

    public DirectContactContent reminded(Instant at) {
        return new DirectContactContent(familyMemberId, fullName, relationship, phoneNumber, shortContent, at);
    }
}
