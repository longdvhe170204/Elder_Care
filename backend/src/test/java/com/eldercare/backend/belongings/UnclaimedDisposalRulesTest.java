package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.ending.domain.UnclaimedDisposalRules;
import com.eldercare.backend.belongings.internal.ending.domain.UnclaimedDisposalRules.Check;
import com.eldercare.backend.belongings.internal.ending.domain.UnclaimedDisposalRules.Contact;
import com.eldercare.backend.belongings.internal.ending.domain.UnclaimedDisposalRules.DisposalItem;
import com.eldercare.backend.belongings.internal.ending.domain.UnclaimedDisposalRules.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 013 T022 ({@code BR_M12_08_XuLyDoIT} phần hàm thuần; FR-027a, Q-152, Q-157, Q-159, CFG-M12-03, CFG-M12-07): kiểm nội
 * dung đề nghị "Xử lý đồ không người nhận".
 */
class UnclaimedDisposalRulesTest {

    private static final Instant NOW = Instant.parse("2026-06-01T03:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-03-01T03:00:00Z");
    private static final Duration THRESHOLD = Duration.ofDays(90);
    private static final Duration GAP = Duration.ofDays(7);

    private static Check check(Method method, List<DisposalItem> items, List<Contact> contacts, boolean minutes) {
        return new Check(NOW, THRESHOLD, ENDED, method, items, minutes, List.of(1L, 2L), contacts, 2, GAP);
    }

    private static List<Contact> enoughContacts() {
        Instant first = NOW.minus(Duration.ofDays(20));
        Instant second = NOW.minus(Duration.ofDays(10));
        return List.of(
                new Contact(1L, first, "Không nghe máy"),
                new Contact(1L, second, "Hẹn gặp"),
                new Contact(2L, first, "Không nghe máy"),
                new Contact(2L, second, "Từ chối nhận"));
    }

    private static DisposalItem item(BelongingStatus status, boolean cash, boolean valuable, boolean photo) {
        return new DisposalItem(1, status, cash, valuable, photo, null);
    }

    @Test
    void waitingPeriodRunsFromTheFoundTimeWhenTheItemWasFoundLater() {
        DisposalItem found =
                new DisposalItem(1, BelongingStatus.HELD, false, false, false, NOW.minus(Duration.ofDays(10)));

        assertThat(UnclaimedDisposalRules.violations(check(Method.SELL, List.of(found), enoughContacts(), true)))
                .contains("DISPOSAL_TOO_EARLY");
    }

    @Test
    void valuableItemsNeedAPhotoInTheRequest() {
        assertThat(UnclaimedDisposalRules.violations(check(
                        Method.SELL, List.of(item(BelongingStatus.HELD, false, true, false)), enoughContacts(), true)))
                .contains("PHOTO_REQUIRED");
    }

    @Test
    void minutesScanIsRequired() {
        assertThat(UnclaimedDisposalRules.violations(check(
                        Method.SELL,
                        List.of(item(BelongingStatus.HELD, false, false, false)),
                        enoughContacts(),
                        false)))
                .contains("MINUTES_REQUIRED");
    }
}
