package com.eldercare.backend.billing.balance.internal.status.service;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.status.domain.Trigger;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lần tính tình trạng hằng ngày và đối chiếu bất biến sổ (017 T021; FR-016, FR-018, FR-019, DBR-28; SC-012; research
 * E-03, E-07): mỗi hồ sơ một transaction;
 * dấu {@code reminder_marker} theo ngày giữ một lần tính mỗi ngày nên chạy lại cùng ngày không báo trùng. Lỗi của một hồ
 * sơ ghi log, không dừng các hồ sơ khác. Không ghi giao dịch (Q-306).
 */
@Service
public class DailyBalanceStatus {

    static final String MARKER_KIND = "M11.BALANCE_STATUS_DAILY";
    private static final Logger LOG = LoggerFactory.getLogger(DailyBalanceStatus.class);

    private final LedgerStore ledgers;
    private final BalanceStatusService statuses;
    private final ReminderMarkers markers;
    private final TransactionTemplate tx;
    private final BusinessTime time;

    public DailyBalanceStatus(
            LedgerStore ledgers,
            BalanceStatusService statuses,
            ReminderMarkers markers,
            TransactionTemplate tx,
            BusinessTime time) {
        this.ledgers = ledgers;
        this.statuses = statuses;
        this.markers = markers;
        this.tx = tx;
        this.time = time;
    }

    /** Số hồ sơ đã tính. */
    public int run() {
        LocalDate today = time.today();
        int count = 0;
        for (long resident : ledgers.residentIds()) {
            try {
                Boolean done = tx.execute(s -> {
                    if (!statuses.eligible(resident)
                            || !markers.mark(MARKER_KIND, BalanceTypes.LEDGER, resident, today.toString(), null)) {
                        return false;
                    }
                    statuses.recalculate(resident, Trigger.DAILY, "DAILY");
                    return true;
                });
                if (Boolean.TRUE.equals(done)) count++;
            } catch (RuntimeException error) {
                LOG.warn("Không tính được tình trạng số dư của người cao tuổi {}: {}", resident, error.getMessage());
            }
        }
        for (long resident : ledgers.mismatches()) {
            // E-03: lệch sổ là lỗi kỹ thuật, ghi lại để kiểm tra; không tự sửa số dư
            LOG.error("Số dư của người cao tuổi {} khác tổng giao dịch Đã xác nhận (DBR-28)", resident);
        }
        return count;
    }
}
