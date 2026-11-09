package com.eldercare.backend.billing.balance.internal.ledger.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc liên kết người cao tuổi của sổ, giao dịch cho phạm vi và lưu giữ (017 T007). */
@Repository
public class BalanceGovernanceStore {

    private final JdbcTemplate jdbc;

    public BalanceGovernanceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasLedger(long residentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM resident_balance WHERE resident_id = ?)", Boolean.class, residentId));
    }

    public Optional<Long> transactionResident(long transactionId) {
        return jdbc
                .queryForList("SELECT resident_id FROM balance_transaction WHERE id = ?", Long.class, transactionId)
                .stream()
                .findFirst();
    }

    public List<Long> transactionsOf(long residentId) {
        return jdbc.queryForList(
                "SELECT id FROM balance_transaction WHERE resident_id = ? ORDER BY id", Long.class, residentId);
    }

    /** Điều chỉnh trỏ giao dịch gốc, giao dịch còn lại của cặp cấn trừ (FR-011, FR-015). */
    public List<Long> transactionsReferencing(long transactionId) {
        return jdbc.queryForList(
                "SELECT id FROM balance_transaction WHERE original_transaction_id = ? OR linked_transaction_id = ?"
                        + " ORDER BY id",
                Long.class,
                transactionId,
                transactionId);
    }
}
