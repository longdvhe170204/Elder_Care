package com.cambae.backend.m13.internal.intake.domain;

import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Kiểm tra yêu cầu thông báo khi tiếp nhận (FR-001a, FR-003, FR-013, FR-015; research B-02). Thiếu trường bắt buộc thì
 * từ chối; thiếu mức, loại thông tin, đối tượng thông tin thì bổ sung mặc định hạn chế hơn và gắn dấu.
 */
public final class RequestValidation {

    private RequestValidation() {}

    public static ValidatedRequest validate(NotificationRequest r) {
        Set<RequestFlag> flags = EnumSet.noneOf(RequestFlag.class);

        NotificationLevel level = optional(r.level()).orElse(null);
        if (level == null) {
            level = NotificationLevel.LOW;
            flags.add(RequestFlag.MISSING_LEVEL);
        }

        List<ContentPart> content = r.content() == null
                ? List.of()
                : r.content().stream()
                        .map(p -> p.type() != null ? p : new ContentPart(InformationType.HEALTH, p.content()))
                        .toList();
        if (r.content() != null && r.content().stream().anyMatch(p -> p.type() == null)) {
            flags.add(RequestFlag.MISSING_INFORMATION_TYPE);
        }

        if (level != NotificationLevel.LOW
                && optional(r.shortContent()).filter(s -> !s.isBlank()).isEmpty()) {
            flags.add(RequestFlag.MISSING_SHORT_CONTENT);
        }

        Optional<InformationSubject> subject = optional(r.informationSubject());
        List<RecipientGroup> groups = r.recipientGroups() == null ? List.of() : r.recipientGroups();
        if (groups.stream().anyMatch(RequestValidation::isFamilyGroup) && subject.isEmpty()) {
            subject = Optional.of(InformationSubject.ABOUT_RESIDENT);
            flags.add(RequestFlag.MISSING_INFORMATION_SUBJECT);
        }

        if (r.afterDeath()) {
            flags.add(RequestFlag.AFTER_DEATH);
        }

        Optional<String> rejection = rejection(r, groups, content);
        return new ValidatedRequest(
                r,
                rejection.isPresent() ? IntakeResult.REJECTED : IntakeResult.VALID,
                rejection,
                level,
                content,
                subject,
                Collections.unmodifiableSet(flags));
    }

    private static Optional<String> rejection(
            NotificationRequest r, List<RecipientGroup> groups, List<ContentPart> content) {
        if (r.sourceRecord() == null
                || blank(r.sourceRecord().type())
                || blank(r.sourceRecord().id())) {
            return Optional.of("MISSING_SOURCE_RECORD");
        }
        if (blank(r.eventKey())) {
            return Optional.of("MISSING_EVENT_KEY");
        }
        if (groups.isEmpty()) {
            return Optional.of("MISSING_RECIPIENT_GROUPS");
        }
        if (content.isEmpty() || content.stream().anyMatch(p -> blank(p.content()))) {
            return Optional.of("MISSING_CONTENT");
        }
        return Optional.empty();
    }

    /** Nhóm có người thân (loại (e), hoặc (d) người thân), cần khai đối tượng thông tin (FR-001a). */
    private static boolean isFamilyGroup(RecipientGroup g) {
        return g instanceof RecipientGroup.PrimaryContact
                || g instanceof RecipientGroup.Representative
                || g instanceof RecipientGroup.FamilyWithRight
                || g instanceof RecipientGroup.FamilyMember;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static <T> Optional<T> optional(Optional<T> o) {
        return o == null ? Optional.empty() : o;
    }
}
