package com.eldercare.backend.billing.balance.internal.settlement.domain;

import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatusView;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Reason;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Quy tắc thuần của quyết toán (017 FR-023, FR-024, FR-034; BR-M11-22; Q-224, Q-387; research E-08, E-09): điều kiện lệnh
 * Xác nhận quyết toán, mục quyết toán của danh sách việc sau qua đời, nhắc sau trạng thái cuối, câu chữ lý do.
 */
public final class SettlementRules {

    /** Căn cứ của lệnh Xác nhận quyết toán (FR-034; tên logic {@code can_cu_loai}). */
    public enum Basis {
        RESIDENCY_ENDING,
        POST_DEATH,
        AFTER_FINAL_STATUS
    }

    /** Một điều kiện chưa đạt của lệnh (409 kèm {@code unmetConditions}). */
    public record Unmet(String code, String description) {}

    public static final String NO_BASIS = "NOT_FINAL_STATUS";
    public static final String MONEY_NOT_SETTLED = "SETTLEMENT_CONDITIONS_NOT_MET";
    public static final String PENDING_REMAIN = "PENDING_TRANSACTIONS_REMAIN";

    private SettlementRules() {}

    /**
     * FR-034: có căn cứ; ba ý tiền đạt hoặc có ngoại lệ đã duyệt (004 FR-064, FR-071a, Q-387); không còn giao dịch Chờ
     * duyệt (không miễn được, BR-M11-22). Rỗng là đạt.
     */
    public static List<Unmet> confirmationUnmet(
            Basis basis, SettlementConditions.Result result, boolean exceptionApproved) {
        List<Unmet> unmet = new ArrayList<>();
        if (basis == null) {
            unmet.add(new Unmet(
                    NO_BASIS,
                    "Chưa có hồ sơ kết thúc lưu trú Đang chuẩn bị, danh sách việc sau qua đời đang mở, hoặc giao dịch"
                            + " mới sau lần xác nhận trước khi hồ sơ đã ở trạng thái cuối"));
        }
        if (result.settlement().status() == Status.NOT_MET && !exceptionApproved) {
            unmet.add(new Unmet(MONEY_NOT_SETTLED, text(result.settlement().reasons())));
        }
        if (result.confirmation().reasons().contains(Reason.PENDING_TRANSACTIONS)) {
            unmet.add(new Unmet(PENDING_REMAIN, text(List.of(Reason.PENDING_TRANSACTIONS))));
        }
        return unmet;
    }

    /**
     * FR-023: mục {@code SETTLEMENT} của danh sách việc sau qua đời. Không áp dụng khi hai sổ chưa từng có giao dịch khác 0
     * đồng; Hoàn thành khi đã xác nhận quyết toán và ba ý tiền đạt hoặc có ngoại lệ đã duyệt (Q-387).
     */
    public static PostDeathItemStatusView postDeathItem(
            boolean hasNonZeroTransaction, SettlementConditions.Result result, boolean exceptionApproved) {
        if (!hasNonZeroTransaction) {
            return new PostDeathItemStatusView(
                    PostDeathItemStatus.NOT_APPLICABLE, "Sổ số dư, sổ tiền cọc chưa từng có giao dịch khác 0 đồng");
        }
        if (result.satisfied(exceptionApproved)) {
            boolean byException = result.settlement().status() == Status.NOT_MET;
            return new PostDeathItemStatusView(
                    PostDeathItemStatus.COMPLETED,
                    byException ? "Đã xác nhận quyết toán theo ngoại lệ đã duyệt (Q-387)" : "Đã xác nhận quyết toán");
        }
        List<Reason> reasons = new ArrayList<>(result.confirmation().reasons());
        if (!exceptionApproved) reasons.addAll(0, result.settlement().reasons());
        return new PostDeathItemStatusView(PostDeathItemStatus.NOT_COMPLETED, text(reasons));
    }

    /**
     * FR-024, Q-224: hồ sơ ở trạng thái cuối, không còn hồ sơ kết thúc Đang chuẩn bị hay danh sách việc sau qua đời đang
     * mở, có giao dịch Đã xác nhận sau thời điểm trạng thái cuối. Đã có bảng quyết toán thì nhắc tới khi xác nhận lại sau
     * giao dịch cuối (xác nhận lại đòi số dư 0); chưa từng xác nhận thì nhắc tới khi hai sổ về 0.
     */
    public static boolean afterFinalReminderDue(
            boolean finalStatus,
            boolean openBasis,
            boolean transactionAfterFinal,
            boolean statementExists,
            boolean uncovered,
            long balance,
            long depositBalance) {
        if (!finalStatus || openBasis || !transactionAfterFinal) return false;
        return statementExists ? uncovered : balance != 0 || depositBalance != 0;
    }

    /** Câu chữ lý do cho 004, màn web (US7 #1, #3). */
    public static String text(List<Reason> reasons) {
        return reasons.stream().map(SettlementRules::text).collect(Collectors.joining("; "));
    }

    private static String text(Reason reason) {
        return switch (reason) {
            case FINAL_PERIOD_NOT_PAID -> "Bảng kỳ cuối chưa chốt";
            case BALANCE_NOT_ZERO -> "Số dư chưa bằng 0";
            case DEPOSIT_NOT_ZERO -> "Số dư cọc chưa bằng 0";
            case PENDING_TRANSACTIONS -> "Còn giao dịch Chờ duyệt";
            case NO_CURRENT_CONFIRMATION -> "Chưa xác nhận quyết toán";
            case TRANSACTION_AFTER_CONFIRMATION -> "Có giao dịch mới sau lần xác nhận quyết toán";
        };
    }
}
