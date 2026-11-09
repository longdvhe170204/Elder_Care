package com.eldercare.backend.billing.balance.internal.status.domain;

import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Dựng yêu cầu thông báo số dư (FR-018 → FR-020, FR-030; research E-07). Người đại diện nhận hai phần: "chung" (không có
 * số tiền) và "chi phí" (tình trạng, số dư, tổng chưa chốt, thông tin nộp tiền); 009 chỉ gửi phần chi phí cho người có
 * quyền xem chi phí (BR-M13-04). Kế toán, Quản lý viện mức Nhẹ. Khóa sự kiện theo người cao tuổi, loại và ngày hoặc giao
 * dịch nên lần chạy lại không gửi trùng.
 */
public final class BalanceNotices {

    private static final String MODULE = "M11";
    private static final String SOURCE = "M11.BALANCE";
    private static final String TITLE = "Số dư";

    /** Số liệu cho nội dung; {@code paymentInstructions} rỗng khi viện chưa đặt CFG-M11-10. */
    public record Facts(
            long residentId,
            String fullName,
            BalanceStatus status,
            long balance,
            long unfinalizedTotal,
            String paymentInstructions,
            LocalDate inDebtSince,
            String key,
            Instant at) {}

    private BalanceNotices() {}

    public static BalanceNotice of(Facts f, java.util.Set<Notice> notices) {
        List<NotificationRequest> requests = new ArrayList<>();
        for (Notice notice : notices) {
            switch (notice) {
                case STATUS -> {
                    String type =
                            f.status() == BalanceStatus.IN_DEBT ? "M11_BALANCE_IN_DEBT" : "M11_BALANCE_RUNNING_LOW";
                    requests.add(family(
                            f,
                            type,
                            NotificationLevel.MEDIUM,
                            "Có thông báo về tài chính của " + f.fullName() + ", vui lòng liên hệ Kế toán."));
                    requests.add(staff(
                            f,
                            type,
                            Role.ACCOUNTANT,
                            f.fullName() + " chuyển " + label(f.status()) + ": số dư " + money(f.balance())
                                    + ", chi phí chưa chốt " + money(f.unfinalizedTotal()) + "."));
                }
                case MANAGER ->
                    requests.add(staff(
                            f,
                            "M11_BALANCE_IN_DEBT_MANAGER",
                            Role.NURSING_HOME_MANAGER,
                            f.fullName() + " còn nợ liên tục từ " + f.inDebtSince() + ": số dư " + money(f.balance())
                                    + "."));
                case PAYMENT_SUFFICIENT ->
                    requests.add(family(
                            f,
                            "M11_BALANCE_PAYMENT_SUFFICIENT",
                            NotificationLevel.LOW,
                            "Đã ghi nhận khoản nộp cho " + f.fullName() + ", số dư đã đủ."));
                case PAYMENT_RECORDED ->
                    requests.add(family(
                            f,
                            "M11_BALANCE_PAYMENT_RECORDED",
                            NotificationLevel.LOW,
                            "Đã ghi nhận khoản nộp cho " + f.fullName() + ", tình trạng sau khi nộp: "
                                    + label(f.status()) + "."));
            }
        }
        return new BalanceNotice(requests);
    }

    private static NotificationRequest family(Facts f, String type, NotificationLevel level, String general) {
        StringBuilder costs = new StringBuilder("Tình trạng: ")
                .append(label(f.status()))
                .append("; số dư ")
                .append(money(f.balance()))
                .append("; chi phí chưa chốt ")
                .append(money(f.unfinalizedTotal()))
                .append('.');
        if (f.paymentInstructions() != null) costs.append(' ').append(f.paymentInstructions());
        return request(
                f,
                type,
                level,
                new RecipientGroup.Representative(f.residentId()),
                List.of(
                        new ContentPart(InformationType.GENERAL, general),
                        new ContentPart(InformationType.COSTS, costs.toString())),
                general,
                Optional.of(InformationSubject.ABOUT_RESIDENT));
    }

    private static NotificationRequest staff(Facts f, String type, Role role, String text) {
        return request(
                f,
                type,
                NotificationLevel.LOW,
                new RecipientGroup.FacilityRole(role),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                text,
                Optional.empty());
    }

    private static NotificationRequest request(
            Facts f,
            String type,
            NotificationLevel level,
            RecipientGroup group,
            List<ContentPart> content,
            String shortContent,
            Optional<InformationSubject> subject) {
        return new NotificationRequest(
                MODULE,
                type,
                SourceGroup.COSTS,
                new SourceRecord(SOURCE, Long.toString(f.residentId())),
                type + ":" + f.residentId() + ":" + f.key(),
                Optional.of(level),
                Optional.of(f.residentId()),
                Optional.empty(),
                List.of(group),
                content,
                Optional.of(shortContent),
                Optional.of(TITLE),
                Optional.empty(),
                false,
                f.at(),
                Map.of(),
                false,
                subject,
                Optional.empty());
    }

    public static String label(BalanceStatus status) {
        return switch (status) {
            case NORMAL -> "Bình thường";
            case RUNNING_LOW -> "Sắp hết tiền";
            case IN_DEBT -> "Còn nợ";
        };
    }

    public static String money(long amount) {
        return String.format(Locale.GERMANY, "%,d", amount) + " đồng";
    }
}
