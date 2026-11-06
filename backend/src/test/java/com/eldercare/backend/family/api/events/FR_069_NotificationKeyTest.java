package com.eldercare.backend.family.api.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 012 T040, FR-069 (009 FR-043a, FR-001a): khóa sự kiện của thông báo là (bản ghi nguồn, loại sự kiện), nhắc lặp thêm số
 * thứ tự lần nhắc; dòng tới người thân là "về việc của chính người thân"; không dòng nào mang mã Q-354. FR-070: nội dung
 * rút gọn cho tin nhắn không chứa thông tin sức khỏe.
 */
class FR_069_NotificationKeyTest {

    private static final Instant AT = Instant.parse("2026-11-06T02:00:00Z");

    private static NotificationRequest only(List<NotificationRequest> requests) {
        assertThat(requests).hasSize(1);
        NotificationRequest request = requests.getFirst();
        assertThat(request.sourceModule()).isEqualTo("M10");
        assertThat(request.q354Case()).isEmpty();
        assertThat(request.eventKey())
                .startsWith(request.sourceRecord().type() + ":"
                        + request.sourceRecord().id());
        return request;
    }

    private static FeedbackNotice feedback(FeedbackNotice.Kind kind, String occurrence) {
        return new FeedbackNotice(7L, 3L, kind, new RecipientGroup.FamilyMember(5L, "SENDER"), false, occurrence, AT);
    }

    @Test
    void FR_069_feedback_keyIsSourceAndEvent_repeatsDifferByOccurrence() {
        NotificationRequest first =
                only(feedback(FeedbackNotice.Kind.ESCALATED, "1").notificationRequests());
        NotificationRequest second =
                only(feedback(FeedbackNotice.Kind.ESCALATED, "2").notificationRequests());
        NotificationRequest replied =
                only(feedback(FeedbackNotice.Kind.REPLIED, "1").notificationRequests());

        assertThat(first.eventKey()).contains("ESCALATED").isNotEqualTo(second.eventKey());
        assertThat(replied.eventKey()).isNotEqualTo(first.eventKey());
        assertThat(only(feedback(FeedbackNotice.Kind.ESCALATED, "1").notificationRequests())
                        .eventKey())
                .as("cùng lần nhắc thì cùng khóa (009 bỏ trùng)")
                .isEqualTo(first.eventKey());
        assertThat(replied.informationSubject()).contains(InformationSubject.ABOUT_FAMILY_MATTER);
        assertThat(first.informationSubject()).as("leo thang là nội bộ").isEmpty();
    }

    @Test
    void FR_069_stay_endReminderIsRepeated_resultGoesToTheFamilyMember() {
        NotificationRequest reminder = only(
                new FamilyStayNotice(9L, 3L, 5L, FamilyStayNotice.Kind.END_REMINDER, 2L, AT).notificationRequests());
        NotificationRequest later =
                only(new FamilyStayNotice(9L, 3L, 5L, FamilyStayNotice.Kind.END_REMINDER, 2L, AT.plusSeconds(3600))
                        .notificationRequests());
        NotificationRequest confirmed =
                only(new FamilyStayNotice(9L, 3L, 5L, FamilyStayNotice.Kind.CONFIRMED, 2L, AT).notificationRequests());

        assertThat(reminder.eventKey()).isNotEqualTo(later.eventKey());
        assertThat(confirmed.eventKey()).isEqualTo("M10.FAMILY_STAY:9:CONFIRMED");
        assertThat(confirmed.informationSubject()).contains(InformationSubject.ABOUT_FAMILY_MATTER);
        assertThat(reminder.informationSubject()).isEmpty();
    }

    @Test
    void FR_069_cancelledVisit_isAboutTheRegistrantsOwnMatter() {
        NotificationRequest request = only(new VisitCancelledNotice(
                        11L,
                        3L,
                        5L,
                        LocalDate.of(2026, 11, 6),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0),
                        "RESIDENT_NOT_PRESENT",
                        AT)
                .notificationRequests());

        assertThat(request.eventKey()).isEqualTo("M10.VISIT:11:CANCELLED");
        assertThat(request.informationSubject()).isEqualTo(Optional.of(InformationSubject.ABOUT_FAMILY_MATTER));
    }

    @Test
    void FR_070_healthReply_shortContentIsTheGeneralPartOnly() {
        NotificationRequest request = only(new FeedbackNotice(
                        7L,
                        3L,
                        FeedbackNotice.Kind.REPLIED,
                        new RecipientGroup.FamilyMember(5L, "SENDER"),
                        true,
                        "1",
                        AT)
                .notificationRequests());

        assertThat(request.content())
                .extracting(ContentPart::type)
                .containsExactly(InformationType.HEALTH, InformationType.GENERAL);
        String health = request.content().getFirst().content();
        assertThat(request.shortContent())
                .hasValueSatisfying(text -> assertThat(text)
                        .isEqualTo(request.content().getLast().content())
                        .isNotEqualTo(health));
    }
}
