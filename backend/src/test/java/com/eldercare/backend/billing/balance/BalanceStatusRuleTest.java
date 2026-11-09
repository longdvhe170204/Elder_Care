package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceStatusRule;
import com.eldercare.backend.billing.balance.internal.status.domain.StatusComputation;
import com.eldercare.backend.support.TableRows;
import org.junit.jupiter.api.Test;

/**
 * 017 T018 (tên logic {@code TinhTrangTest}; FR-016, FR-017; BR-M11-13; research E-07): số ngày còn đủ tiền = (số dư −
 * tổng chưa chốt) ÷ phí một ngày, làm tròn xuống; Còn nợ khi phần còn lại âm; Sắp hết tiền khi số ngày nhỏ hơn
 * CFG-M11-04; phí bằng 0 hoặc chưa biết thì chỉ xét Còn nợ.
 */
class BalanceStatusRuleTest {

    @Test
    void BR_M11_13_statusFollowsAvailableMoneyAndDailyFee() {
        TableRows.csv(
                row -> {
                    StatusComputation result = BalanceStatusRule.compute(
                            Long.parseLong(row[0]),
                            Long.parseLong(row[1]),
                            row[2] == null ? null : Long.valueOf(row[2]),
                            Integer.parseInt(row[3]));
                    assertThat(result.status()).isEqualTo(BalanceStatus.valueOf(row[4]));
                    assertThat(result.daysCovered()).isEqualTo(row[5] == null ? null : Integer.valueOf(row[5]));
                },
                // số dư, chưa chốt, phí một ngày (rỗng: chưa biết), CFG-M11-04, tình trạng, số ngày còn đủ tiền
                "6000000, 2400000, 300000, 15, RUNNING_LOW, 12",
                "9000000, 0, 300000, 15, NORMAL, 30",
                "4500000, 0, 300000, 15, NORMAL, 15",
                "4499999, 0, 300000, 15, RUNNING_LOW, 14",
                "0, 0, 300000, 15, RUNNING_LOW, 0",
                "1000000, 1000001, 300000, 15, IN_DEBT,",
                "-1, 0, 0, 15, IN_DEBT,",
                "5000, 0, 0, 15, NORMAL,",
                "5000, 0, , 15, NORMAL,");
    }
}
