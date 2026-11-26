package com.eldercare.backend.administration.approval.api.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Demo 2026-11-26 (E): tiêu đề thông báo yêu cầu phê duyệt ghi tên chức năng thay mã yêu cầu; giao dịch số dư ghi đúng
 * loại (hoàn số dư, hoàn cọc, điều chỉnh); loại chưa có nhãn giữ "Yêu cầu" kèm mã.
 */
class NoticesTitleTest {

    private static final Instant AT = Instant.parse("2026-11-26T02:00:00Z");

    private static ApprovalRequestView request(String type, String content) {
        return new ApprovalRequestView(
                1,
                "YC-2026-000001",
                type,
                "PENDING_APPROVAL",
                JsonMapper.builder().build().readTree(content),
                new Target(type, "6", 9004L, null),
                9,
                AT,
                "Hoàn số dư khi kết thúc lưu trú",
                null,
                null,
                AT,
                null,
                null,
                null,
                false,
                null,
                null,
                false,
                null,
                null,
                null,
                1);
    }

    private static String titleOf(ApprovalRequestView request) {
        return new RequestSubmitted(request, List.of(5L), Optional.empty(), AT)
                .notificationRequests()
                .getFirst()
                .title()
                .orElseThrow();
    }

    @Test
    void balanceTransaction_titleNamesTheTransactionType() {
        assertThat(titleOf(request("M11.BALANCE_TRANSACTION", "{\"type\":\"REFUND\",\"amount\":-7916675}")))
                .isEqualTo("Hoàn số dư");
        assertThat(titleOf(request("M11.BALANCE_TRANSACTION", "{\"type\":\"DEPOSIT_REFUND\",\"amount\":-5000000}")))
                .isEqualTo("Hoàn tiền cọc");
    }

    @Test
    void otherKnownType_titleIsTheFunctionName_unknownKeepsTheCode() {
        assertThat(titleOf(request("M02.RESIDENCY_CHANGE", "{}"))).isEqualTo("Điều chỉnh hợp đồng");
        assertThat(titleOf(request("M99.SOMETHING", "{}"))).isEqualTo("Yêu cầu YC-2026-000001");
    }
}
