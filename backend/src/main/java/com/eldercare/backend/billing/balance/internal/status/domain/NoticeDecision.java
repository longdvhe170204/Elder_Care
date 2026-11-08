package com.eldercare.backend.billing.balance.internal.status.domain;

import static com.eldercare.backend.billing.api.dto.BalanceStatus.IN_DEBT;
import static com.eldercare.backend.billing.api.dto.BalanceStatus.NORMAL;
import static com.eldercare.backend.billing.api.dto.BalanceStatus.RUNNING_LOW;

import com.eldercare.backend.billing.api.dto.BalanceStatus;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * Bảng chuyển tình trạng số dư (tên logic {@code QuyetDinhBao}; spec 017 mục D, FR-018 → FR-020; Q-252, Q-257; research
 * E-07):
 *
 * <ul>
 *   <li>xấu đi (Bình thường → Sắp hết tiền; Bình thường, Sắp hết tiền → Còn nợ): báo ngay, bắt đầu chu kỳ mới; vào Còn
 *       nợ thì bắt đầu đếm cho FR-019;
 *   <li>Còn nợ → Sắp hết tiền: không báo ngay, giữ chu kỳ;
 *   <li>về Bình thường: dừng nhắc (xóa chu kỳ); do nộp tiền thì báo "đã ghi nhận khoản nộp, số dư đã đủ";
 *   <li>nộp tiền mà vẫn Sắp hết tiền, Còn nợ: báo "đã ghi nhận khoản nộp", không đổi lần nhắc theo chu kỳ;
 *   <li>giữ nguyên và đã qua CFG-M11-05 kể từ lần báo trước: nhắc lại; Còn nợ liên tục từ CFG-M11-05 thì báo thêm Quản lý
 *       viện.
 * </ul>
 *
 * "Dừng nhắc" của Quản lý viện chỉ chặn các lần báo tình trạng, không chặn thông báo nộp tiền (FR-018, FR-020).
 */
public final class NoticeDecision {

    private NoticeDecision() {}

    public static Decision decide(
            BalanceStatus before,
            BalanceStatus after,
            Cycle cycle,
            LocalDate today,
            int cycleDays,
            Trigger trigger,
            boolean remindersStopped) {
        Set<Notice> notices = EnumSet.noneOf(Notice.class);
        Cycle next;
        if (after == NORMAL) {
            next = Cycle.NONE;
            if (trigger == Trigger.PAYMENT && before != NORMAL) notices.add(Notice.PAYMENT_SUFFICIENT);
        } else if (worsens(before, after)) {
            next = new Cycle(today, today, after == IN_DEBT ? today : null);
            notices.add(Notice.STATUS);
        } else {
            LocalDate inDebtSince = after == IN_DEBT ? cycle.inDebtSince() : null;
            next = new Cycle(cycle.cycleStartedOn(), cycle.lastNoticeOn(), inDebtSince);
            if (trigger == Trigger.PAYMENT) {
                notices.add(Notice.PAYMENT_RECORDED);
            } else if (before == after && due(cycle.lastNoticeOn(), today, cycleDays)) {
                next = new Cycle(cycle.cycleStartedOn(), today, inDebtSince);
                notices.add(Notice.STATUS);
                if (after == IN_DEBT && due(inDebtSince, today, cycleDays)) notices.add(Notice.MANAGER);
            }
        }
        if (remindersStopped) {
            notices.remove(Notice.STATUS);
            notices.remove(Notice.MANAGER);
            if (next.lastNoticeOn() != null && next.lastNoticeOn().equals(today) && trigger == Trigger.DAILY) {
                next = new Cycle(cycle.cycleStartedOn(), cycle.lastNoticeOn(), next.inDebtSince());
            }
        }
        return new Decision(after, next, notices);
    }

    private static boolean worsens(BalanceStatus before, BalanceStatus after) {
        return (before == NORMAL && after == RUNNING_LOW) || (before != IN_DEBT && after == IN_DEBT);
    }

    private static boolean due(LocalDate since, LocalDate today, int cycleDays) {
        return since == null || !since.plusDays(cycleDays).isAfter(today);
    }
}
