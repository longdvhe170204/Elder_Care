package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.billing.balance.internal.status.domain.Cycle;
import com.eldercare.backend.billing.balance.internal.status.domain.Decision;
import com.eldercare.backend.billing.balance.internal.status.domain.Notice;
import com.eldercare.backend.billing.balance.internal.status.domain.NoticeDecision;
import com.eldercare.backend.billing.balance.internal.status.domain.Trigger;
import com.eldercare.backend.support.TableRows;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 017 T018 (tên logic {@code QuyetDinhBaoTest}; FR-018 → FR-020; BR-M11-13; Q-252, Q-257; research E-07): mọi dòng của
 * bảng chuyển tình trạng số dư. Chiều xấu đi báo ngay, đặt lại chu kỳ; Còn nợ → Sắp hết tiền không báo, giữ chu kỳ; về
 * Bình thường thì dừng nhắc; giữ nguyên thì nhắc lại sau CFG-M11-05 (Quản lý viện thêm khi còn nợ từ CFG-M11-05); nộp tiền
 * về Bình thường báo "đã đủ", chưa đủ báo "đã ghi nhận", chu kỳ giữ; "dừng nhắc" chỉ chặn lần nhắc.
 */
class NoticeDecisionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final int CYCLE_DAYS = 7;

    private static LocalDate ago(String days) {
        return days == null ? null : TODAY.minusDays(Integer.parseInt(days));
    }

    @Test
    void BR_61_everyRowOfTheBalanceStatusTransitionTable() {
        TableRows.csv(
                row -> {
                    Decision d = NoticeDecision.decide(
                            BalanceStatus.valueOf(row[0]),
                            BalanceStatus.valueOf(row[1]),
                            new Cycle(ago(row[2]), ago(row[2]), ago(row[3])),
                            TODAY,
                            CYCLE_DAYS,
                            Trigger.valueOf(row[4]),
                            Boolean.parseBoolean(row[5]));
                    Set<Notice> expected = row[6] == null
                            ? EnumSet.noneOf(Notice.class)
                            : Arrays.stream(row[6].split("\\|"))
                                    .map(Notice::valueOf)
                                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Notice.class)));
                    assertThat(d.status()).isEqualTo(BalanceStatus.valueOf(row[1]));
                    assertThat(d.notices()).isEqualTo(expected);
                    assertThat(d.cycle().lastNoticeOn()).as("lần báo").isEqualTo(ago(row[7]));
                    assertThat(d.cycle().cycleStartedOn()).as("đầu chu kỳ").isEqualTo(ago(row[8]));
                    assertThat(d.cycle().inDebtSince()).as("còn nợ từ").isEqualTo(ago(row[9]));
                },
                // cũ, mới, lần báo trước (ngày trước), còn nợ từ, lần tính, dừng nhắc, báo, lần báo sau, đầu chu kỳ
                // sau, còn nợ từ sau
                "NORMAL, RUNNING_LOW, , , DAILY, false, STATUS, 0, 0,",
                "NORMAL, IN_DEBT, , , DAILY, false, STATUS, 0, 0, 0",
                "RUNNING_LOW, IN_DEBT, 3, , DAILY, false, STATUS, 0, 0, 0",
                "IN_DEBT, RUNNING_LOW, 3, 10, DAILY, false, , 3, 3,",
                "RUNNING_LOW, NORMAL, 3, , DAILY, false, , , ,",
                "IN_DEBT, NORMAL, 3, 10, DAILY, false, , , ,",
                "RUNNING_LOW, RUNNING_LOW, 6, , DAILY, false, , 6, 6,",
                "RUNNING_LOW, RUNNING_LOW, 7, , DAILY, false, STATUS, 0, 7,",
                "IN_DEBT, IN_DEBT, 7, 7, DAILY, false, STATUS|MANAGER, 0, 7, 7",
                "IN_DEBT, IN_DEBT, 7, 6, DAILY, false, STATUS, 0, 7, 6",
                "IN_DEBT, IN_DEBT, 7, 30, DAILY, true, , 7, 7, 30",
                "RUNNING_LOW, NORMAL, 3, , PAYMENT, false, PAYMENT_SUFFICIENT, , ,",
                "IN_DEBT, RUNNING_LOW, 3, 10, PAYMENT, false, PAYMENT_RECORDED, 3, 3,",
                "IN_DEBT, IN_DEBT, 3, 10, PAYMENT, true, PAYMENT_RECORDED, 3, 3, 10",
                "NORMAL, NORMAL, , , PAYMENT, false, , , ,",
                "NORMAL, NORMAL, , , DAILY, false, , , ,");
    }
}
