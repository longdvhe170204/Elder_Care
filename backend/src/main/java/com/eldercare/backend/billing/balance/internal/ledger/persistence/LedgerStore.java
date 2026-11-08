package com.eldercare.backend.billing.balance.internal.ledger.persistence;

import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.billing.balance.internal.ledger.domain.LedgerState;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc, ghi {@code resident_balance} (data-model 017 mục 2, research E-03): số dư chỉ đổi cùng giao dịch Đã xác nhận. */
@Repository
public class LedgerStore {

    private static final String COLUMNS = "resident_id, opening_contract_id, payment_code, balance, deposit_balance,"
            + " status, days_covered, cycle_started_on, last_notice_on, in_debt_since, reminders_stopped,"
            + " deposit_status, deposit_status_contract_id, version";

    private final JdbcTemplate jdbc;

    public LedgerStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<LedgerState> find(long residentId) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM resident_balance WHERE resident_id = ?",
                        LedgerStore::row,
                        residentId)
                .stream()
                .findFirst();
    }

    /** Khóa dòng sổ tới hết transaction (E-03: mọi giao dịch Đã xác nhận cập nhật sổ dưới khóa này). */
    public Optional<LedgerState> lock(long residentId) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM resident_balance WHERE resident_id = ? FOR UPDATE",
                        LedgerStore::row,
                        residentId)
                .stream()
                .findFirst();
    }

    public Optional<LedgerState> byPaymentCode(String paymentCode) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM resident_balance WHERE payment_code = ?",
                        LedgerStore::row,
                        paymentCode)
                .stream()
                .findFirst();
    }

    public boolean paymentCodeTaken(String paymentCode) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM resident_balance WHERE payment_code = ?)", Boolean.class, paymentCode));
    }

    public void open(long residentId, long contractId, String paymentCode, Instant at) {
        jdbc.update(
                "INSERT INTO resident_balance (resident_id, opened_at, opening_contract_id, payment_code)"
                        + " VALUES (?, ?, ?, ?)",
                residentId,
                Timestamp.from(at),
                contractId,
                paymentCode);
    }

    /** Cộng {@code amount} vào sổ; gọi sau {@link #lock} trong transaction ghi giao dịch Đã xác nhận. */
    public void add(long residentId, Ledger ledger, long amount) {
        String column = ledger == Ledger.BALANCE ? "balance" : "deposit_balance";
        jdbc.update(
                "UPDATE resident_balance SET " + column + " = " + column + " + ?, version = version + 1"
                        + " WHERE resident_id = ?",
                amount,
                residentId);
    }

    public void saveStatus(
            long residentId,
            BalanceStatus status,
            Integer daysCovered,
            LocalDate cycleStartedOn,
            LocalDate lastNoticeOn,
            LocalDate inDebtSince) {
        jdbc.update(
                "UPDATE resident_balance SET status = ?, days_covered = ?, cycle_started_on = ?, last_notice_on = ?,"
                        + " in_debt_since = ?, version = version + 1 WHERE resident_id = ?",
                status.name(),
                daysCovered,
                date(cycleStartedOn),
                date(lastNoticeOn),
                date(inDebtSince),
                residentId);
    }

    public void stopReminders(long residentId, long by, String reason) {
        jdbc.update(
                "UPDATE resident_balance SET reminders_stopped = TRUE, reminders_stopped_by = ?,"
                        + " reminders_stopped_reason = ?, version = version + 1 WHERE resident_id = ?",
                by,
                reason,
                residentId);
    }

    public void saveDepositStatus(long residentId, long contractId, DepositStatus status) {
        jdbc.update(
                "UPDATE resident_balance SET deposit_status = ?, deposit_status_contract_id = ? WHERE resident_id = ?",
                status.name(),
                contractId,
                residentId);
    }

    public List<LedgerState> inStatuses(Collection<BalanceStatus> statuses) {
        if (statuses.isEmpty()) return List.of();
        String in = statuses.stream().map(s -> "'" + s.name() + "'").collect(Collectors.joining(","));
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM resident_balance WHERE status IN (" + in + ") ORDER BY resident_id",
                LedgerStore::row);
    }

    /**
     * Đối chiếu bất biến (FR-002, DBR-28; research E-03): hồ sơ có số dư hoặc số dư cọc khác tổng giao dịch Đã xác nhận
     * của sổ đó. Chỉ đọc; không tự sửa.
     */
    public List<Long> mismatches() {
        return jdbc.queryForList(
                "SELECT b.resident_id FROM resident_balance b LEFT JOIN (SELECT resident_id,"
                        + " SUM(CASE WHEN ledger = 'BALANCE' THEN amount ELSE 0 END) AS balance,"
                        + " SUM(CASE WHEN ledger = 'DEPOSIT' THEN amount ELSE 0 END) AS deposit"
                        + " FROM balance_transaction WHERE status = 'CONFIRMED' GROUP BY resident_id) t"
                        + " ON t.resident_id = b.resident_id"
                        + " WHERE b.balance <> COALESCE(t.balance, 0) OR b.deposit_balance <> COALESCE(t.deposit, 0)"
                        + " ORDER BY b.resident_id",
                Long.class);
    }

    public List<Long> residentIds() {
        return jdbc.queryForList("SELECT resident_id FROM resident_balance ORDER BY resident_id", Long.class);
    }

    private static Date date(LocalDate day) {
        return day == null ? null : Date.valueOf(day);
    }

    private static LocalDate day(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static LedgerState row(ResultSet rs, int i) throws SQLException {
        String deposit = rs.getString("deposit_status");
        return new LedgerState(
                rs.getLong("resident_id"),
                rs.getLong("opening_contract_id"),
                rs.getString("payment_code"),
                rs.getLong("balance"),
                rs.getLong("deposit_balance"),
                BalanceStatus.valueOf(rs.getString("status")),
                (Integer) rs.getObject("days_covered", Integer.class),
                day(rs, "cycle_started_on"),
                day(rs, "last_notice_on"),
                day(rs, "in_debt_since"),
                rs.getBoolean("reminders_stopped"),
                deposit == null ? null : DepositStatus.valueOf(deposit),
                (Long) rs.getObject("deposit_status_contract_id", Long.class),
                rs.getInt("version"));
    }
}
