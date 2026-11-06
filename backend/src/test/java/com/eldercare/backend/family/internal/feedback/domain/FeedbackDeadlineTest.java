package com.eldercare.backend.family.internal.feedback.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.family.internal.feedback.domain.Feedback.AssigneeRole;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.ContentGroup;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Priority;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Status;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Type;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Hạn xử lý phản hồi ({@code HanPhanHoiTest}; 012 T029, Q43; FR-059, FR-061 → FR-064, BR-M10-05): mức mặc định theo loại,
 * hạn từ mốc tính hạn theo CFG-M10-01, leo thang một lần mỗi hạn, tự đóng sau CFG-M10-02.
 */
class FeedbackDeadlineTest {

    private static final FeedbackDeadline.Durations CFG =
            new FeedbackDeadline.Durations(Duration.ofHours(24), Duration.ofHours(72));
    private static final Instant SENT = Instant.parse("2031-11-01T02:00:00Z");

    @Test
    void FR_062_complaintIsHigh_othersNormal() {
        assertThat(FeedbackDeadline.defaultPriority(Type.COMPLAINT)).isEqualTo(Priority.HIGH);
        assertThat(FeedbackDeadline.defaultPriority(Type.SUGGESTION)).isEqualTo(Priority.NORMAL);
        assertThat(FeedbackDeadline.defaultPriority(Type.QUESTION)).isEqualTo(Priority.NORMAL);
    }

    @Test
    void FR_061_dueFromBasisByPriority() {
        assertThat(FeedbackDeadline.dueAt(SENT, Priority.HIGH, CFG)).isEqualTo(SENT.plus(Duration.ofHours(24)));
        assertThat(FeedbackDeadline.dueAt(SENT, Priority.NORMAL, CFG)).isEqualTo(SENT.plus(Duration.ofHours(72)));
    }

    @Test
    void FR_059_defaultAssigneeByGroup() {
        assertThat(FeedbackDeadline.defaultAssignee(ContentGroup.CARE)).isEqualTo(AssigneeRole.FLOOR_MANAGER);
        assertThat(FeedbackDeadline.defaultAssignee(ContentGroup.EATING)).isEqualTo(AssigneeRole.FLOOR_MANAGER);
        assertThat(FeedbackDeadline.defaultAssignee(ContentGroup.HEALTH_MEDICATION))
                .isEqualTo(AssigneeRole.FLOOR_MANAGER);
        assertThat(FeedbackDeadline.defaultAssignee(ContentGroup.COSTS_CONTRACT))
                .isEqualTo(AssigneeRole.OFFICE_STAFF);
        assertThat(FeedbackDeadline.defaultAssignee(ContentGroup.OTHER)).isEqualTo(AssigneeRole.OFFICE_STAFF);
    }

    @Test
    void BR_M10_05_escalateOncePerDue() {
        Instant due = FeedbackDeadline.dueAt(SENT, Priority.HIGH, CFG);

        assertThat(FeedbackDeadline.shouldEscalate(Status.NEW, due, null, due.minusSeconds(1)))
                .isFalse();
        assertThat(FeedbackDeadline.shouldEscalate(Status.NEW, due, null, due)).isTrue();
        assertThat(FeedbackDeadline.shouldEscalate(Status.IN_PROGRESS, due, null, due.plusSeconds(60)))
                .isTrue();
        // đã leo thang cho đúng hạn này: không nhắc lặp (FR-063)
        assertThat(FeedbackDeadline.shouldEscalate(Status.NEW, due, due, due.plus(Duration.ofDays(3))))
                .isFalse();
        // hạn mới sau Mở lại: leo thang lại
        Instant newDue = due.plus(Duration.ofDays(2));
        assertThat(FeedbackDeadline.shouldEscalate(Status.REOPENED, newDue, due, newDue))
                .isTrue();
        // đã trả lời, đã đóng thì không leo thang
        assertThat(FeedbackDeadline.shouldEscalate(Status.REPLIED, due, null, due))
                .isFalse();
        assertThat(FeedbackDeadline.shouldEscalate(Status.CLOSED, due, null, due))
                .isFalse();
    }

    @Test
    void FR_064_autoCloseAfterCfg() {
        Duration week = Duration.ofDays(7);
        Instant replied = SENT.plus(Duration.ofHours(5));

        assertThat(FeedbackDeadline.shouldAutoClose(Status.REPLIED, replied, week, replied.plus(week)))
                .isTrue();
        assertThat(FeedbackDeadline.shouldAutoClose(
                        Status.REPLIED, replied, week, replied.plus(week).minusSeconds(1)))
                .isFalse();
        assertThat(FeedbackDeadline.shouldAutoClose(Status.REOPENED, replied, week, replied.plus(week)))
                .isFalse();
    }
}
