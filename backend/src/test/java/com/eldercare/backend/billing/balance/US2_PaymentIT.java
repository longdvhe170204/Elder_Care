package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.admission.api.events.DepositStatusChanged;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;

/**
 * 017 T012, T013 (US2 #1 → #6, US3 #1 → #3; FR-005 → FR-007; BR-M11-12, 16; DBR-29; quickstart Q3 → Q6): tiền mặt, chuyển
 * khoản Đã xác nhận ngay; trạng thái đặt cọc đổi khi đủ khoản cọc và báo 004 kèm giao dịch, Kế toán; hai dòng cùng mã
 * ngân hàng trong một lệnh; mã đã dùng thì 409 kèm giao dịch trước, gửi lại có xác nhận thì lưu.
 */
class US2_PaymentIT extends BalanceTestBase {

    @Test
    void Q3_Q4_Q5_Q6_cashAndTransfersAreConfirmed_depositStatusReported_duplicateCodeWarned() throws Exception {
        long contract = TestUsers.nextId();
        long b = sentForSignature(contract, 20_000_000L);
        String cash = "/api/v1/residents/" + b + "/cash-payments";
        String transfer = "/api/v1/residents/" + b + "/bank-transfer-payments";
        String received = time.now().toString();

        postAs(
                        office,
                        cash,
                        "{\"ledger\":\"DEPOSIT\",\"amount\":10000000,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\""
                                + received + "\"}")
                .andExpect(status().isForbidden());
        postAs(
                        accountant,
                        cash,
                        "{\"ledger\":\"BALANCE\",\"amount\":0,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\"" + received
                                + "\"}")
                .andExpect(status().isUnprocessableEntity());
        postAs(
                        accountant,
                        cash,
                        "{\"ledger\":\"DEPOSIT\",\"amount\":10000000,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\""
                                + received + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactions[0].type").value("DEPOSIT_RECEIPT"))
                .andExpect(jsonPath("$.transactions[0].method").value("CASH"))
                .andExpect(jsonPath("$.transactions[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.balance.depositBalance").value(10_000_000))
                .andExpect(jsonPath("$.balance.depositStatus").value("NOT_SATISFIED"));
        assertThat(events.stream(DepositStatusChanged.class).filter(e -> e.residentId() == b))
                .isEmpty();

        String code = "NH001-" + TestUsers.nextId();
        postAs(
                        accountant,
                        transfer,
                        "{\"lines\":[{\"ledger\":\"DEPOSIT\",\"amount\":10000000}],\"receivedOn\":\"" + time.today()
                                + "\",\"bankTransactionCode\":\"" + code + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance.depositBalance").value(20_000_000))
                .andExpect(jsonPath("$.balance.depositStatus").value("SATISFIED"));
        var reported = events.stream(DepositStatusChanged.class)
                .filter(e -> e.residentId() == b)
                .toList();
        assertThat(reported).singleElement().satisfies(e -> {
            assertThat(e.contractId()).isEqualTo(contract);
            assertThat(e.status()).isEqualTo(DepositStatus.SATISFIED);
            assertThat(e.accountantId()).isEqualTo(accountant.accountId());
            assertThat(e.transactionId()).isNotNull();
        });

        long c = sentForSignature(TestUsers.nextId(), null);
        String shared = "NH002-" + TestUsers.nextId();
        postAs(
                        accountant,
                        "/api/v1/residents/" + c + "/bank-transfer-payments",
                        "{\"lines\":[{\"ledger\":\"DEPOSIT\",\"amount\":20000000},{\"ledger\":\"BALANCE\",\"amount\":5000000}],"
                                + "\"receivedOn\":\"" + time.today() + "\",\"bankTransactionCode\":\"" + shared
                                + "\",\"payerName\":\"Lê Văn Hải\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.transactions[0].bankTransactionCode").value(shared))
                .andExpect(jsonPath("$.transactions[1].bankTransactionCode").value(shared))
                .andExpect(jsonPath("$.balance.balance").value(5_000_000));

        String again = "{\"lines\":[{\"ledger\":\"BALANCE\",\"amount\":3000000}],\"receivedOn\":\"" + time.today()
                + "\",\"bankTransactionCode\":\"" + code + "\"";
        postAs(accountant, "/api/v1/residents/" + c + "/bank-transfer-payments", again + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BANK_TRANSACTION_CODE_USED"))
                .andExpect(jsonPath("$.duplicateTransactions[0].bankTransactionCode")
                        .value(code));
        postAs(
                        accountant,
                        "/api/v1/residents/" + c + "/bank-transfer-payments",
                        again + ",\"confirmDuplicateCode\":true}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance.balance").value(8_000_000));

        postAs(
                        accountant,
                        "/api/v1/residents/" + c + "/cash-payments",
                        "{\"ledger\":\"BALANCE\",\"amount\":5000000,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\""
                                + received + "\",\"note\":\"PT 0123\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactions[0].type").value("CASH_PAYMENT"))
                .andExpect(jsonPath("$.transactions[0].note").value("PT 0123"))
                .andExpect(jsonPath("$.balance.balance").value(13_000_000));
        assertThat(jdbc.queryForObject("SELECT balance FROM resident_balance WHERE resident_id = ?", Long.class, c))
                .as("số dư bằng tổng giao dịch Đã xác nhận (FR-002)")
                .isEqualTo(jdbc.queryForObject(
                        "SELECT SUM(amount) FROM balance_transaction WHERE resident_id = ? AND ledger = 'BALANCE'"
                                + " AND status = 'CONFIRMED'",
                        Long.class,
                        c));
        postAs(
                        accountant,
                        "/api/v1/residents/" + resident() + "/cash-payments",
                        "{\"ledger\":\"BALANCE\",\"amount\":1000,\"payerName\":\"X\",\"receivedAt\":\"" + received
                                + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NO_LEDGER"));
    }
}
