package com.eldercare.backend.family.internal.feedback.domain;

import static org.assertj.core.api.Assertions.assertThat;

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
