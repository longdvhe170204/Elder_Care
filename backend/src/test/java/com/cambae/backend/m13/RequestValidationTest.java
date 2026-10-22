package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.m13.internal.intake.domain.IntakeResult;
import com.cambae.backend.m13.internal.intake.domain.RequestFlag;
import com.cambae.backend.m13.internal.intake.domain.RequestValidation;
import com.cambae.backend.m13.internal.intake.domain.ValidatedRequest;
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
    void FR_003_validRequest_isValidWithoutFlags() {
        ValidatedRequest v = RequestValidation.validate(
                request(Optional.of(NotificationLevel.LOW), MANAGERS, GENERAL, Optional.empty(), Optional.empty()));

        assertThat(v.result()).isEqualTo(IntakeResult.VALID);
        assertThat(v.level()).isEqualTo(NotificationLevel.LOW);
        assertThat(v.flags()).isEmpty();
    }

    @Test
    void FR_003_missingLevel_isLowWithFlag_notRejected() {
        ValidatedRequest v = RequestValidation.validate(
                request(Optional.empty(), MANAGERS, GENERAL, Optional.empty(), Optional.empty()));

        assertThat(v.result()).isEqualTo(IntakeResult.VALID);
        assertThat(v.level()).isEqualTo(NotificationLevel.LOW);
        assertThat(v.flags()).containsExactly(RequestFlag.MISSING_LEVEL);
    }

    @Test
    void FR_003_missingRequiredFields_areRejected() {
        NotificationRequest noGroups =
                request(Optional.of(NotificationLevel.LOW), List.of(), GENERAL, Optional.empty(), Optional.empty());
        NotificationRequest noContent =
                request(Optional.of(NotificationLevel.LOW), MANAGERS, List.of(), Optional.empty(), Optional.empty());
        NotificationRequest blankContent = request(
                Optional.of(NotificationLevel.LOW),
                MANAGERS,
                List.of(new ContentPart(InformationType.GENERAL, " ")),
                Optional.empty(),
                Optional.empty());
        NotificationRequest base =
                request(Optional.of(NotificationLevel.LOW), MANAGERS, GENERAL, Optional.empty(), Optional.empty());
        NotificationRequest noSource = new NotificationRequest(
                base.sourceModule(),
                base.eventType(),
                base.sourceGroup(),
                null,
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
                base.afterDeath(),
                base.informationSubject(),
                base.q354Case());
        NotificationRequest noKey = new NotificationRequest(
                base.sourceModule(),
                base.eventType(),
                base.sourceGroup(),
                base.sourceRecord(),
                " ",
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
                base.afterDeath(),
                base.informationSubject(),
                base.q354Case());

        assertThat(RequestValidation.validate(noGroups).rejectionReason()).contains("MISSING_RECIPIENT_GROUPS");
        assertThat(RequestValidation.validate(noContent).rejectionReason()).contains("MISSING_CONTENT");
        assertThat(RequestValidation.validate(blankContent).rejectionReason()).contains("MISSING_CONTENT");
        assertThat(RequestValidation.validate(noSource).rejectionReason()).contains("MISSING_SOURCE_RECORD");
        assertThat(RequestValidation.validate(noKey).rejectionReason()).contains("MISSING_EVENT_KEY");
        assertThat(RequestValidation.validate(noKey).result()).isEqualTo(IntakeResult.REJECTED);
    }

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
    void FR_003_mediumOrUrgentWithoutShortContent_isFlagged() {
        ValidatedRequest medium = RequestValidation.validate(
                request(Optional.of(NotificationLevel.MEDIUM), MANAGERS, GENERAL, Optional.empty(), Optional.empty()));
        ValidatedRequest low = RequestValidation.validate(
                request(Optional.of(NotificationLevel.LOW), MANAGERS, GENERAL, Optional.empty(), Optional.empty()));

        assertThat(medium.result()).isEqualTo(IntakeResult.VALID);
        assertThat(medium.flags()).containsExactly(RequestFlag.MISSING_SHORT_CONTENT);
        assertThat(low.flags()).isEmpty();
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
