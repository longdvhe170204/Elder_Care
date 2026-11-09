package com.eldercare.backend.billing.balance.internal.export.persistence;

import com.eldercare.backend.billing.balance.internal.transaction.domain.BalanceEntry;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc giao dịch Đã xác nhận cho sao kê, báo cáo thu chi (017 T038; FR-025); thời điểm là lúc xác nhận như sổ. */
@Repository
public class ConfirmedEntries {

    private final JdbcTemplate jdbc;

    public ConfirmedEntries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Giao dịch Đã xác nhận của các hồ sơ ({@code null}: mọi hồ sơ) xác nhận trước {@code endExclusive}. */
    public List<BalanceEntry> upTo(Collection<Long> residentIds, Instant endExclusive) {
        StringBuilder sql = new StringBuilder(
                "SELECT resident_id, ledger, type, amount, confirmed_at, method, bank_transaction_code,"
                        + " cost_statement_id FROM balance_transaction WHERE status = 'CONFIRMED' AND confirmed_at < ?");
        List<Object> args = new ArrayList<>();
        args.add(Timestamp.from(endExclusive));
        if (residentIds != null) {
            sql.append(" AND resident_id IN (")
                    .append(String.join(",", residentIds.stream().map(id -> "?").toList()))
                    .append(")");
            args.addAll(residentIds);
        }
        sql.append(" ORDER BY confirmed_at, id");
        return jdbc.query(
                sql.toString(),
                (rs, i) -> {
                    String bankCode = rs.getString("bank_transaction_code");
                    long statement = rs.getLong("cost_statement_id");
                    String reference = bankCode != null ? bankCode : rs.wasNull() ? null : Long.toString(statement);
                    return new BalanceEntry(
                            rs.getLong("resident_id"),
                            Ledger.valueOf(rs.getString("ledger")),
                            TransactionType.valueOf(rs.getString("type")),
                            rs.getLong("amount"),
                            rs.getTimestamp("confirmed_at").toInstant(),
                            TransactionStatus.CONFIRMED,
                            PaymentMethod.valueOf(rs.getString("method")),
                            reference);
                },
                args.toArray());
    }
}
