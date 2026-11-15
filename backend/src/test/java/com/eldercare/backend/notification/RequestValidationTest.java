package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.notification.internal.intake.domain.RequestFlag;
import com.eldercare.backend.notification.internal.intake.domain.RequestValidation;
import com.eldercare.backend.notification.internal.intake.domain.ValidatedRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** FR-001a, FR-003, research B-02 (009 T007): kiểm tra yêu cầu khi tiếp nhận. */
class RequestValidationTest {

    static NotificationRequest request(
            Optional<NotificationLevel> level,
            List<RecipientGroup> groups,
            List<ContentPart> content,
            Optional<String> shortContent,
            Optional<InformationSubject> subject) {
        return new NotificationRequest(
                "M05",
                "INCIDENT_CREATED",
                SourceGroup.INCIDENT,
                new SourceRecord("M05.INCIDENT", "1"),
                "created",
                level,
                Optional.of(10L),
                Optional.of(2L),
                groups,
                content,
                shortContent,
                Optional.of("Sự cố mới"),
                Optional.empty(),
                false,
                Instant.parse("2026-10-21T01:00:00Z"),
                Map.of(),
                false,
                subject,
                Optional.empty());
    }

    static final List<RecipientGroup> MANAGERS = List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
    static final List<ContentPart> GENERAL = List.of(new ContentPart(InformationType.GENERAL, "Có sự cố"));

    @Test
    void FR_013_partWithoutInformationType_isHealth() {
        ValidatedRequest v = RequestValidation.validate(request(
                Optional.of(NotificationLevel.LOW),
                MANAGERS,
                List.of(new ContentPart(null, "Huyết áp cao")),
                Optional.empty(),
                Optional.empty()));

        assertThat(v.content()).extracting(ContentPart::type).containsExactly(InformationType.HEALTH);
        assertThat(v.flags()).containsExactly(RequestFlag.MISSING_INFORMATION_TYPE);
    }

    @Test
    void FR_001a_familyGroupWithoutSubject_isAboutResidentWithFlag() {
        ValidatedRequest v = RequestValidation.validate(request(
                Optional.of(NotificationLevel.LOW),
                List.of(new RecipientGroup.PrimaryContact(10L)),
                GENERAL,
                Optional.empty(),
                Optional.empty()));
        ValidatedRequest staffOnly = RequestValidation.validate(
                request(Optional.of(NotificationLevel.LOW), MANAGERS, GENERAL, Optional.empty(), Optional.empty()));

        assertThat(v.informationSubject()).contains(InformationSubject.ABOUT_RESIDENT);
        assertThat(v.flags()).containsExactly(RequestFlag.MISSING_INFORMATION_SUBJECT);
        assertThat(staffOnly.informationSubject()).isEmpty();
    }

    @Test
    void FR_015_afterDeathMark_isKeptAsFlag() {
        NotificationRequest base =
                request(Optional.of(NotificationLevel.LOW), MANAGERS, GENERAL, Optional.empty(), Optional.empty());
        NotificationRequest afterDeath = new NotificationRequest(
                base.sourceModule(),
                base.eventType(),
                base.sourceGroup(),
                base.sourceRecord(),
                base.eventKey(),
                base.level(),
                base.residentId(),
                base.floorAreaId(),
                base.recipientGroups(),
                base.content(),
                base.shortContent(),
                base.title(),
                base.causedBy(),
                base.notifyCauser(),
                base.occurredAt(),
                base.displayParams(),
                true,
                base.informationSubject(),
                base.q354Case());

        assertThat(RequestValidation.validate(afterDeath).flags()).containsExactly(RequestFlag.AFTER_DEATH);
    }
}
