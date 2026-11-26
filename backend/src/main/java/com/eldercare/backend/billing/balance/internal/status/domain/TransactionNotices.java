package com.eldercare.backend.billing.balance.internal.status.domain;

import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Thông báo người đại diện mỗi lần số dư, tiền cọc biến động (demo 2026-11-26, E làm, chờ chủ dự án duyệt; mở rộng FR-018,
 * FR-020 của 017): giao dịch Đã xác nhận nêu loại, số tiền, số dư và tiền cọc sau giao dịch; giao dịch bị từ chối, hủy
 * báo số dư không đổi. Phần chung (GENERAL) trung tính, chi tiết ở phần chi phí (COSTS) như {@link BalanceNotices}; khoản
 * phải trả là việc của chính người thân (ABOUT_FAMILY_MATTER, Q-353, Q-362).
 */
public final class TransactionNotices {

    private static final String TYPE = "M11_BALANCE_TRANSACTION";
    private static final Map<TransactionType, String> LABELS = Map.of(
            TransactionType.CASH_PAYMENT, "nộp tiền mặt",
            TransactionType.BANK_TRANSFER_PAYMENT, "nộp chuyển khoản",
            TransactionType.STATEMENT_PAYMENT, "thanh toán bảng chi phí",
            TransactionType.REFUND, "hoàn số dư",
            TransactionType.DEPOSIT_OFFSET, "cấn trừ tiền cọc",
            TransactionType.ADJUSTMENT, "điều chỉnh",
            TransactionType.DEPOSIT_RECEIPT, "thu tiền cọc",
            TransactionType.DEPOSIT_REFUND, "hoàn tiền cọc");

    /** Một dòng giao dịch; {@code amount} có dấu như trong sổ. */
    public record Line(TransactionType type, long amount) {}

    private TransactionNotices() {}

    public static BalanceNotice confirmed(
            long residentId,
            String name,
            List<Line> lines,
            long balanceAfter,
            long depositAfter,
            String key,
            Instant at) {
        String what = lines.stream()
                .map(l -> LABELS.get(l.type())
                        + (l.type() == TransactionType.ADJUSTMENT
                                ? " " + signed(l.amount())
                                : " " + money(Math.abs(l.amount()))))
                .collect(Collectors.joining(", "));
        String text = "Đã ghi nhận " + what + " cho " + name + ". Số dư sau giao dịch: " + money(balanceAfter)
                + (balanceAfter < 0 ? " (còn nợ " + money(-balanceAfter) + ")" : "") + "; tiền cọc: "
                + money(depositAfter) + ".";
        return notice(residentId, name, text, key, at);
    }

    public static BalanceNotice closed(
            long residentId, String name, TransactionType type, long amount, boolean rejected, String key, Instant at) {
        String text = "Yêu cầu " + LABELS.get(type) + " " + money(Math.abs(amount)) + " cho " + name + " đã bị "
                + (rejected ? "từ chối" : "hủy") + "; số dư không đổi.";
        return notice(residentId, name, text, key, at);
    }

    private static BalanceNotice notice(long residentId, String name, String text, String key, Instant at) {
        String general = "Có biến động số dư của " + name + ", xem chi tiết ở mục Số dư.";
        return new BalanceNotice(List.of(new NotificationRequest(
                "M11",
                TYPE,
                SourceGroup.COSTS,
                new SourceRecord("M11.BALANCE", Long.toString(residentId)),
                TYPE + ":" + residentId + ":" + key,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.Representative(residentId)),
                List.of(
                        new ContentPart(InformationType.GENERAL, general),
                        new ContentPart(InformationType.COSTS, text)),
                Optional.of(general),
                Optional.of("Số dư"),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty())));
    }

    private static String signed(long amount) {
        return (amount > 0 ? "+" : "") + money(amount);
    }

    static String money(long amount) {
        return String.format(Locale.GERMANY, "%,d", amount) + " đ";
    }
}
