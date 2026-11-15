package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 017 T007 (research E-12, data-model mục 8; 000 FR-051a): sổ và giao dịch lưu theo hồ sơ người cao tuổi, sổ không bị
 * xóa trước giao dịch trỏ tới nó; phạm vi của sổ, giao dịch là người cao tuổi; trường tiền thuộc nhóm tài chính.
 */
class T007_BalanceProvidersIT extends MySqlIntegrationTestBase {

    private static final String LEDGER = "M11.BALANCE";
    private static final String TRANSACTION = "M11.BALANCE_TRANSACTION";

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    List<FieldClassificationProvider> classifications;

    private <T> T of(List<T> providers, java.util.function.Predicate<T> match) {
        return providers.stream().filter(match).findFirst().orElseThrow();
    }

    private long transaction(long resident, String type, Long original) {
        jdbc.update(
                "INSERT INTO balance_transaction (resident_id, ledger, type, amount, occurred_at, method, reason,"
                        + " original_transaction_id, status, confirmed_at, locked_at, created_at, created_by)"
                        + " VALUES (?, 'BALANCE', ?, 500000, UTC_TIMESTAMP(6), ?, ?, ?, 'CONFIRMED',"
                        + " UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 1)",
                resident,
                type,
                type.equals("CASH_PAYMENT") ? "CASH" : "OTHER",
                type.equals("ADJUSTMENT") ? "Ghi nhầm người" : null,
                original);
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM balance_transaction WHERE resident_id = ?", Long.class, resident);
    }

    @Test
    void FR_029_ledgerAndTransactions_followTheResidentRecord_andAreScopedToTheResident() {
        long resident = TestUsers.nextId();
        long[] ids = tx.execute(status -> {
            jdbc.update(
                    "INSERT INTO resident_balance (resident_id, opened_at, opening_contract_id, payment_code)"
                            + " VALUES (?, UTC_TIMESTAMP(6), ?, ?)",
                    resident,
                    TestUsers.nextId(),
                    "T" + resident);
            long payment = transaction(resident, "CASH_PAYMENT", null);
            long adjustment = transaction(resident, "ADJUSTMENT", payment);
            return new long[] {payment, adjustment};
        });
        RetentionProvider ledgers = of(retention, p -> LEDGER.equals(p.dataType()));
        RetentionProvider transactions = of(retention, p -> TRANSACTION.equals(p.dataType()));
        DataRef residentRecord = new DataRef(SharedRowRetention.RESIDENT, resident);

        assertThat(ledgers.referencing(residentRecord)).containsExactly(new DataRef(LEDGER, resident));
        // 010 T033 (2026-11-12): giao dịch đi theo sổ rồi theo giao dịch được trỏ, không gắn thẳng hồ sơ, để chuỗi xóa
        // điều chỉnh trước giao dịch gốc (fk_balance_transaction_original); cả chuỗi kiểm ở ResidentRetentionIT.
        assertThat(transactions.referencing(residentRecord)).isEmpty();
        assertThat(ledgers.referencesTo(resident)).containsExactly(new DataRef(TRANSACTION, ids[0]));
        assertThat(transactions.referencesTo(ids[0])).containsExactly(new DataRef(TRANSACTION, ids[1]));

        assertThat(of(scopes, p -> LEDGER.equals(p.entityType())).assignScope(resident))
                .isEqualTo(new Target(LEDGER, Long.toString(resident), resident, null));
        assertThat(of(scopes, p -> TRANSACTION.equals(p.entityType())).assignScope(ids[1]))
                .isEqualTo(new Target(TRANSACTION, Long.toString(ids[1]), resident, null));
        assertThat(of(classifications, p -> TRANSACTION.equals(p.entityType())).fieldGroups())
                .containsEntry("amount", InfoGroup.FINANCE)
                .containsEntry("bankTransactionCode", InfoGroup.FINANCE);
        assertThat(of(classifications, p -> LEDGER.equals(p.entityType())).fieldGroups())
                .containsEntry("paymentCode", InfoGroup.FINANCE);

        tx.executeWithoutResult(status -> {
            transactions.delete(List.of(ids[0], ids[1]));
            ledgers.delete(List.of(resident));
        });
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM balance_transaction WHERE resident_id = ?", Integer.class, resident))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM resident_balance WHERE resident_id = ?", Integer.class, resident))
                .isZero();
    }
}
