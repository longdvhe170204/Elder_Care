package com.eldercare.backend.billing.balance.internal.ledger.persistence;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.domain.ConfirmedEntry;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Đọc, ghi {@code balance_transaction} (data-model 017 mục 3); giao dịch Đã xác nhận không sửa (trigger, DBR-17). */
@Repository
public class BalanceTransactionStore {

    private static final String COLUMNS = "id, resident_id, ledger, type, amount, occurred_at, method,"
            + " bank_transaction_code, cost_statement_id, original_transaction_id, linked_transaction_id, payer_name,"
            + " note, reason, status, approval_request_id, approved_by, confirmed_at, adjusted, created_by";

    private final JdbcTemplate jdbc;

    public BalanceTransactionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Thêm giao dịch Đã xác nhận ({@code locked_at} cùng lúc xác nhận); trả mã. */
    public long insertConfirmed(ConfirmedEntry e, Instant now) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO balance_transaction (resident_id, ledger, type, amount, occurred_at, method,"
                                    + " bank_transaction_code, cost_statement_id, payer_name, note, status,"
                                    + " confirmed_at, duplicate_code_confirmed, locked_at, created_at, created_by)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMED', ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    Timestamp at = Timestamp.from(now);
                    ps.setLong(1, e.residentId());
                    ps.setString(2, e.ledger().name());
                    ps.setString(3, e.type().name());
                    ps.setLong(4, e.amount());
                    ps.setTimestamp(5, Timestamp.from(e.at()));
                    ps.setString(6, e.method().name());
                    ps.setString(7, e.bankTransactionCode());
                    if (e.costStatementId() == null) ps.setNull(8, Types.BIGINT);
                    else ps.setLong(8, e.costStatementId());
                    ps.setString(9, e.payerName());
                    ps.setString(10, e.note());
                    ps.setTimestamp(11, at);
                    ps.setBoolean(12, e.duplicateCodeConfirmed());
                    ps.setTimestamp(13, at);
                    ps.setTimestamp(14, at);
                    if (e.createdBy() == null) ps.setNull(15, Types.BIGINT);
                    else ps.setLong(15, e.createdBy());
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<BalanceTransactionRow> find(long id) {
        return jdbc
                .query("SELECT " + COLUMNS + " FROM balance_transaction WHERE id = ?", BalanceTransactionStore::row, id)
                .stream()
                .findFirst();
    }

    /** Giao dịch của hồ sơ theo sổ, khoảng ngày theo thời điểm (giờ Việt Nam, đã quy ra mốc), trạng thái; mới nhất trước. */
    public List<BalanceTransactionRow> list(
            long residentId, Ledger ledger, Instant from, Instant toExclusive, TransactionStatus status) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM balance_transaction WHERE resident_id = ?");
        List<Object> args = new ArrayList<>(List.of(residentId));
        if (ledger != null) {
            sql.append(" AND ledger = ?");
            args.add(ledger.name());
        }
        if (from != null) {
            sql.append(" AND occurred_at >= ?");
            args.add(Timestamp.from(from));
        }
        if (toExclusive != null) {
            sql.append(" AND occurred_at < ?");
            args.add(Timestamp.from(toExclusive));
        }
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }
        sql.append(" ORDER BY occurred_at DESC, id DESC");
        return jdbc.query(sql.toString(), BalanceTransactionStore::row, args.toArray());
    }

    /** DBR-29: giao dịch khác đã mang mã giao dịch ngân hàng này (mọi hồ sơ), trừ các dòng {@code except}. */
    public List<BalanceTransactionRow> byBankCode(String bankTransactionCode, Collection<Long> except) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM balance_transaction WHERE bank_transaction_code = ? ORDER BY id",
                        BalanceTransactionStore::row,
                        bankTransactionCode)
                .stream()
                .filter(t -> !except.contains(t.id()))
                .toList();
    }

    public boolean statementPaid(long costStatementId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM balance_transaction WHERE cost_statement_id = ?)",
                Boolean.class,
                costStatementId));
    }

    /** Giao dịch Đã xác nhận gần nhất của sổ, làm căn cứ trạng thái đặt cọc (FR-006). */
    public Optional<BalanceTransactionRow> lastConfirmed(long residentId, Ledger ledger) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM balance_transaction WHERE resident_id = ? AND ledger = ?"
                                + " AND status = 'CONFIRMED' ORDER BY confirmed_at DESC, id DESC LIMIT 1",
                        BalanceTransactionStore::row,
                        residentId,
                        ledger.name())
                .stream()
                .findFirst();
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static BalanceTransactionRow row(ResultSet rs, int i) throws SQLException {
        return new BalanceTransactionRow(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                Ledger.valueOf(rs.getString("ledger")),
                TransactionType.valueOf(rs.getString("type")),
                rs.getLong("amount"),
                instant(rs, "occurred_at"),
                PaymentMethod.valueOf(rs.getString("method")),
                rs.getString("bank_transaction_code"),
                rs.getObject("cost_statement_id", Long.class),
                rs.getObject("original_transaction_id", Long.class),
                rs.getObject("linked_transaction_id", Long.class),
                rs.getString("payer_name"),
                rs.getString("note"),
                rs.getString("reason"),
                TransactionStatus.valueOf(rs.getString("status")),
                rs.getObject("approval_request_id", Long.class),
                rs.getObject("approved_by", Long.class),
                instant(rs, "confirmed_at"),
                rs.getBoolean("adjusted"),
                rs.getObject("created_by", Long.class));
    }
}
