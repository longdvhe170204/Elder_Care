package com.cambae.backend.m15.approval.internal;

import static com.cambae.backend.m15.approval.api.ApprovalStatus.APPLIED;
import static com.cambae.backend.m15.approval.api.ApprovalStatus.APPLY_FAILED;
import static com.cambae.backend.m15.approval.api.ApprovalStatus.APPROVED;
import static com.cambae.backend.m15.approval.api.ApprovalStatus.CANCELLED;
import static com.cambae.backend.m15.approval.api.ApprovalStatus.DRAFT;
import static com.cambae.backend.m15.approval.api.ApprovalStatus.PENDING_APPROVAL;
import static com.cambae.backend.m15.approval.api.ApprovalStatus.REJECTED;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m15.approval.api.ExtendedTransitions;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * Bảng chuyển trạng thái chung của yêu cầu phê duyệt (data-model mục 3; 000 T091). Chuyển không có trong bảng, hoặc
 * không được loại yêu cầu khai báo thêm, trả 409 {@code INVALID_STATE}. Kiểm tra chung khác (lý do, quyền tại lúc quyết
 * định, khóa lạc quan, nhật ký) nằm ở service và áp cho cả chuyển mở rộng.
 */
public class RequestTransitionTable {

    public enum Command {
        EDIT_CONTENT,
        SUBMIT,
        APPROVE,
        REJECT,
        CANCEL,
        APPLY,
        APPLY_FAILED,
        /** Người cao tuổi sang trạng thái cuối (bổ sung theo plan 004). */
        CLOSE_FOR_FINAL_STATE
    }

    private static final Set<String> FINAL = Set.of(APPLIED, APPLY_FAILED, REJECTED, CANCELLED);

    private static final Map<Command, Map<String, String>> TABLE = new EnumMap<>(Map.of(
            Command.EDIT_CONTENT, Map.of(DRAFT, DRAFT),
            Command.SUBMIT, Map.of(DRAFT, PENDING_APPROVAL),
            Command.APPROVE, Map.of(PENDING_APPROVAL, APPROVED),
            Command.REJECT, Map.of(PENDING_APPROVAL, REJECTED),
            Command.CANCEL, Map.of(DRAFT, CANCELLED, PENDING_APPROVAL, CANCELLED, APPROVED, CANCELLED),
            Command.APPLY, Map.of(APPROVED, APPLIED),
            Command.APPLY_FAILED, Map.of(APPROVED, APPLY_FAILED),
            Command.CLOSE_FOR_FINAL_STATE,
                    Map.of(DRAFT, CANCELLED, PENDING_APPROVAL, CANCELLED, APPROVED, APPLY_FAILED)));

    public String next(String status, Command command) {
        String to = TABLE.get(command).get(status);
        if (to == null) {
            throw invalid(status, command.name());
        }
        return to;
    }

    public boolean isFinal(String status) {
        return FINAL.contains(status);
    }

    public String nextExtended(String status, ExtendedTransitions declared, String code) {
        ExtendedTransitions.Transition transition = declared.byCode().get(code);
        if (transition == null || !transition.from().contains(status)) {
            throw invalid(status, code);
        }
        return transition.to();
    }

    private static BusinessException invalid(String status, String command) {
        return new BusinessException(
                HttpStatus.CONFLICT, "INVALID_STATE", "Không thực hiện được " + command + " ở trạng thái " + status);
    }
}
