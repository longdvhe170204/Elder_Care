package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.status.domain.TransactionNotices;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Demo 2026-11-26 (E, chờ chủ dự án duyệt): mọi biến động số dư báo người đại diện, nêu loại, số tiền, số dư và tiền cọc
 * sau giao dịch; giao dịch bị từ chối, hủy cũng báo (số dư không đổi).
 */
class TransactionNoticesTest {

    private static final Instant AT = Instant.parse("2026-11-26T02:00:00Z");

    @Test
    void confirmed_representativeToldTypeAmountAndBalanceAfter() {
        var notice = TransactionNotices.confirmed(
                7,
                "Phạm Văn Dũng",
                List.of(new TransactionNotices.Line(TransactionType.CASH_PAYMENT, 10_000_000)),
                12_500_000,
                5_000_000,
                "31",
                AT);

        var request = notice.notificationRequests().getFirst();
        assertThat(request.recipientGroups()).containsExactly(new RecipientGroup.Representative(7));
        assertThat(request.informationSubject()).contains(InformationSubject.ABOUT_FAMILY_MATTER);
        assertThat(request.content()).anySatisfy(p -> {
            assertThat(p.type()).isEqualTo(InformationType.COSTS);
            assertThat(p.content())
                    .isEqualTo("Đã ghi nhận nộp tiền mặt 10.000.000 đ cho Phạm Văn Dũng. Số dư sau giao dịch:"
                            + " 12.500.000 đ; tiền cọc: 5.000.000 đ.");
        });
    }

    @Test
    void confirmed_refundOfBalanceAndDeposit_oneNotice_andDebtIsSpelledOut() {
        var notice = TransactionNotices.confirmed(
                7,
                "Phạm Văn Dũng",
                List.of(
                        new TransactionNotices.Line(TransactionType.REFUND, -7_916_675),
                        new TransactionNotices.Line(TransactionType.DEPOSIT_REFUND, -5_000_000)),
                -250_000,
                0,
                "40,41",
                AT);

        assertThat(notice.notificationRequests()).hasSize(1);
        assertThat(notice.notificationRequests().getFirst().content())
                .anySatisfy(p -> assertThat(p.content())
                        .isEqualTo("Đã ghi nhận hoàn số dư 7.916.675 đ, hoàn tiền cọc 5.000.000 đ cho Phạm Văn"
                                + " Dũng. Số dư sau giao dịch: -250.000 đ (còn nợ 250.000 đ); tiền cọc: 0 đ."));
    }

    @Test
    void closed_rejectedRefund_toldBalanceUnchanged() {
        var notice = TransactionNotices.closed(7, "Phạm Văn Dũng", TransactionType.REFUND, -500_000, true, "9", AT);

        assertThat(notice.notificationRequests().getFirst().content())
                .anySatisfy(p -> assertThat(p.content())
                        .isEqualTo("Yêu cầu hoàn số dư 500.000 đ cho Phạm Văn Dũng đã bị từ chối; số dư không đổi."));
    }
}
