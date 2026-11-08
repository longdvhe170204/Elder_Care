package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.billing.balance.internal.ledger.domain.DepositStatusRule;
import com.eldercare.backend.support.TableRows;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/** 017 T008 (tên logic {@code TrangThaiDatCocTest}; research E-05, FR-006): hàm thuần trạng thái đặt cọc. */
class DepositStatusTest {

    @Test
    void FR_006_depositStatus_followsRequiredAmountAndDepositBalance() {
        TableRows.csv(
                row -> {
                    OptionalLong required =
                            row[0] == null ? OptionalLong.empty() : OptionalLong.of(Long.parseLong(row[0]));
                    assertThat(DepositStatusRule.of(required, Long.parseLong(row[1])))
                            .isEqualTo(DepositStatus.valueOf(row[2]));
                },
                // khoản cần cọc (rỗng: hợp đồng không yêu cầu cọc), số dư cọc, trạng thái
                ", 0, NOT_REQUIRED",
                ", 5000000, NOT_REQUIRED",
                "10000000, 0, NOT_SATISFIED",
                "10000000, 9999999, NOT_SATISFIED",
                "10000000, 10000000, SATISFIED",
                "10000000, 12000000, SATISFIED",
                "10000000, -1, NOT_SATISFIED");
    }
}
