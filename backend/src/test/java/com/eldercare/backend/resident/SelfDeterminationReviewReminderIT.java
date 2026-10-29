package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.api.events.SelfDeterminationReviewReminder;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWish;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T067 (FR-023i, Q-360; đồng hồ giả lập): tới ngày xem lại nhận định ghi trên phiếu Hiệu lực, hệ thống nhắc một lần
 * (Bác sĩ đã nhận định và Điều dưỡng phụ trách do 009 xếp); chạy lại không nhắc trùng; phiếu bị thay bằng phiên bản mới
 * trước ngày đó thì không nhắc; hồ sơ ở trạng thái cuối thì không nhắc. Nhắc không đổi phiếu.
 */
class SelfDeterminationReviewReminderIT extends WishTestBase {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    TestClockControl clock;

    @Autowired
    ApplicationEvents events;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    private LocalDate today() {
        return time.now().atZone(BUSINESS_ZONE).toLocalDate();
    }

    private EndOfLifeWish wishReviewedIn(long residentId, int days) {
        return record(command(residentId)
                .assessment(true, "Hỏi lại từng ý bằng lời", today().plusDays(days))
                .build());
    }

    private long reminders(long residentId) {
        return events.stream(SelfDeterminationReviewReminder.class)
                .filter(e -> e.residentId() == residentId)
                .count();
    }

    @Test
    void FR_023i_remindsOnceOnTheReviewDate_notBefore() {
        long id = newResident();
        EndOfLifeWish wish = wishReviewedIn(id, 10);

        clock.advanceBy(Duration.ofDays(8));
        wishReminders.runReviews();
        assertThat(reminders(id)).as("chưa tới ngày xem lại").isZero();

        clock.advanceBy(Duration.ofDays(3));
        wishReminders.runReviews();
        wishReminders.runReviews();
        clock.advanceBy(Duration.ofDays(5));
        wishReminders.runReviews();

        assertThat(events.stream(SelfDeterminationReviewReminder.class)
                        .filter(e -> e.residentId() == id)
                        .map(SelfDeterminationReviewReminder::wishId)
                        .toList())
                .as("đúng một nhắc cho phiếu này")
                .containsExactly(wish.id());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM periodic_reminder WHERE type = 'REVIEW_SELF_DETERMINATION'"
                                + " AND resident_id = ? AND milestone_id = ?",
                        Integer.class,
                        id,
                        wish.id()))
                .isEqualTo(1);
        assertThat(wishes.get(target(id)).current())
                .hasValueSatisfying(w -> assertThat(w.id()).isEqualTo(wish.id()));
    }

    @Test
    void FR_023i_supersededBeforeTheReviewDate_doesNotRemind_theNewVersionDoes() {
        long id = newResident();
        wishReviewedIn(id, 10);
        EndOfLifeWish newer = recordAs(
                WishRecordSource.FAMILY_REQUEST,
                command(id)
                        .assessment(true, "Hỏi lại từng ý bằng lời", today().plusDays(20))
                        .build());

        clock.advanceBy(Duration.ofDays(12));
        wishReminders.runReviews();
        assertThat(reminders(id)).as("bản cũ đã được thay thế").isZero();

        clock.advanceBy(Duration.ofDays(10));
        wishReminders.runReviews();
        assertThat(events.stream(SelfDeterminationReviewReminder.class)
                        .filter(e -> e.residentId() == id)
                        .map(SelfDeterminationReviewReminder::wishId)
                        .toList())
                .containsExactly(newer.id());
    }

    @Test
    void FR_023i_finalStatusProfile_isNotReminded() {
        long id = newResident();
        wishReviewedIn(id, 5);
        setStatus(id, ResidentStatus.DECEASED.name());

        clock.advanceBy(Duration.ofDays(6));
        wishReminders.runReviews();

        assertThat(reminders(id)).isZero();
    }

    @Test
    void FR_023i_reminderDoesNotBlockAnyCommand_andTheWishStaysActive() {
        long id = newResident();
        wishReviewedIn(id, 1);
        clock.advanceBy(Duration.ofDays(3));
        wishReminders.runReviews();
        assertThat(reminders(id)).isEqualTo(1);

        recordAs(WishRecordSource.FAMILY_REQUEST, command(id).build());

        assertThat(activeWishCount(id)).isEqualTo(1);
    }
}
