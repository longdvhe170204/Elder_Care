package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceNotices;
import com.eldercare.backend.billing.balance.internal.status.domain.Notice;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Test luồng BF-16 (2026-11-22): thông báo số dư tới người thân là việc của chính người thân (khoản phải trả; nghiep-vu
 * 14.x, Q-353, Q-362), không theo mục "nhận thông báo" của bản đồng ý; thông báo cho Kế toán, Quản lý viện không trùng
 * khóa sự kiện với thông báo cho người đại diện (009 FR-005 bỏ yêu cầu trùng khóa).
 */
class BalanceNoticesTest {

    private static final BalanceNotices.Facts IN_DEBT = new BalanceNotices.Facts(
            9004,
            "Lê Văn Thử",
            BalanceStatus.IN_DEBT,
            -500_000,
            0,
            null,
            LocalDate.parse("2026-11-01"),
            "2026-11-23",
            Instant.parse("2026-11-23T02:00:00Z"));

    private static List<NotificationRequest> requests() {
        return BalanceNotices.of(IN_DEBT, EnumSet.of(Notice.STATUS, Notice.MANAGER, Notice.PAYMENT_RECORDED))
                .notificationRequests();
    }

    @Test
    void familyNoticesAreAboutTheFamilysOwnMatter() {
        assertThat(requests())
                .filteredOn(r -> r.recipientGroups().contains(new RecipientGroup.Representative(9004)))
                .hasSize(2)
                .allSatisfy(r -> assertThat(r.informationSubject()).contains(InformationSubject.ABOUT_FAMILY_MATTER));
    }

    @Test
    void everyRequestHasItsOwnEventKey() {
        List<String> keys = requests().stream()
                .map(r -> r.sourceRecord().type() + "|" + r.sourceRecord().id() + "|" + r.eventKey())
                .toList();

        assertThat(keys).hasSize(4).doesNotHaveDuplicates();
    }
}
