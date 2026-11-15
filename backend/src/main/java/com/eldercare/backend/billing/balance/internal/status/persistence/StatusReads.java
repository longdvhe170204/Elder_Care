package com.eldercare.backend.billing.balance.internal.status.persistence;

import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.billing.api.dto.BalanceStatusView;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc tình trạng đã lưu của sổ cho 016 (017 T038; data-model mục 2): chỉ hồ sơ Sắp hết tiền hoặc Còn nợ. */
@Repository
public class StatusReads {

    private final JdbcTemplate jdbc;

    public StatusReads(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<BalanceStatusView> lowOrOwing() {
        return jdbc.query(
                "SELECT resident_id, status, balance, days_covered FROM resident_balance WHERE status <> 'NORMAL'"
                        + " ORDER BY resident_id",
                (rs, i) -> {
                    int days = rs.getInt("days_covered");
                    return new BalanceStatusView(
                            rs.getLong("resident_id"),
                            BalanceStatus.valueOf(rs.getString("status")),
                            rs.getLong("balance"),
                            rs.wasNull() ? null : days);
                });
    }
}
