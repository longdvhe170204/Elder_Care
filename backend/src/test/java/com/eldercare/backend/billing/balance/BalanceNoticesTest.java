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
            Instant.parse("2026-11-23T02:00:00Z"),
            false);

    private static List<NotificationRequest> requests() {
        return BalanceNotices.of(IN_DEBT, EnumSet.of(Notice.STATUS, Notice.MANAGER, Notice.PAYMENT_RECORDED))
                .notificationRequests();
    }

    // Demo 2026-11-26: nộp tiền báo bằng thông báo biến động số dư (TransactionNotices), không còn PAYMENT_RECORDED
    // riêng
    @Test
    void familyNoticesAreAboutTheFamilysOwnMatter() {
        assertThat(requests())
                .filteredOn(r -> r.recipientGroups().contains(new RecipientGroup.Representative(9004)))
                .hasSize(1)
                .allSatisfy(r -> assertThat(r.informationSubject()).contains(InformationSubject.ABOUT_FAMILY_MATTER));
    }

    @Test
    void everyRequestHasItsOwnEventKey() {
        List<String> keys = requests().stream()
                .map(r -> r.sourceRecord().type() + "|" + r.sourceRecord().id() + "|" + r.eventKey())
                .toList();

        assertThat(keys).hasSize(3).doesNotHaveDuplicates();
    }

    /**
     * Góp ý test BF-16 bước 8 (2026-11-25): tiêu đề cho nhân viên nêu tình trạng (trước luôn "Số dư"); lần nhắc theo chu kỳ
     * ghi "vẫn" thay "chuyển"; ngày còn nợ dạng dd/MM/yyyy (trước 2026-11-01). Người đại diện giữ tiêu đề trung tính vì
     * người không có quyền xem chi phí chỉ nhận phần chung.
     */
    @Test
    void staffTitlesNameTheStatus_reminderSaysStill_dateIsVietnamese() {
        List<NotificationRequest> first = requests();
        assertThat(staffRequest(first, "M11_BALANCE_IN_DEBT").title()).contains("Còn nợ");
        assertThat(staffRequest(first, "M11_BALANCE_IN_DEBT").shortContent())
                .contains("Lê Văn Thử chuyển Còn nợ: số dư -500.000 đồng, chi phí chưa chốt 0 đồng.");
        NotificationRequest manager = staffRequest(first, "M11_BALANCE_IN_DEBT_MANAGER");
        assertThat(manager.title()).contains("Còn nợ kéo dài");
        assertThat(manager.shortContent()).contains("Lê Văn Thử còn nợ liên tục từ 01/11/2026: số dư -500.000 đồng.");
        assertThat(first)
                .filteredOn(r -> r.recipientGroups().contains(new RecipientGroup.Representative(9004)))
                .allSatisfy(r -> assertThat(r.title()).contains("Số dư"));

        BalanceNotices.Facts again = new BalanceNotices.Facts(
                9004,
                "Lê Văn Thử",
                BalanceStatus.RUNNING_LOW,
                1_866_667,
                0,
                null,
                null,
                "2026-12-02",
                Instant.parse("2026-12-02T02:00:00Z"),
                true);
        NotificationRequest reminder = staffRequest(
                BalanceNotices.of(again, EnumSet.of(Notice.STATUS)).notificationRequests(), "M11_BALANCE_RUNNING_LOW");
        assertThat(reminder.title()).contains("Sắp hết tiền");
        assertThat(reminder.shortContent())
                .contains("Nhắc lại: Lê Văn Thử vẫn Sắp hết tiền: số dư 1.866.667 đồng, chi phí chưa chốt 0 đồng.");
    }

    private static NotificationRequest staffRequest(List<NotificationRequest> requests, String eventType) {
        return requests.stream()
                .filter(r -> r.eventType().equals(eventType))
                .filter(r -> r.recipientGroups().stream().anyMatch(g -> g instanceof RecipientGroup.FacilityRole))
                .findFirst()
                .orElseThrow();
    }
}
