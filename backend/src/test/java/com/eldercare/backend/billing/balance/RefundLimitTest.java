package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.transaction.domain.RefundLimit;
import com.eldercare.backend.billing.balance.internal.transaction.domain.RefundLimit.Kind;
import com.eldercare.backend.billing.balance.internal.transaction.domain.RefundLimit.Outcome;
import com.eldercare.backend.support.TableRows;
import org.junit.jupiter.api.Test;

/** 017 T023 (FR-014, BR-M11-14; quickstart Q11, Q12): giới hạn số tiền hoàn tiền, hoàn cọc, mọi nhánh. */
class RefundLimitTest {

    @Test
    void FR_014_maximumByKindAndStatus() {
        // loại, số dư, số dư cọc, tổng chưa chốt, trạng thái cuối, mức tối đa
        TableRows.csv(
                r -> assertThat(RefundLimit.maximum(
                                Kind.valueOf(r[0]),
                                Long.parseLong(r[1]),
                                Long.parseLong(r[2]),
                                Long.parseLong(r[3]),
                                Boolean.parseBoolean(r[4])))
                        .isEqualTo(Long.parseLong(r[5])),
                // đang lưu trú: số dư trừ tổng chưa chốt (US6 #7: 5.000.000 − 4.000.000)
                "REFUND, 5000000, 0, 4000000, false, 1000000",
                "REFUND, 2000000, 0, 0, false, 2000000",
                // chi phí chưa chốt lớn hơn số dư: không được hoàn
                "REFUND, 2000000, 0, 3000000, false, 0",
                // số dư âm (còn nợ): không được hoàn
                "REFUND, -500000, 0, 0, false, 0",
                // trạng thái cuối: chỉ giới hạn bởi số dư, bỏ qua chi phí chưa chốt
                "REFUND, 5000000, 0, 4000000, true, 5000000",
                "REFUND, -500000, 0, 0, true, 0",
                // hoàn cọc chỉ giới hạn bởi số dư cọc, không phụ thuộc số dư và chi phí
                "DEPOSIT_REFUND, 0, 3000000, 9000000, false, 3000000",
                "DEPOSIT_REFUND, -100, 3000000, 0, true, 3000000",
                "DEPOSIT_REFUND, 5000000, 0, 0, false, 0");
    }

    @Test
    void FR_014_checkRejectsNonPositiveAndOverLimit() {
        // loại, số dư, số dư cọc, tổng chưa chốt, trạng thái cuối, số tiền, kết quả
        TableRows.csv(
                r -> assertThat(RefundLimit.check(
                                Kind.valueOf(r[0]),
                                Long.parseLong(r[1]),
                                Long.parseLong(r[2]),
                                Long.parseLong(r[3]),
                                Boolean.parseBoolean(r[4]),
                                Long.parseLong(r[5])))
                        .isEqualTo(Outcome.valueOf(r[6])),
                // US6 #7: xin rút 2.000.000 thì chặn, 1.000.000 thì được
                "REFUND, 5000000, 0, 4000000, false, 2000000, EXCEEDED",
                "REFUND, 5000000, 0, 4000000, false, 1000000, ALLOWED",
                "REFUND, 5000000, 0, 4000000, false, 1000001, EXCEEDED",
                // Q11: số dư 2.000.000, hoàn 3.000.000 thì chặn; hoàn đúng số dư thì được
                "REFUND, 2000000, 0, 0, false, 3000000, EXCEEDED",
                "REFUND, 2000000, 0, 0, false, 2000000, ALLOWED",
                "DEPOSIT_REFUND, 0, 3000000, 0, false, 3000001, EXCEEDED",
                "DEPOSIT_REFUND, 0, 3000000, 0, false, 3000000, ALLOWED",
                // số tiền không dương
                "REFUND, 2000000, 0, 0, false, 0, INVALID_AMOUNT",
                "REFUND, 2000000, 0, 0, false, -1, INVALID_AMOUNT",
                "DEPOSIT_REFUND, 0, 3000000, 0, false, 0, INVALID_AMOUNT");
    }
}
