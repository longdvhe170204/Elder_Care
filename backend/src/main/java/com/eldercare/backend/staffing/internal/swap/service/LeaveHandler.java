package com.eldercare.backend.staffing.internal.swap.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ExtendedTransitions;
import com.eldercare.backend.administration.approval.api.dto.ExtendedTransitions.Transition;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.staffing.internal.swap.domain.SwapStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Loại yêu cầu {@code M09.LEAVE_REQUEST} (nghỉ đột xuất, FR-026 → FR-031): kết quả duyệt từng ca hoặc ngày là chuyển mở
 * rộng ({@code APPLIED}, {@code PARTIALLY_APPLIED}, {@code REJECTED}, {@code EXPIRED}) kích hoạt từ {@link LeaveService}.
 * Duyệt chung của 000 bị chặn như {@link SwapHandler}, vì cần kết quả từng mục và người phụ trách mới.
 */
@Component
class LeaveHandler implements RequestTypeHandler {

    static final String TYPE = "M09.LEAVE_REQUEST";
    static final String APPLY = "APPLY";
    static final String APPLY_PARTIAL = "APPLY_PARTIAL";
    static final String REJECT = "REJECT";
    static final String CANCEL = "CANCEL";
    static final String EXPIRE = "EXPIRE";

    @Override
    public String typeCode() {
        return TYPE;
    }

    @Override
    public void validateContent(JsonNode content) {
        for (String field : List.of("kind", "staffId")) {
            if (!content.hasNonNull(field)) {
                throw new InvalidValueException("Thiếu trường " + field);
            }
        }
        if (!Set.of("BY_SHIFT", "BY_DAY").contains(content.get("kind").asString(""))) {
            throw new InvalidValueException("Dạng nghỉ không hợp lệ");
        }
    }

    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.SHIFT_SWAP_REQUEST, Operation.CREATE);
    }

    /**
     * FR-026 (015 T033): nhân viên lập yêu cầu nghỉ cho chính mình không cần phân công hiện tại; lập thay (Trưởng tầng)
     * vẫn kiểm quyền theo phạm vi.
     */
    @Override
    public Optional<Boolean> creatorInOwnScope(CurrentUser user, JsonNode content) {
        long target = content.path("staffId").asLong();
        return user.staffId().filter(id -> id == target).map(id -> true);
    }

    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.SHIFT_SWAP_REQUEST, Operation.APPROVE);
    }

    @Override
    public Target scope(JsonNode content) {
        Long floor =
                content.hasNonNull("floorAreaId") ? content.get("floorAreaId").asLong() : null;
        return new Target("M09.LEAVE_REQUEST", content.path("staffId").asString(""), null, floor);
    }

    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        return List.of();
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        return new ResultRef(TYPE, Long.toString(request.id()));
    }

    @Override
    public List<UnmetCondition> checkOnApprove(ApprovalRequestView request) {
        return List.of(new UnmetCondition(
                "USE_LEAVE_REQUEST_ENDPOINT", "Duyệt nghỉ đột xuất qua POST /leave-requests/{id}/approve của lịch ca"));
    }

    @Override
    public ExtendedTransitions extendedTransitions() {
        Set<String> pending = Set.of(SwapStatus.PENDING_APPROVAL);
        return new ExtendedTransitions(Map.of(
                APPLY, new Transition(pending, SwapStatus.APPLIED),
                APPLY_PARTIAL, new Transition(pending, SwapStatus.PARTIALLY_APPLIED),
                REJECT, new Transition(pending, SwapStatus.REJECTED),
                CANCEL, new Transition(pending, SwapStatus.CANCELLED),
                EXPIRE, new Transition(pending, SwapStatus.EXPIRED)));
    }
}
